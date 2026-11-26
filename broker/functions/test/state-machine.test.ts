import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import { getFirestore } from "firebase-admin/firestore";
import type { Deps } from "../src/app.ts";
import { reconcilePending } from "../src/clients/logmeal-sync.ts";
import { ALLOWED, transition } from "../src/clients/state-machine.ts";
import { LogMealError } from "../src/logmeal/client.ts";
import { withClientLease } from "../src/store/lease.ts";
import { refs, type ClientDoc } from "../src/store/model.ts";
import { anonymousIdToken, call, clearEmulators, deviceId, startBroker, type TestBroker } from "./harness.ts";

async function provisioned(broker: TestBroker, device = "phone-1") {
  const { idToken } = await anonymousIdToken();
  await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: { deviceId: deviceId(device), language: "eng", country: "RS" } });
  const doc = (await getFirestore().collection("clients").where("deviceId", "==", deviceId(device)).get()).docs[0]!;
  return { idToken, clientId: doc.id, userId: doc.get("logmealUserId") as number | null };
}

const clientDoc = async (id: string) => (await refs(getFirestore()).client(id).get()).data() as ClientDoc;
const admin = { actor: "admin:test", reason: "test", admin: true };
const system = { actor: "system", reason: "test" };

describe("client state machine", () => {
  let broker: TestBroker;
  let logmealDown = false;
  before(async () => {
    broker = await startBroker();
    const real = broker.deps.logmeal;
    const wrapped: Deps["logmeal"] = Object.create(real);
    const failing = <A extends unknown[], R>(fn: (...a: A) => Promise<R>) => async (...a: A) => {
      if (logmealDown) throw new LogMealError("/v2/users", 503, null, "down");
      return fn(...a);
    };
    wrapped.setEnabled = failing(real.setEnabled.bind(real));
    wrapped.deleteUser = failing(real.deleteUser.bind(real));
    wrapped.signUp = failing(real.signUp.bind(real));
    broker.deps.logmeal = wrapped;
  });
  after(() => broker.close());
  beforeEach(async () => {
    logmealDown = false;
    await clearEmulators();
    broker.fake.users.clear();
  });

  const enabled = (userId: number | null) => broker.fake.users.get(userId!)?.enabled;
  const toggles = (userId: number | null) => broker.fake.calls.filter((c) => c.path === `/v2/users/toggleEnabled/${userId}`).length;

  it("pause and resume disable and enable the APIUser, reading before each toggle", async () => {
    const { clientId, userId } = await provisioned(broker);
    await transition(broker.deps, clientId, "paused", { ...admin, reason: "payment on hold" });
    assert.equal(enabled(userId), false);
    const paused = await clientDoc(clientId);
    assert.equal(paused.status, "paused");
    assert.equal(paused.logmealEnabled, false);
    assert.equal(paused.logmealPending, false);

    await transition(broker.deps, clientId, "active", system);
    assert.equal(enabled(userId), true);
    assert.equal(toggles(userId), 2);
  });

  it("is idempotent: the same transition twice toggles once", async () => {
    const { clientId, userId } = await provisioned(broker);
    await transition(broker.deps, clientId, "suspended", { ...system, reason: "abuse" });
    await transition(broker.deps, clientId, "suspended", { ...system, reason: "abuse" });
    assert.equal(toggles(userId), 1);
    const audits = await getFirestore().collection("audit").where("action", "==", "status_suspended").get();
    assert.equal(audits.size, 1);
    assert.equal(audits.docs[0]!.get("before.status"), "trial");
    assert.equal(audits.docs[0]!.get("after.status"), "suspended");
    assert.equal(audits.docs[0]!.get("reason"), "abuse");
    assert.equal(audits.docs[0]!.get("after.tokenCiphertext"), undefined, "no token material in audit");
  });

  it("does not toggle when LogMeal already has the wanted state", async () => {
    const { clientId, userId } = await provisioned(broker);
    broker.fake.users.get(userId!)!.enabled = false; // changed by hand in LogMeal's dashboard
    await transition(broker.deps, clientId, "paused", admin);
    assert.equal(toggles(userId), 0);
    assert.equal(enabled(userId), false);
  });

  it("cancel deletes the APIUser and wipes the token", async () => {
    const { idToken, clientId, userId } = await provisioned(broker);
    await transition(broker.deps, clientId, "cancelled", { ...admin, reason: "cleanup" });
    assert.equal(broker.fake.users.has(userId!), false);
    const client = await clientDoc(clientId);
    assert.equal(client.tokenCiphertext, null);
    assert.equal(client.logmealUserId, null);
    const status = await call(broker, "GET", "/v1/logmeal/status?knownTokenVersion=0", { idToken });
    assert.equal(status.body?.status, "cancelled");
    assert.equal(status.body?.token, undefined);
  });

  it("refuses transitions outside the diagram, and leaving Cancelled even for admins", async () => {
    const { clientId } = await provisioned(broker);
    await assert.rejects(transition(broker.deps, clientId, "waitlisted", system), /cannot become waitlisted/);
    await transition(broker.deps, clientId, "cancelled", admin);
    await assert.rejects(transition(broker.deps, clientId, "trial", admin), /cannot become trial/);
    assert.deepEqual(ALLOWED.cancelled, []);
  });

  it("approving a waitlisted client provisions its APIUser into a free slot only", async () => {
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 1 });
    const first = await provisioned(broker, "phone-1");
    const waiting = await provisioned(broker, "phone-2");
    assert.equal((await clientDoc(waiting.clientId)).status, "waitlisted");

    await assert.rejects(transition(broker.deps, waiting.clientId, "trial", { ...admin, reason: "approved" }), /pilot slot/);
    await transition(broker.deps, first.clientId, "cancelled", admin);
    const approved = await transition(broker.deps, waiting.clientId, "trial", { ...admin, reason: "approved" });
    assert.equal(approved.status, "trial");
    assert.equal(approved.statusReason, "approved");
    assert.ok(approved.logmealUserId && broker.fake.users.get(approved.logmealUserId)?.enabled);
  });

  it("keeps the new status when LogMeal is down and the hourly job finishes the change", async () => {
    const { clientId, userId } = await provisioned(broker);
    logmealDown = true;
    const paused = await transition(broker.deps, clientId, "paused", admin);
    assert.equal(paused.status, "paused", "the registry holds the intent");
    assert.equal(paused.logmealPending, true);
    assert.equal(enabled(userId), true);

    assert.deepEqual(await reconcilePending(broker.deps), { checked: 1, inStep: 0 });
    logmealDown = false;
    assert.deepEqual(await reconcilePending(broker.deps), { checked: 1, inStep: 1 });
    assert.equal(enabled(userId), false);
    assert.equal((await clientDoc(clientId)).logmealPending, false);
    assert.deepEqual(await reconcilePending(broker.deps), { checked: 0, inStep: 0 });
  });

  it("the hourly job also finishes a trial end and a cancellation", async () => {
    const ended = await provisioned(broker, "phone-1");
    const cancelled = await provisioned(broker, "phone-2");
    await refs(getFirestore()).client(ended.clientId).update({ picturesUsed: 89 });
    logmealDown = true;
    await call(broker, "POST", "/v1/logmeal/picture", { idToken: ended.idToken, body: { requestId: "req-00000090" } });
    await transition(broker.deps, cancelled.clientId, "cancelled", admin);
    logmealDown = false;

    assert.deepEqual(await reconcilePending(broker.deps), { checked: 2, inStep: 2 });
    assert.equal(enabled(ended.userId), false);
    assert.equal(broker.fake.users.has(cancelled.userId!), false);
  });

  it("serializes jobs per client: a second change waits for the lease", async () => {
    const { clientId } = await provisioned(broker);
    let release!: () => void;
    const holding = withClientLease(broker.deps.db, clientId, broker.deps.now, () => new Promise<void>((r) => (release = r)));
    await new Promise((r) => setTimeout(r, 100));
    await assert.rejects(transition(broker.deps, clientId, "paused", admin), /in progress/);
    release();
    await holding;
    assert.equal((await transition(broker.deps, clientId, "paused", admin)).status, "paused");
  });

  it("an expired lease from a crashed job does not block the client forever", async () => {
    const { clientId } = await provisioned(broker);
    await refs(getFirestore()).client(clientId).update({ lease: { holder: "crashed", until: new Date("2026-11-26T11:59:00Z") } });
    assert.equal((await transition(broker.deps, clientId, "paused", admin)).status, "paused");
  });
});
