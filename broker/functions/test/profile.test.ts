import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import { getFirestore } from "firebase-admin/firestore";
import type { Deps } from "../src/app.ts";
import { LogMealError } from "../src/logmeal/client.ts";
import type { ClientDoc } from "../src/store/model.ts";
import { anonymousIdToken, call, clearEmulators, deviceId, startBroker, type TestBroker } from "./harness.ts";

async function provisioned(broker: TestBroker, device = "phone-1") {
  const { idToken } = await anonymousIdToken();
  await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: { deviceId: deviceId(device), language: "eng", country: "RS" } });
  return idToken;
}

const onlyClient = async () => (await getFirestore().collection("clients").get()).docs[0]!;

describe("profile", () => {
  let broker: TestBroker;
  let failNext = false;
  before(async () => {
    broker = await startBroker();
    const real = broker.deps.logmeal;
    const wrapped: Deps["logmeal"] = Object.create(real);
    wrapped.setProfile = async (...args) => {
      if (failNext) {
        failNext = false;
        throw new LogMealError("/v2/users/modifyUserProfileInfo/1", 500, null, "boom");
      }
      return real.setProfile(...args);
    };
    broker.deps.logmeal = wrapped;
  });
  after(() => broker.close());
  beforeEach(async () => {
    await clearEmulators();
    broker.fake.users.clear();
  });

  it("updates LogMeal and the registry", async () => {
    const idToken = await provisioned(broker);
    const res = await call(broker, "POST", "/v1/logmeal/profile", { idToken, body: { language: "spa", country: "ES" } });
    assert.deepEqual(res, { status: 200, body: { language: "spa", country: "ES" } });
    const [user] = [...broker.fake.users.values()];
    assert.equal(user?.language, "spa");
    assert.equal(user?.country, "ES");
    const client = (await onlyClient()).data() as ClientDoc;
    assert.equal(client.language, "spa");
    assert.equal(client.country, "ES");
    const audit = await getFirestore().collection("audit").where("action", "==", "profile_updated").get();
    assert.equal(audit.size, 1);
  });

  it("does nothing when nothing changed", async () => {
    const idToken = await provisioned(broker);
    const before = broker.fake.calls.length;
    await call(broker, "POST", "/v1/logmeal/profile", { idToken, body: { language: "eng", country: "RS" } });
    assert.equal(broker.fake.calls.length, before);
  });

  it("stores the profile of a waitlisted client and sends it when the APIUser is created", async () => {
    await getFirestore().collection("settings").doc("broker").set({ maxClients: 0 });
    const idToken = await provisioned(broker);
    assert.equal(((await onlyClient()).data() as ClientDoc).status, "waitlisted");
    await call(broker, "POST", "/v1/logmeal/profile", { idToken, body: { language: "ita", country: "IT" } });
    assert.equal(broker.fake.users.size, 0);

    await getFirestore().collection("settings").doc("broker").set({ maxClients: 5 });
    const res = await call(broker, "POST", "/v1/logmeal/provision", { idToken, body: { deviceId: deviceId("phone-1"), language: "eng", country: "RS" } });
    assert.equal(res.body?.status, "trial");
    const [user] = [...broker.fake.users.values()];
    assert.equal(user?.language, "ita");
    assert.equal(user?.country, "IT");
  });

  it("leaves both sides unchanged when LogMeal fails", async () => {
    const idToken = await provisioned(broker);
    failNext = true;
    const res = await call(broker, "POST", "/v1/logmeal/profile", { idToken, body: { language: "spa", country: "ES" } });
    assert.equal(res.status, 503);
    assert.equal(((await onlyClient()).data() as ClientDoc).language, "eng");
  });

  it("validates codes and needs a provisioned client", async () => {
    const idToken = await provisioned(broker);
    assert.equal((await call(broker, "POST", "/v1/logmeal/profile", { idToken, body: { language: "Spanish", country: "ES" } })).status, 400);
    assert.equal((await call(broker, "POST", "/v1/logmeal/profile", { idToken, body: { language: "spa", country: "Spain" } })).status, 400);
    const stranger = (await anonymousIdToken()).idToken;
    const res = await call(broker, "POST", "/v1/logmeal/profile", { idToken: stranger, body: { language: "spa", country: "ES" } });
    assert.equal(res.status, 404);
  });
});
