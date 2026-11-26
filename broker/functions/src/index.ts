import { getApps, initializeApp } from "firebase-admin/app";
import { getAppCheck } from "firebase-admin/app-check";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { onRequest } from "firebase-functions/v2/https";
import type { Express } from "express";
import { createApp } from "./app.ts";
import { loadConfig, REGION } from "./config.ts";
import { EmulatorTokenCipher, KmsTokenCipher } from "./crypto/token-cipher.ts";
import { createLogMeal } from "./logmeal/client.ts";

// Each project holds only the secret its mode needs (doc 12 §9), so only that one is declared.
const companyToken = defineSecret("LOGMEAL_COMPANY_TOKEN");
const testUserToken = defineSecret("LOGMEAL_TEST_USER_TOKEN");
const mode = process.env.LOGMEAL_MODE;
const secrets = mode === "company" ? [companyToken] : mode === "test-user" ? [testUserToken] : [];

let app: Express | undefined;

function getApp(): Express {
  if (app) return app;
  const config = loadConfig();
  if (getApps().length === 0) initializeApp();
  app = createApp({
    config,
    db: getFirestore(),
    auth: getAuth(),
    appCheck: getAppCheck(),
    logmeal: createLogMeal(config, { companyToken: () => companyToken.value(), testUserToken: () => testUserToken.value() }),
    cipher: config.kmsKeyName ? new KmsTokenCipher(config.kmsKeyName) : new EmulatorTokenCipher(),
    now: () => new Date(),
  });
  return app;
}

/** `https://europe-west3-<project>.cloudfunctions.net/api/v1/...` (Android BuildConfig.BROKER_BASE_URL). */
export const api = onRequest(
  {
    region: REGION,
    // Runs as the broker account: Firestore, its own LogMeal secret, and the token KMS key only.
    serviceAccount: "broker@",
    secrets,
    memory: "256MiB",
    maxInstances: 5,
    timeoutSeconds: 30,
    invoker: "public", // every endpoint checks the Firebase ID token and App Check itself
  },
  (req, res) => getApp()(req, res),
);
