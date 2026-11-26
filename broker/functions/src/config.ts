/**
 * Broker configuration, read once from the environment. Firebase loads `.env.<projectId>` for
 * deploys and the emulator; secrets come from Secret Manager (docs/Architecture/12 §3.3, §9).
 */
export type LogMealMode = "fake" | "test-user" | "company";

export type BrokerConfig = {
  projectId: string;
  mode: LogMealMode;
  logmealBaseUrl: string;
  /** The test APIUser id in `test-user` mode. */
  testUserId: number | null;
  appCheckRequired: boolean;
  /** Cloud KMS key that encrypts APIUser tokens at rest; null only in `fake` mode on the emulator. */
  kmsKeyName: string | null;
  emulator: boolean;
};

export const REGION = "europe-west3";

export function loadConfig(env: NodeJS.ProcessEnv = process.env): BrokerConfig {
  const projectId = env.GCLOUD_PROJECT ?? env.GOOGLE_CLOUD_PROJECT ?? "demo-snackshot";
  const emulator = env.FUNCTIONS_EMULATOR === "true" || projectId.startsWith("demo-");
  const mode = parseMode(env.LOGMEAL_MODE);
  const logmealBaseUrl = (env.LOGMEAL_BASE_URL ?? "https://api.logmeal.com").replace(/\/+$/, "");
  const appCheckRequired = env.APP_CHECK_REQUIRED !== "false";

  // Guard rails: the fake and the cleartext URL never run against a real project, and the
  // company mode never runs outside prod (the APICompany token exists only there).
  if (mode === "fake" && !emulator) throw new Error("LOGMEAL_MODE=fake is for the emulator only");
  if (!emulator && !logmealBaseUrl.startsWith("https://")) throw new Error("LogMeal must be reached over HTTPS");
  if (!emulator && !appCheckRequired) throw new Error("App Check is required outside the emulator");
  if (mode === "company" && projectId !== "snackshot-prod" && !emulator) {
    throw new Error("LOGMEAL_MODE=company runs only in snackshot-prod");
  }

  const testUserId = env.LOGMEAL_TEST_USER_ID ? Number(env.LOGMEAL_TEST_USER_ID) : null;
  if (mode === "test-user" && !Number.isInteger(testUserId)) throw new Error("LOGMEAL_TEST_USER_ID is required in test-user mode");

  return {
    projectId,
    mode,
    logmealBaseUrl,
    testUserId,
    appCheckRequired,
    kmsKeyName: emulator && mode === "fake" ? null : `projects/${projectId}/locations/${REGION}/keyRings/broker/cryptoKeys/apiuser-token`,
    emulator,
  };
}

function parseMode(value: string | undefined): LogMealMode {
  if (value === "fake" || value === "test-user" || value === "company") return value;
  throw new Error(`LOGMEAL_MODE must be fake, test-user or company, not ${value ?? "unset"}`);
}
