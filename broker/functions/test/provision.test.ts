import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import { getFirestore } from "firebase-admin/firestore";
import type { Deps } from "../src/app.ts";
import { LogMealError } from "../src/logmeal/client.ts";
import { refs, type ClientDoc } from "../src/store/model.ts";
import { anonymousIdToken, call, clearEmulators, deviceId, emailIdToken, startBroker, type TestBroker } from "./harness.ts";

const body = (device: string) => ({ deviceId: deviceId(device), language: "eng", country: "RS" });

async function provisionAs(broker: TestBroker, device: string, idToken?: string) {
  const token = idToken ?? (await anonymousIdToken()).idToken;
  return { idToken: token, ...(await call(broker, "POST", "/v1/logmeal/provision", { idToken: token, body: body(device) })) };
}

async function clientsByStatus() {
  const snap = await getFirestore().collection("clients").get();
  const counts: Record<string, number> = {};
  for (const doc of snap.docs) counts[doc.get("status")] = (counts[doc.get("status")] ?? 0) + 1;
  return counts;
}

describe("provision", () => {
  let broker: TestBroker;
  before(async () => {
    broker = await startBroker();
  });
  after(() => broker.close());
  beforeEach(async () => {
    await clearEmulators();
    broker.fake.users.clear();
  });

  it("requires an anonymous Firebase user", async () => {
    assert.equal((await call(broker, "POST", "/v1/logmeal/provision", { body: body("a") })).status, 401);
    assert.equal((await call(broker, "POST", "/v1/logmeal/provision", { idToken: "not-a-token", body: body("a") })).status, 401);
    const admin = await call(broker, "POST", "/v1/logmeal/provision", { idToken: await emailIdToken("someone@example.com"), body: body("a") });
    assert.equal(admin.status, 403);
    assert.equal(admin.body?.error, "anonymous_only");
  });

  it("validates the body and never takes a raw ANDROID_ID", async () => {
    const { idToken } = await anonymousIdToken();
    for (const bad of [
      { ...body("a"), deviceId: "9774d56d682e549c" },
      { ...body("a"), language: "en" },
      { ...body("a"), country: "rs" },
      [],
    ]) {
      const res = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: bad });
      assert.equal(res.status, 400, JSON.stringify(bad));
    }
  });

  it("first AI use creates a Trial client with its own APIUser and profile", async () => {
    const res = await provisionAs(broker, "phone-1");
    assert.equal(res.status, 200);
    assert.equal(res.body?.status, "trial");
    assert.match(res.body?.token, /^fake-token-/);
    assert.deepEqual(res.body?.pictures, { used: 0, limit: 90 });
    assert.equal(res.body?.isTest, false);
    assert.equal(res.body?.tokenVersion, 1);
    const [user] = [...broker.fake.users.values()];
    assert.equal(user?.country, "RS");
    assert.match(user?.username ?? "", /^snk_[0-9a-f]{16}$/);

    const stored = (await getFirestore().collection("clients").get()).docs[0]?.data() as ClientDoc;
    assert.ok(stored.tokenCiphertext && !stored.tokenCiphertext.includes(res.body?.token), "token stored encrypted only");
  });

  it("is idempotent for the same install", async () => {
    const first = await provisionAs(broker, "phone-1");
    const again = await provisionAs(broker, "phone-1", first.idToken);
    assert.equal(again.body?.token, first.body?.token);
    assert.equal(broker.fake.users.size, 1);
  });

  it("re-attaches a reinstall on the same phone with the same token and picture counter", async () => {
    const first = await provisionAs(broker, "phone-1");
    const clientId = (await getFirestore().collection("clients").get()).docs[0]!.id;
    await refs(getFirestore()).client(clientId).update({ picturesUsed: 33 });

    const reinstall = await provisionAs(broker, "phone-1"); // new anonymous UID
    assert.equal(reinstall.body?.token, first.body?.token);
    assert.deepEqual(reinstall.body?.pictures, { used: 33, limit: 90 });
    assert.equal(broker.fake.users.size, 1);
    const client = (await refs(getFirestore()).client(clientId).get()).data() as ClientDoc;
    assert.equal(client.authUids.length, 2);
  });

  it("waitlists the 6th client without creating an APIUser; test clients take no slot", async () => {
    for (let i = 1; i <= 5; i++) assert.equal((await provisionAs(broker, `phone-${i}`)).body?.status, "trial");
    const sixth = await provisionAs(broker, "phone-6");
    assert.equal(sixth.body?.status, "waitlisted");
    assert.equal(sixth.body?.reason, "pilot_full");
    assert.equal(sixth.body?.token, undefined);
    assert.equal(broker.fake.users.size, 5);

    // Marking one pilot client as test frees its slot; the waitlisted device gets it on retry.
    const trial = (await getFirestore().collection("clients").where("status", "==", "trial").limit(1).get()).docs[0]!;
    await trial.ref.update({ isTest: true });
    const retried = await provisionAs(broker, "phone-6", sixth.idToken);
    assert.equal(retried.body?.status, "trial");
    assert.equal(broker.fake.users.size, 6);
  });

  it("promotes the waitlist in order of the first AI attempt", async () => {
    for (let i = 1; i <= 5; i++) await provisionAs(broker, `phone-${i}`);
    broker.setNow(new Date("2026-11-26T12:01:00Z"));
    const firstInLine = await provisionAs(broker, "phone-6");
    broker.setNow(new Date("2026-11-26T12:02:00Z"));
    const secondInLine = await provisionAs(broker, "phone-7");

    const trial = (await getFirestore().collection("clients").where("status", "==", "trial").limit(1).get()).docs[0]!;
    await trial.ref.update({ status: "cancelled" });

    assert.equal((await provisionAs(broker, "phone-7", secondInLine.idToken)).body?.status, "waitlisted", "not first in line");
    assert.equal((await provisionAs(broker, "phone-6", firstInLine.idToken)).body?.status, "trial");
    assert.equal((await provisionAs(broker, "phone-7", secondInLine.idToken)).body?.status, "waitlisted", "no slot left");
  });

  it("a device that used its pictures gets no new trial after Delete all data", async () => {
    await refs(getFirestore()).device(deviceId("phone-1")).set({ clientId: null, picturesUsed: 90 });
    const res = await provisionAs(broker, "phone-1");
    assert.equal(res.body?.status, "trial_ended");
    assert.equal(res.body?.reason, "device_allowance_used");
    assert.equal(res.body?.token, undefined);
    assert.equal(broker.fake.users.size, 0);
  });

  it("keeps the remaining pictures of a device after Delete all data", async () => {
    await refs(getFirestore()).device(deviceId("phone-1")).set({ clientId: null, picturesUsed: 40 });
    assert.deepEqual((await provisionAs(broker, "phone-1")).body?.pictures, { used: 40, limit: 90 });
  });

  it("never creates two APIUsers for a double tap", async () => {
    const { idToken } = await anonymousIdToken();
    const results = await Promise.all([1, 2, 3].map(() => call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") })));
    for (const r of results) assert.ok([200, 503].includes(r.status), `status ${r.status}`);
    assert.ok(results.some((r) => r.status === 200));
    assert.equal(broker.fake.users.size, 1);
    assert.equal((await getFirestore().collection("clients").get()).size, 1);
  });

  it("never exceeds the pilot cap under concurrent first use", async () => {
    await Promise.all([1, 2, 3, 4, 5, 6, 7, 8].map((i) => provisionAs(broker, `phone-${i}`)));
    const counts = await clientsByStatus();
    assert.equal(counts.trial, 5);
    assert.equal(counts.waitlisted, 3);
  });
});

