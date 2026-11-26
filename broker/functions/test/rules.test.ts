import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { after, before, describe, it } from "node:test";
import { assertFails, initializeTestEnvironment, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc } from "firebase/firestore";
import { Collections } from "../src/store/model.ts";

/**
 * firestore.rules is deny-all: the app and the admin console reach broker data only through the
 * functions (doc 11 §8.1). Checked for every collection and every kind of signed-in user.
 */
describe("Firestore rules", () => {
  let env: RulesTestEnvironment;
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080").split(":");

  before(async () => {
    env = await initializeTestEnvironment({
      projectId: "demo-snackshot-rules", // its own project, so the rules never mix with other tests' data
      firestore: { rules: readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8"), host: host!, port: Number(port) },
    });
    await env.withSecurityRulesDisabled(async (ctx) => {
      for (const name of Object.values(Collections)) await setDoc(doc(ctx.firestore(), name, "existing"), { seeded: true });
    });
  });
  after(() => env.cleanup());

  const users = () => ({
    "signed out": env.unauthenticatedContext().firestore(),
    "anonymous app user": env.authenticatedContext("anon-uid", { firebase: { sign_in_provider: "anonymous" } }).firestore(),
    "admin claim": env.authenticatedContext("admin-uid", { admin: true, email: "admin@example.com" }).firestore(),
  });

  for (const name of Object.values(Collections)) {
    it(`denies all client access to ${name}`, async () => {
      for (const [who, db] of Object.entries(users())) {
        const message = `${who} on ${name}`;
        await assertFails(getDoc(doc(db, name, "existing"))).catch(() => assert.fail(`read allowed: ${message}`));
        await assertFails(getDocs(collection(db, name))).catch(() => assert.fail(`list allowed: ${message}`));
        await assertFails(setDoc(doc(db, name, "new"), { x: 1 })).catch(() => assert.fail(`create allowed: ${message}`));
        await assertFails(setDoc(doc(db, name, "existing"), { x: 1 })).catch(() => assert.fail(`update allowed: ${message}`));
        await assertFails(deleteDoc(doc(db, name, "existing"))).catch(() => assert.fail(`delete allowed: ${message}`));
      }
    });
  }

  it("denies collections the broker does not know yet", async () => {
    await assertFails(getDoc(doc(users()["anonymous app user"], "anything", "x")));
  });
});
