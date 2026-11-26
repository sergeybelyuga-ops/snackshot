import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import { getFirestore } from "firebase-admin/firestore";
import type { Deps } from "../src/app.ts";
import { syncLogMealEnabled } from "../src/clients/logmeal-sync.ts";
import { LogMealError } from "../src/logmeal/client.ts";
import { refs, type ClientDoc } from "../src/store/model.ts";
import { anonymousIdToken, call, clearEmulators, deviceId, startBroker, type TestBroker } from "./harness.ts";

const provisionBody = (device: string) => ({ deviceId: deviceId(device), language: "eng", country: "RS" });

async function provisioned(broker: TestBroker, device = "phone-1") {
  const { idToken } = await anonymousIdToken();
  const res = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: provisionBody(device) });
  const client = (await getFirestore().collection("clients").where("deviceId", "==", deviceId(device)).get()).docs[0]!;
  return { idToken, clientId: client.id, token: res.body?.token as string };
}

const picture = (broker: TestBroker, idToken: string, requestId: string) => call(broker, "POST", "/v1/logmeal/picture", { idToken, body: { requestId } });
const clientDoc = async (id: string) => (await refs(getFirestore()).client(id).get()).data() as ClientDoc | undefined;

describe("picture", () => {
  let broker: TestBroker;
  let failDisable = false;
  before(async () => {
    broker = await startBroker();
    const real = broker.deps.logmeal;
    const wrapped: Deps["logmeal"] = Object.create(real);
    wrapped.setEnabled = async (...args) => {
      if (failDisable) throw new LogMealError("/v2/users/toggleEnabled/1", 500, null, "boom");
      return real.setEnabled(...args);
    };
    broker.deps.logmeal = wrapped;
  });
  after(() => broker.close());
  beforeEach(async () => {
    failDisable = false;
    await clearEmulators();
    broker.fake.users.clear();
  });

  it("counts one picture per request id", async () => {
    const { idToken } = await provisioned(broker);
    assert.deepEqual((await picture(broker, idToken, "req-00000001")).body, { status: "trial", pictures: { used: 1, limit: 90 } });
    assert.deepEqual((await picture(broker, idToken, "req-00000001")).body?.pictures, { used: 1, limit: 90 }, "retry is not counted");
    assert.deepEqual((await picture(broker, idToken, "req-00000002")).body?.pictures, { used: 2, limit: 90 });
  });

  it("counts concurrent retries of the same report once", async () => {
    const { idToken, clientId } = await provisioned(broker);
    await Promise.all([1, 2, 3, 4].map(() => picture(broker, idToken, "req-same-0001")));
    assert.equal((await clientDoc(clientId))?.picturesUsed, 1);
  });

  it("ends the trial at the 90th picture and disables the APIUser", async () => {
    const { idToken, clientId } = await provisioned(broker);
    await refs(getFirestore()).client(clientId).update({ picturesUsed: 88 });
    assert.equal((await picture(broker, idToken, "req-00000089")).body?.status, "trial");
    const last = await picture(broker, idToken, "req-00000090");
    assert.deepEqual(last.body, { status: "trial_ended", pictures: { used: 90, limit: 90 } });

    const user = [...broker.fake.users.values()][0]!;
    assert.equal(user.enabled, false);
    const client = await clientDoc(clientId);
    assert.equal(client?.statusReason, "trial_allowance_used");
    assert.equal(client?.logmealEnabled, false);
    const status = await call(broker, "GET", "/v1/logmeal/status?knownTokenVersion=0", { idToken });
    assert.equal(status.body?.status, "trial_ended");
    assert.equal(status.body?.token, undefined);
    assert.equal((await getFirestore().collection("audit").where("action", "==", "trial_ended").get()).size, 1);
  });

  it("still counts a recognition that was in flight when the trial ended", async () => {
    const { idToken, clientId } = await provisioned(broker);
    await refs(getFirestore()).client(clientId).update({ picturesUsed: 89 });
    await picture(broker, idToken, "req-00000090");
    const late = await picture(broker, idToken, "req-00000091");
    assert.deepEqual(late.body, { status: "trial_ended", pictures: { used: 91, limit: 90 } });
  });

  it("keeps the trial ended when LogMeal cannot be reached and disables it on the next try", async () => {
    const { idToken, clientId } = await provisioned(broker);
    await refs(getFirestore()).client(clientId).update({ picturesUsed: 89 });
    failDisable = true;
    assert.equal((await picture(broker, idToken, "req-00000090")).body?.status, "trial_ended");
    assert.equal((await clientDoc(clientId))?.logmealEnabled, true, "LogMeal not in step yet");
    assert.equal([...broker.fake.users.values()][0]!.enabled, true);

    failDisable = false;
    assert.equal(await syncLogMealEnabled(broker.deps, clientId), true);
    assert.equal([...broker.fake.users.values()][0]!.enabled, false);
  });

  it("keeps the device counter in step for Delete all data", async () => {
    const { idToken } = await provisioned(broker);
    await picture(broker, idToken, "req-00000001");
    const device = (await refs(getFirestore()).device(deviceId("phone-1")).get()).data();
    assert.equal(device?.picturesUsed, 1);
  });

  it("refuses reports without AI access and validates the request id", async () => {
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 0 });
    const { idToken } = await provisioned(broker);
    assert.equal((await picture(broker, idToken, "req-00000001")).status, 409);
    assert.equal((await picture(broker, idToken, "short")).status, 400);
    assert.equal((await picture(broker, (await anonymousIdToken()).idToken, "req-00000001")).status, 404);
  });
});