describe("provision when LogMeal fails", () => {
  let broker: TestBroker;
  let failures = 0;
  before(async () => {
    broker = await startBroker();
    const real = broker.deps.logmeal;
    const flaky: Deps["logmeal"] = Object.create(real);
    flaky.setProfile = async (...args) => {
      if (failures-- > 0) throw new LogMealError("/v2/users/modifyUserProfileInfo/1", 500, null, "boom");
      return real.setProfile(...args);
    };
    broker.deps.logmeal = flaky;
  });
  after(() => broker.close());
  beforeEach(() => clearEmulators());

  it("answers 503 and resumes on retry without a second APIUser", async () => {
    failures = 1;
    const { idToken } = await anonymousIdToken();
    const first = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
    assert.equal(first.status, 503);
    assert.equal(first.body?.error, "logmeal_unavailable");
    const retry = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
    assert.equal(retry.body?.status, "trial");
    assert.equal(broker.fake.users.size, 1);
  });
});

describe("status", () => {
  let broker: TestBroker;
  before(async () => {
    broker = await startBroker();
  });
  after(() => broker.close());
  beforeEach(() => clearEmulators());

  it("is 404 before the first AI action", async () => {
    const { idToken } = await anonymousIdToken();
    const res = await call(broker, "GET", "/v1/logmeal/status", { idToken });
    assert.equal(res.status, 404);
    assert.equal(res.body?.error, "not_provisioned");
  });

  it("returns the token only when the app's copy is out of date", async () => {
    const { idToken, body: provisioned } = await provisionAs(broker, "phone-1");
    const current = await call(broker, "GET", "/v1/logmeal/status?knownTokenVersion=1", { idToken });
    assert.equal(current.body?.status, "trial");
    assert.equal(current.body?.token, undefined);
    const stale = await call(broker, "GET", "/v1/logmeal/status?knownTokenVersion=0", { idToken });
    assert.equal(stale.body?.token, provisioned?.token);
    assert.equal((await call(broker, "GET", "/v1/logmeal/status?knownTokenVersion=x", { idToken })).status, 400);
  });

  it("shows trial ended without a token, with the counter and the test flag", async () => {
    const { idToken } = await provisionAs(broker, "phone-1");
    const client = (await getFirestore().collection("clients").get()).docs[0]!;
    await client.ref.update({ status: "trial_ended", statusReason: "trial_allowance_used", picturesUsed: 90, isTest: true });
    const res = await call(broker, "GET", "/v1/logmeal/status", { idToken });
    assert.deepEqual(res.body, { status: "trial_ended", reason: "trial_allowance_used", tokenVersion: 1, pictures: { used: 90, limit: 90 }, isTest: true });
  });
});

describe("modes and App Check", () => {
  it("local fake mode marks every client as test", async () => {
    await clearEmulators();
    const broker = await startBroker({ mode: "fake" });
    try {
      assert.equal((await provisionAs(broker, "phone-1")).body?.isTest, true);
    } finally {
      await broker.close();
    }
  });

  it("rejects calls without App Check when it is required", async () => {
    const broker = await startBroker({ appCheckRequired: true });
    try {
      const { idToken } = await anonymousIdToken();
      const res = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
      assert.equal(res.status, 401);
      assert.match(res.body?.message, /App Check/);
    } finally {
      await broker.close();
    }
  });
});
