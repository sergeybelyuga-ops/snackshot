import { getApps, initializeApp } from "firebase-admin/app";
import { getAppCheck } from "firebase-admin/app-check";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { getRemoteConfig } from "firebase-admin/remote-config";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import type { Express } from "express";
import { createApp, type Deps } from "./app.ts";
import { reconcilePending } from "./clients/logmeal-sync.ts";
import { loadConfig, REGION } from "./config.ts";
import { checkCredits } from "./cost/credit-check.ts";
import { FirestoreKillSwitch, RemoteConfigKillSwitch } from "./cost/kill-switch.ts";
import { EmulatorTokenCipher, KmsTokenCipher } from "./crypto/token-cipher.ts";
import { createLogMeal } from "./logmeal/client.ts";

// Each project holds only the secret its mode needs (doc 12 §9), so only that one is declared.
// The Firebase CLI requires a value for every defineSecret() it finds, used or not, so the other
// secret must not be defined at all. The CLI loads functions/.env.<projectId> first, so the mode is set.
const mode = process.env.LOGMEAL_MODE;
const companyToken = mode === "company" ? defineSecret("LOGMEAL_COMPANY_TOKEN") : undefined;
const testUserToken = mode === "test-user" ? defineSecret("LOGMEAL_TEST_USER_TOKEN") : undefined;
const secrets = [companyToken, testUserToken].filter((s) => s !== undefined);

function secretValue(secret: ReturnType<typeof defineSecret> | undefined, name: string): string {
  if (!secret) throw new Error(`${name} is not declared in LOGMEAL_MODE=${mode}`);
  return secret.value();
}

let deps: Deps | undefined;
let app: Express | undefined;

function getDeps(): Deps {
  if (deps) return deps;
  const config = loadConfig();
  if (getApps().length === 0) initializeApp();
  deps = {
    config,
    db: getFirestore(),
    auth: getAuth(),
    appCheck: getAppCheck(),
    logmeal: createLogMeal(config, {
      companyToken: () => secretValue(companyToken, "LOGMEAL_COMPANY_TOKEN"),
      testUserToken: () => secretValue(testUserToken, "LOGMEAL_TEST_USER_TOKEN"),
    }),
    cipher: config.kmsKeyName ? new KmsTokenCipher(config.kmsKeyName) : new EmulatorTokenCipher(),
    // Remote Config has no emulator; locally the switch is a field in settings/broker.
    killSwitch: config.emulator ? new FirestoreKillSwitch(getFirestore()) : new RemoteConfigKillSwitch(getRemoteConfig(), () => new Date()),
    now: () => new Date(),
  };
  return deps;
}

function getApp(): Express {
  app ??= createApp(getDeps());
  return app;
}

// The full e-mail from functions/.env.<projectId>: the Firebase CLI grants secret access to the
// service account as written, and Secret Manager rejects the short form "broker@".
const serviceAccount = process.env.BROKER_SERVICE_ACCOUNT ?? (process.env.FUNCTIONS_EMULATOR === "true" ? "broker@" : undefined);
if (!serviceAccount) throw new Error("BROKER_SERVICE_ACCOUNT is not set in functions/.env.<projectId>");

/** Shared by every function: the broker account, its LogMeal secret, Frankfurt. */
const runtime = { region: REGION, serviceAccount, secrets, memory: "256MiB" } as const;

/** `https://europe-west3-<project>.cloudfunctions.net/api/v1/...` (Android BuildConfig.BROKER_BASE_URL). */
export const api = onRequest(
  {
    // Runs as the broker account: Firestore, its own LogMeal secret, and the token KMS key only.
    ...runtime,
    maxInstances: 5,
    timeoutSeconds: 30,
    invoker: "public", // every endpoint checks the Firebase ID token and App Check itself
  },
  (req, res) => getApp()(req, res),
);

/**
 * Hourly: finishes LogMeal changes that failed when the status changed (doc 11 §5.1), so an
 * ended trial is disabled and a cancelled APIUser deleted even if LogMeal was down at the time.
 */
export const reconcileClients = onSchedule({ ...runtime, schedule: "every 60 minutes", timeZone: "Europe/Berlin", maxInstances: 1 }, async () => {
  const result = await reconcilePending(getDeps());
  if (result.checked > 0) logger.info("reconcileClients", result);
});

/**
 * Every 6 hours: company credits against the monthly plan; one alarm per threshold per month,
 * which the Cloud Monitoring alert policy turns into an e-mail (doc 11 §6.3).
 */
export const checkLogMealCredits = onSchedule({ ...runtime, schedule: "every 6 hours", timeZone: "Europe/Berlin", maxInstances: 1 }, async () => {
  await checkCredits(getDeps());
});
