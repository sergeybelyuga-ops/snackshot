import { Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import type { Deps } from "../app.ts";
import { readSettings, refs } from "../store/model.ts";

/**
 * The structured log event the Cloud Monitoring alert policy matches to send the e-mail
 * (broker/scripts/setup-credit-alert.ts). Keep it in step with that policy's filter.
 */
export const CREDIT_ALARM_EVENT = "logmeal_credit_alarm";

export type CreditCheck = {
  month: string;
  used: number;
  remaining: number;
  fraction: number;
  /** The highest threshold crossed this month, 0 if none. */
  level: number;
  alarmed: boolean;
};

/**
 * Scheduled credit check (doc 11 §6.3): reads the company credit counters from LogMeal's free
 * `info/limitations` headers and raises one alarm per threshold per calendar month (70 %, 90 %,
 * and 100 % of the monthly credits). The state lives in `settings/credit_alarm`.
 */
export async function checkCredits(deps: Deps): Promise<CreditCheck | null> {
  const credits = await deps.logmeal.credits();
  if (!credits) {
    logger.warn("LogMeal returned no credit headers; credit check skipped");
    return null;
  }
  const settings = await readSettings(deps.db);
  const now = deps.now();
  const month = now.toISOString().slice(0, 7);
  // The headers carry the real plan size; the setting is the fallback if they ever disagree.
  const total = credits.used + credits.remaining || settings.monthlyCredits;
  const fraction = total > 0 ? credits.used / total : 1;
  const thresholds = [...settings.creditAlarmThresholds, 1].sort((a, b) => a - b);
  const level = thresholds.filter((t) => fraction >= t).pop() ?? 0;

  const ref = refs(deps.db).creditAlarm();
  const alarmed = await deps.db.runTransaction(async (tx) => {
    const state = (await tx.get(ref)).data() as { month?: string; level?: number } | undefined;
    const previous = state?.month === month ? (state.level ?? 0) : 0;
    tx.set(ref, { month, level: Math.max(previous, level), used: credits.used, remaining: credits.remaining, checkedAt: Timestamp.fromDate(now) });
    return level > previous;
  });

  const result = { month, used: credits.used, remaining: credits.remaining, fraction, level, alarmed };
  if (alarmed) {
    // Severity WARNING or higher; the alert policy matches the event name.
    logger.warn(`LogMeal credits at ${Math.round(fraction * 100)} % of the monthly plan`, { event: CREDIT_ALARM_EVENT, ...result });
  } else {
    logger.info("LogMeal credit check", result);
  }
  return result;
}
