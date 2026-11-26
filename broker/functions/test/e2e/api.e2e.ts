import assert from "node:assert/strict";
import { after, before, describe, it } from "node:test";
import { startFakeLogMeal, type FakeLogMeal } from "../../../../tools/fake-logmeal/server.ts";
import { anonymousIdToken, clearEmulators, deviceId, requireEmulators } from "../harness.ts";

/**
 * The built `api` function in the Functions emulator, as deployed: index.ts wiring, the
 * `.env.demo-snackshot` file (fake mode, fake LogMeal on :8090), and the emulator kill switch.
 * Run with `npm run test:e2e` in broker/.
 */
const API = "http://127.0.0.1:5001/demo-snackshot/europe-west3/api";

async function request(method: string, path: string, idToken: string, body?: unknown) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: { Authorization: `Bearer ${idToken}`, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  return { status: res.status, body: text ? JSON.parse(text) : null };
}

describe("api function in the Functions emulator", () => {
  let fake: FakeLogMeal;
  before(async () => {
    requireEmulators();
    await clearEmulators();
    fake = await startFakeLogMeal({ port: 8090 });
  });
  after(() => fake.close());

  it("provisions, reports a picture, shows status, and deletes the account", async () => {
    const { idToken } = await anonymousIdToken();
    const provisioned = await request("POST", "/v1/logmeal/provision", idToken, { deviceId: deviceId("e2e"), language: "eng", country: "RS" });
    assert.equal(provisioned.status, 200, JSON.stringify(provisioned.body));
    assert.equal(provisioned.body.status, "trial");
    assert.equal(provisioned.body.isTest, true, "fake mode: every client is a test client");
    assert.equal(provisioned.body.aiEnabled, true);
    assert.match(provisioned.body.token, /^fake-token-/);
    assert.equal(fake.users.size, 1);

    const picture = await request("POST", "/v1/logmeal/picture", idToken, { requestId: "e2e-request-1" });
    assert.deepEqual(picture.body, { status: "trial", pictures: { used: 1, limit: 90 } });

    const status = await request("GET", "/v1/logmeal/status?knownTokenVersion=1", idToken);
    assert.equal(status.body.status, "trial");
    assert.equal(status.body.token, undefined);

    assert.equal((await request("DELETE", "/v1/account", idToken)).status, 204);
    assert.equal(fake.users.size, 0);
  });

  it("rejects callers without an anonymous ID token", async () => {
    assert.equal((await request("GET", "/v1/logmeal/status", "not-a-token")).status, 401);
  });
});
