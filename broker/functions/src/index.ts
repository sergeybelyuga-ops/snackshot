import { getApps, initializeApp } from "firebase-admin/app";
import { getAppCheck } from "firebase-admin/app-check";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import type { Express } from "express";
import { createApp, type Deps } from "./app.ts";
import { reconcilePending } from "./clients/logmeal-sync.ts";
import { loadConfig, REGION } from "./config.ts";
import { EmulatorTokenCipher, KmsTokenCipher } from "./crypto/token-cipher.ts";
import { createLogMeal } from "./logmeal/client.ts";

// Each project holds only the secret its mode needs (doc 12 §9), so only that one is declared.
const companyToken = defineSecret("LOGMEAL_COMPANY_TOKEN");
const testUserToken = defineSecret("LOGMEAL_TEST_USER_TOKEN");
const mode = process.env.LOGMEAL_MODE;
const secrets = mode === "company" ? [companyToken] : mode === "test-user" ? [testUserToken] : [];

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
    logmeal: createLogMeal(config, { companyToken: () => companyToken.value(), testUserToken: () => testUserToken.value() }),
    cipher: config.kmsKeyName ? new KmsTokenCipher(config.kmsKeyName) : new EmulatorTokenCipher(),
    now: () => new Date(),
  };
  return deps;
}

function getApp(): Express {
  app ??= createApp(getDeps());
  return app;
}

/** Shared by every function: the broker account, its LogMeal secret, Frankfurt. */
const runtime = { region: REGION, serviceAccount: "broker@", secrets, memory: "256MiB" } as const;

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