describe("delete account", () => {
  let broker: TestBroker;
  let failDelete = false;
  before(async () => {
    broker = await startBroker();
    const real = broker.deps.logmeal;
    const wrapped: Deps["logmeal"] = Object.create(real);
    wrapped.deleteUser = async (...args) => {
      if (failDelete) throw new LogMealError("/v2/users/deleteAPIUser/1", 500, null, "boom");
      return real.deleteUser(...args);
    };
    broker.deps.logmeal = wrapped;
  });
  after(() => broker.close());
  beforeEach(async () => {
    failDelete = false;
    await clearEmulators();
    broker.fake.users.clear();
  });

  it("deletes the APIUser and the record but keeps the device's picture counter", async () => {
    const { idToken, clientId } = await provisioned(broker);
    for (const id of ["req-00000001", "req-00000002", "req-00000003"]) await picture(broker, idToken, id);

    assert.equal((await call(broker, "DELETE", "/v1/account", { idToken })).status, 204);
    assert.equal(broker.fake.users.size, 0);
    assert.equal(await clientDoc(clientId), undefined);
    assert.deepEqual((await refs(getFirestore()).device(deviceId("phone-1")).get()).data(), { clientId: null, picturesUsed: 3 });
    assert.equal((await call(broker, "GET", "/v1/logmeal/status", { idToken })).status, 404);

    // The next AI action provisions a new client that continues the device's counter.
    const again = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: provisionBody("phone-1") });
    assert.equal(again.body?.status, "trial");
    assert.deepEqual(again.body?.pictures, { used: 3, limit: 90 });
    const audit = await getFirestore().collection("audit").where("action", "==", "account_deleted").get();
    assert.equal(audit.size, 1);
    assert.equal(audit.docs[0]!.get("after"), null);
  });

  it("frees the pilot slot", async () => {
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 1 });
    const first = await provisioned(broker, "phone-1");
    const waiting = await provisioned(broker, "phone-2");
    assert.equal((await clientDoc(waiting.clientId))?.status, "waitlisted");
    await call(broker, "DELETE", "/v1/account", { idToken: first.idToken });
    const res = await call(broker, "POST", "/v1/logmeal/provision", { idToken: waiting.idToken, body: provisionBody("phone-2") });
    assert.equal(res.body?.status, "trial");
  });

  it("erases nothing when LogMeal fails, so the app can retry", async () => {
    const { idToken, clientId } = await provisioned(broker);
    failDelete = true;
    assert.equal((await call(broker, "DELETE", "/v1/account", { idToken })).status, 503);
    assert.ok(await clientDoc(clientId));
    failDelete = false;
    assert.equal((await call(broker, "DELETE", "/v1/account", { idToken })).status, 204);
    assert.equal(await clientDoc(clientId), undefined);
  });

  it("is a no-op for a caller who never used AI or already deleted", async () => {
    const { idToken } = await anonymousIdToken();
    assert.equal((await call(broker, "DELETE", "/v1/account", { idToken })).status, 204);
  });

  it("deletes a waitlisted client without calling LogMeal", async () => {
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 0 });
    const { idToken, clientId } = await provisioned(broker);
    const before = broker.fake.calls.length;
    assert.equal((await call(broker, "DELETE", "/v1/account", { idToken })).status, 204);
    assert.equal(await clientDoc(clientId), undefined);
    assert.equal(broker.fake.calls.length, before);
  });
});
