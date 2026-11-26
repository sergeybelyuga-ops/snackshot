import assert from "node:assert/strict";
import { after, before, describe, it } from "node:test";
import { startFakeLogMeal, FAKE_COMPANY_TOKEN, type FakeLogMeal } from "../../../tools/fake-logmeal/server.ts";
import { loadConfig } from "../src/config.ts";
import { EmulatorTokenCipher } from "../src/crypto/token-cipher.ts";
import { HttpLogMealCompany, LogMealError, TestUserLogMeal } from "../src/logmeal/client.ts";
import { call, startBroker, type TestBroker } from "./harness.ts";

describe("config guard rails", () => {
  const base = { LOGMEAL_BASE_URL: "https://api.logmeal.com", APP_CHECK_REQUIRED: "true" };

  it("maps each project to its mode and KMS key", () => {
    const prod = loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-prod", LOGMEAL_MODE: "company" });
    assert.equal(prod.kmsKeyName, "projects/snackshot-prod/locations/europe-west3/keyRings/broker/cryptoKeys/apiuser-token");
    assert.equal(loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-dev-app", LOGMEAL_MODE: "test-user", LOGMEAL_TEST_USER_ID: "75682" }).testUserId, 75682);
    const local = loadConfig({ GCLOUD_PROJECT: "demo-snackshot", LOGMEAL_MODE: "fake", LOGMEAL_BASE_URL: "http://127.0.0.1:8090", APP_CHECK_REQUIRED: "false" });
    assert.equal(local.kmsKeyName, null);
  });

  it("refuses unsafe combinations", () => {
    assert.throws(() => loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-dev-app", LOGMEAL_MODE: "fake" }), /emulator only/);
    assert.throws(() => loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-dev-app", LOGMEAL_MODE: "company" }), /only in snackshot-prod/);
    assert.throws(() => loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-prod", LOGMEAL_MODE: "company", APP_CHECK_REQUIRED: "false" }), /App Check/);
    assert.throws(
      () => loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-prod", LOGMEAL_MODE: "company", LOGMEAL_BASE_URL: "http://api.logmeal.com" }),
      /HTTPS/,
    );
    assert.throws(() => loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-dev-app", LOGMEAL_MODE: "test-user" }), /TEST_USER_ID/);
    assert.throws(() => loadConfig({ ...base, GCLOUD_PROJECT: "snackshot-prod" }), /LOGMEAL_MODE/);
  });
});

describe("LogMeal company client against the fake", () => {
  let fake: FakeLogMeal;
  let logmeal: HttpLogMealCompany;

  before(async () => {
    fake = await startFakeLogMeal();
    logmeal = new HttpLogMealCompany(fake.url, () => FAKE_COMPANY_TOKEN);
  });
  after(() => fake.close());

  it("signs up, sets the profile, and lists the user", async () => {
    const user = await logmeal.signUp("snk_test_1", "eng");
    assert.match(user.token, /^fake-token-/);
    await logmeal.setProfile(user.id, "spa", "ES");
    assert.equal(fake.users.get(user.id)?.language, "spa");
    assert.equal(fake.users.get(user.id)?.country, "ES");
    assert.deepEqual((await logmeal.listUsers()).find((u) => u.id === user.id), { id: user.id, username: "snk_test_1", enabled: true });
  });

  it("reads before toggling, so setting the same state twice toggles once", async () => {
    const { id } = await logmeal.signUp("snk_test_2", "eng");
    await logmeal.setEnabled(id, false);
    await logmeal.setEnabled(id, false);
    assert.equal(fake.users.get(id)?.enabled, false);
    assert.equal(fake.calls.filter((c) => c.path === `/v2/users/toggleEnabled/${id}`).length, 1);
    await logmeal.setEnabled(id, true);
    assert.equal(fake.users.get(id)?.enabled, true);
  });

  it("delete is idempotent", async () => {
    const { id } = await logmeal.signUp("snk_test_3", "eng");
    await logmeal.deleteUser(id);
    await logmeal.deleteUser(id);
    assert.equal(fake.users.has(id), false);
  });

  it("reads company credits from the free limitations endpoint", async () => {
    fake.setCredits(150, 50);
    assert.deepEqual(await logmeal.credits(), { used: 150, remaining: 50 });
  });

  it("maps errors, including LogMeal's empty 401 body, without leaking the token", async () => {
    const wrong = new HttpLogMealCompany(fake.url, () => "secret-wrong-token");
    const error = await wrong.signUp("x", "eng").catch((e: unknown) => e);
    assert.ok(error instanceof LogMealError);
    assert.equal(error.status, 401);
    assert.ok(!String(error).includes("secret-wrong-token"));
    const conflict = await logmeal.signUp("snk_test_1", "eng").catch((e: unknown) => e);
    assert.ok(conflict instanceof LogMealError);
    assert.equal(conflict.status, 409);
  });

  it("test-user mode returns the test token and never manages users", async () => {
    const before = fake.calls.length;
    const testUser = new TestUserLogMeal(75682, () => "the-test-token", fake.url);
    assert.deepEqual(await testUser.signUp("ignored", "eng"), { id: 75682, token: "the-test-token" });
    await testUser.setProfile(75682, "eng", "RS");
    await testUser.setEnabled(75682, false);
    await testUser.deleteUser(75682);
    assert.equal(fake.calls.length, before);
  });
});

describe("token cipher", () => {
  it("round-trips and binds the ciphertext to its client", async () => {
    const cipher = new EmulatorTokenCipher();
    const ciphertext = await cipher.encrypt("apiuser-token", "client-a");
    assert.ok(!ciphertext.includes("apiuser-token"));
    assert.equal(await cipher.decrypt(ciphertext, "client-a"), "apiuser-token");
    await assert.rejects(cipher.decrypt(ciphertext, "client-b"));
  });
});

describe("HTTP shell", () => {
  let broker: TestBroker;
  before(async () => {
    broker = await startBroker();
  });
  after(() => broker.close());

  it("answers unknown routes with a JSON 404", async () => {
    assert.deepEqual(await call(broker, "GET", "/v1/nothing"), { status: 404, body: { error: "not_found", message: "Not found" } });
  });

  it("rejects malformed JSON with 400", async () => {
    const res = await call(broker, "POST", "/v1/nothing", { body: "{not json" });
    assert.equal(res.status, 400);
    assert.equal(res.body?.error, "invalid_request");
  });
});
