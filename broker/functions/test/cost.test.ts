import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import { getFirestore } from "firebase-admin/firestore";
import type { RemoteConfig, RemoteConfigTemplate } from "firebase-admin/remote-config";
import { checkCredits } from "../src/cost/credit-check.ts";
import { KILL_SWITCH_PARAMETER, RemoteConfigKillSwitch } from "../src/cost/kill-switch.ts";
import { anonymousIdToken, call, clearEmulators, deviceId, startBroker, type TestBroker } from "./harness.ts";

const body = (device: string) => ({ deviceId: deviceId(device), language: "eng", country: "RS" });
const setSwitch = (on: boolean) => getFirestore().collection("settings").doc("broker").set({ aiRecognitionEnabled: on }, { merge: true });

describe("kill switch", () => {
  let broker: TestBroker;
  before(async () => {
    broker = await startBroker();
  });
  after(() => broker.close());
  beforeEach(async () => {
    await clearEmulators();
    broker.fake.users.clear();
  });

  it("while off, creates no client and no APIUser", async () => {
    await setSwitch(false);
    const { idToken } = await anonymousIdToken();
    const res = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
    assert.equal(res.status, 503);
    assert.equal(res.body?.error, "ai_disabled");
    assert.equal((await getFirestore().collection("clients").get()).size, 0);
    assert.equal(broker.fake.users.size, 0);
  });

  it("existing clients see it in provision and status, without touching LogMeal users", async () => {
    const { idToken } = await anonymousIdToken();
    await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
    await setSwitch(false);
    const calls = broker.fake.calls.length;
    const status = await call(broker, "GET", "/v1/logmeal/status", { idToken });
    assert.equal(status.body?.aiEnabled, false);
    assert.equal(status.body?.status, "trial");
    const again = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
    assert.equal(again.body?.aiEnabled, false);
    assert.equal(broker.fake.calls.length, calls, "no LogMeal call");
    assert.equal([...broker.fake.users.values()][0]?.enabled, true);

    await setSwitch(true);
    assert.equal((await call(broker, "GET", "/v1/logmeal/status", { idToken })).body?.aiEnabled, true);
  });

  it("does not promote the waitlist while off", async () => {
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 0 });
    const { idToken } = await anonymousIdToken();
    await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") });
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 5, aiRecognitionEnabled: false });
    assert.equal((await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: body("phone-1") })).body?.status, "waitlisted");
    assert.equal(broker.fake.users.size, 0);
  });
});

describe("Remote Config kill switch", () => {
  const template = (value?: string) =>
    ({ parameters: value === undefined ? {} : { [KILL_SWITCH_PARAMETER]: { defaultValue: { value } } } }) as unknown as RemoteConfigTemplate;
  const remoteConfig = (get: () => Promise<RemoteConfigTemplate>) => ({ getTemplate: get }) as unknown as RemoteConfig;

  it("reads the parameter, caches it for a minute, and fails open", async () => {
    let value = "false";
    let reads = 0;
    let t = Date.parse("2026-11-26T12:00:00Z");
    const ks = new RemoteConfigKillSwitch(
      remoteConfig(async () => {
        reads++;
        return template(value);
      }),
      () => new Date(t),
    );
    assert.equal(await ks.aiEnabled(), false);
    value = "true";
    assert.equal(await ks.aiEnabled(), false, "cached");
    t += 61_000;
    assert.equal(await ks.aiEnabled(), true);
    assert.equal(reads, 2);

    assert.equal(await new RemoteConfigKillSwitch(remoteConfig(async () => template()), () => new Date(t)).aiEnabled(), true, "missing parameter = on");
    const broken = new RemoteConfigKillSwitch(
      remoteConfig(async () => {
        throw new Error("permission denied");
      }),
      () => new Date(t),
    );
    assert.equal(await broken.aiEnabled(), true, "unreadable = on");
  });
});

describe("credit check", () => {
  let broker: TestBroker;
  before(async () => {
    broker = await startBroker();
  });
  after(() => broker.close());
  beforeEach(() => clearEmulators());

  it("raises each threshold once per month", async () => {
    broker.fake.setCredits(100, 100);
    assert.equal((await checkCredits(broker.deps))?.alarmed, false, "50 %");

    broker.fake.setCredits(141, 59);
    const at70 = await checkCredits(broker.deps);
    assert.equal(at70?.level, 0.7);
    assert.equal(at70?.alarmed, true);
    assert.equal((await checkCredits(broker.deps))?.alarmed, false, "70 % already raised");

    broker.fake.setCredits(185, 15);
    assert.equal((await checkCredits(broker.deps))?.level, 0.9);
    broker.fake.setCredits(200, 0);
    const full = await checkCredits(broker.deps);
    assert.equal(full?.level, 1);
    assert.equal(full?.alarmed, true);
    assert.equal(full?.month, "2026-11");
  });

  it("starts over in a new month", async () => {
    broker.fake.setCredits(150, 50);
    assert.equal((await checkCredits(broker.deps))?.alarmed, true);
    broker.setNow(new Date("2026-12-01T06:00:00Z"));
    try {
      assert.equal((await checkCredits(broker.deps))?.alarmed, true, "December starts at no alarm");
    } finally {
      broker.setNow(new Date("2026-11-26T12:00:00Z"));
    }
  });

  it("uses the thresholds from settings", async () => {
    await getFirestore().collection("settings").doc("broker").set({ creditAlarmThresholds: [0.5] });
    broker.fake.setCredits(110, 90);
    const result = await checkCredits(broker.deps);
    assert.equal(result?.level, 0.5);
    assert.equal(result?.alarmed, true);
  });
});
