import { getApps, initializeApp } from "firebase-admin/app";
import { getAppCheck } from "firebase-admin/app-check";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { createHash } from "node:crypto";
import type { AddressInfo } from "node:net";
import type { Server } from "node:http";
import { FAKE_COMPANY_TOKEN, startFakeLogMeal, type FakeLogMeal } from "../../../tools/fake-logmeal/server.ts";
import { createApp, type Deps } from "../src/app.ts";
import { loadConfig, type LogMealMode } from "../src/config.ts";
import { EmulatorTokenCipher } from "../src/crypto/token-cipher.ts";
import { createLogMeal } from "../src/logmeal/client.ts";

export const PROJECT_ID = "demo-snackshot";

/**
 * Tests run inside `firebase emulators:exec --only auth,firestore`, which sets
 * FIRESTORE_EMULATOR_HOST and FIREBASE_AUTH_EMULATOR_HOST. LogMeal is the in-process fake.
 */
export function requireEmulators() {
  if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST) {
    throw new Error("Run the broker tests with `npm run test:emulators` (Auth and Firestore emulators).");
  }
  if (getApps().length === 0) initializeApp({ projectId: PROJECT_ID });
}

export async function clearEmulators() {
  const firestore = process.env.FIRESTORE_EMULATOR_HOST;
  const auth = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  await fetch(`http://${firestore}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, { method: "DELETE" });
  await fetch(`http://${auth}/emulator/v1/projects/${PROJECT_ID}/accounts`, { method: "DELETE" });
}

export type TestBroker = {
  url: string;
  fake: FakeLogMeal;
  deps: Deps;
  /** Moves the broker clock. */
  setNow(date: Date): void;
  close(): Promise<void>;
};

/**
 * The broker app on a random port against a fresh fake LogMeal. `company` mode (the default
 * here) behaves like prod: real pilot cap, clients not marked as test. `fake` mode is what the
 * local emulator runs, where every client is a test client.
 */
export async function startBroker(options: { mode?: LogMealMode; appCheckRequired?: boolean } = {}, overrides: Partial<Deps> = {}): Promise<TestBroker> {
  requireEmulators();
  const fake = await startFakeLogMeal();
  const config = loadConfig({
    GCLOUD_PROJECT: PROJECT_ID,
    FUNCTIONS_EMULATOR: "true",
    LOGMEAL_MODE: options.mode ?? "company",
    LOGMEAL_BASE_URL: fake.url,
    APP_CHECK_REQUIRED: options.appCheckRequired ? "true" : "false",
  });
  let now = new Date("2026-11-26T12:00:00Z");
  const deps: Deps = {
    config,
    db: getFirestore(),
    auth: getAuth(),
    appCheck: getAppCheck(),
    logmeal: createLogMeal(config, { companyToken: () => FAKE_COMPANY_TOKEN, testUserToken: () => "unused" }),
    cipher: new EmulatorTokenCipher(),
    now: () => now,
    ...overrides,
  };
  const server: Server = await new Promise((resolve) => {
    const s = createApp(deps).listen(0, "127.0.0.1", () => resolve(s));
  });
  return {
    url: `http://127.0.0.1:${(server.address() as AddressInfo).port}`,
    fake,
    deps,
    setNow: (date) => {
      now = date;
    },
    close: async () => {
      await new Promise((resolve) => server.close(resolve));
      await fake.close();
    },
  };
}

/** Signs in a new anonymous user on the Auth emulator and returns its ID token. */
export async function anonymousIdToken(): Promise<{ uid: string; idToken: string }> {
  const res = await fetch(
    `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key`,
    { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ returnSecureToken: true }) },
  );
  const body = (await res.json()) as { localId: string; idToken: string };
  return { uid: body.localId, idToken: body.idToken };
}

/** Signs in an email user (not anonymous) on the Auth emulator. */
export async function emailIdToken(email: string): Promise<string> {
  const res = await fetch(
    `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key`,
    { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password: "password-123", returnSecureToken: true }) },
  );
  return ((await res.json()) as { idToken: string }).idToken;
}

/** A device ID as the app sends it: a SHA-256 hex digest. */
export function deviceId(name: string): string {
  return createHash("sha256").update(`test-device-${name}`).digest("hex");
}

export async function call(broker: TestBroker, method: string, path: string, options: { idToken?: string; body?: unknown } = {}) {
  const res = await fetch(`${broker.url}${path}`, {
    method,
    headers: {
      ...(options.idToken ? { Authorization: `Bearer ${options.idToken}` } : {}),
      ...(options.body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: options.body === undefined ? undefined : typeof options.body === "string" ? options.body : JSON.stringify(options.body),
  });
  const text = await res.text();
  return { status: res.status, body: text ? (JSON.parse(text) as Record<string, any>) : null };
}
