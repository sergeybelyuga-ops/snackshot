// Owner-only, never in CI. Creates the single LogMeal test APIUser
// (docs/Architecture/12_dev-and-test-environment.md §3.4).
//
// Reads the APICompany token from snackshot-prod Secret Manager into memory,
// calls signUp + modifyUserProfileInfo, and stores the new APIUser token in
// snackshot-dev-app Secret Manager as LOGMEAL_TEST_USER_TOKEN. No token is
// written to disk or printed.
//
//   node broker/scripts/create-test-user.ts --country ES [--language eng]
import { randomBytes } from "node:crypto";
import { arg, putSecret, readSecret, secretExists } from "./gcloud.ts";

const PROD = "snackshot-prod";
const DEV = "snackshot-dev-app";
const LOGMEAL = "https://api.logmeal.com";

const country = arg("country").toUpperCase();
const language = arg("language", "eng");
if (!/^[A-Z]{2}$/.test(country)) throw new Error("--country must be ISO 3166-1 alpha-2, e.g. ES");

if (secretExists(DEV, "LOGMEAL_TEST_USER_TOKEN") && !process.argv.includes("--replace")) {
  throw new Error(
    "LOGMEAL_TEST_USER_TOKEN already exists in dev. Pass --replace to create a new test user " +
      "(then delete the old APIUser in LogMeal).",
  );
}

const companyToken = readSecret(PROD, "LOGMEAL_COMPANY_TOKEN");

async function logmeal(path: string, body: unknown): Promise<any> {
  const res = await fetch(`${LOGMEAL}${path}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${companyToken}`, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${path} → HTTP ${res.status}: ${text.slice(0, 300)}`);
  return text ? JSON.parse(text) : {};
}

const username = `snackshot-test-${new Date().toISOString().slice(0, 10)}-${randomBytes(3).toString("hex")}`;
const created = await logmeal("/v2/users/signUp", { username, language });
if (!created.token || !created.id) throw new Error("signUp returned no token/id");
console.log(`Created APIUser id=${created.id} username=${username}`);

await logmeal(`/v2/users/modifyUserProfileInfo/${created.id}`, { language, country });
console.log(`Profile set: language=${language} country=${country}`);

putSecret(DEV, "LOGMEAL_TEST_USER_TOKEN", created.token);
console.log(`Token stored in ${DEV} Secret Manager as LOGMEAL_TEST_USER_TOKEN.`);
console.log("Record the id/username in test-fixtures/logmeal/README.md. Register it as isTest when the broker exists (WBS 3.x).");
