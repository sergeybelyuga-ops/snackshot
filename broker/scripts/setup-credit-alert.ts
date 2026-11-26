// Owner-only, run once. Creates the e-mail alert for the LogMeal credit check (WBS 3.6,
// docs/Architecture/11 §6.3) in both projects: an e-mail notification channel and a log-match
// alert policy on the `logmeal_credit_alarm` event that checkLogMealCredits logs at 70 / 90 / 100 %.
// Safe to run again: existing channels and policies are reused.
//
//   node broker/scripts/setup-credit-alert.ts --email you@example.com
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { arg, gcloud } from "./gcloud.ts";

const PROJECTS = ["snackshot-dev-app", "snackshot-prod"];
const POLICY_NAME = "LogMeal credits alarm";
const EVENT = "logmeal_credit_alarm"; // functions/src/cost/credit-check.ts CREDIT_ALARM_EVENT

const email = arg("email");
if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) throw new Error("--email must be an e-mail address");

const dir = mkdtempSync(join(tmpdir(), "snackshot-alert-"));
try {
  for (const project of PROJECTS) {
    const channels = JSON.parse(gcloud(["beta", "monitoring", "channels", "list", `--project=${project}`, "--format=json"])) as {
      name: string;
      type: string;
      labels?: { email_address?: string };
    }[];
    let channel = channels.find((c) => c.type === "email" && c.labels?.email_address === email)?.name;
    if (!channel) {
      channel = gcloud([
        "beta", "monitoring", "channels", "create", `--project=${project}`,
        "--type=email", `--display-name=Snackshot owner`, `--channel-labels=email_address=${email}`, "--format=value(name)",
      ]).trim();
      console.log(`${project}: e-mail channel created`);
    }

    const policies = JSON.parse(gcloud(["alpha", "monitoring", "policies", "list", `--project=${project}`, "--format=json"])) as { displayName: string }[];
    if (policies.some((p) => p.displayName === POLICY_NAME)) {
      console.log(`${project}: alert policy already exists`);
      continue;
    }
    const policy = {
      displayName: POLICY_NAME,
      documentation: {
        mimeType: "text/markdown",
        content:
          "LogMeal company credits crossed an alarm threshold (70 %, 90 % or 100 % of the monthly plan). " +
          "See the `checkLogMealCredits` log entry for used and remaining credits. At 100 % LogMeal answers 429 " +
          "until next month; consider the Remote Config kill switch `ai_recognition_enabled` (docs/Architecture/11 §6.3).",
      },
      combiner: "OR",
      conditions: [{ displayName: `${EVENT} logged`, conditionMatchedLog: { filter: `jsonPayload.event="${EVENT}"` } }],
      alertStrategy: { notificationRateLimit: { period: "3600s" }, autoClose: "86400s" },
      notificationChannels: [channel],
      severity: "WARNING",
    };
    const file = join(dir, `${project}.json`);
    writeFileSync(file, JSON.stringify(policy));
    gcloud(["alpha", "monitoring", "policies", "create", `--project=${project}`, `--policy-from-file=${file}`, "--format=value(name)"]);
    console.log(`${project}: alert policy created`);
  }
} finally {
  rmSync(dir, { recursive: true, force: true });
}
console.log("Done. Google sends a confirmation only for some channel types; e-mail channels work at once.");
