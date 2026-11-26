import { logger } from "firebase-functions";
import type { Deps } from "../app.ts";
import { LeaseBusy, withClientLease } from "../store/lease.ts";
import { type ClientDoc, type ClientStatus, refs } from "../store/model.ts";

/** Statuses whose APIUser is enabled in LogMeal (doc 11 §5.1); every other status is disabled. */
export const ENABLED_STATUSES: ClientStatus[] = ["trial", "active"];

export const shouldBeEnabled = (status: ClientStatus) => ENABLED_STATUSES.includes(status);

/**
 * Makes the APIUser's `enabled` flag in LogMeal match the client's status, under the client
 * lease. The registry is the intent; `logmealEnabled` records what LogMeal was last set to, so a
 * failed call is retried by the next request or the scheduled check. Returns whether it is in step.
 */
export async function syncLogMealEnabled(deps: Deps, clientId: string): Promise<boolean> {
  try {
    return await withClientLease(deps.db, clientId, deps.now, () => syncUnderLease(deps, clientId));
  } catch (error) {
    if (error instanceof LeaseBusy) return false; // the lease holder will finish the change
    logger.warn("LogMeal enabled flag not in step yet; will retry", { clientId, error: String(error) });
    return false;
  }
}

/** For callers that already hold the lease. */
export async function syncUnderLease(deps: Deps, clientId: string): Promise<boolean> {
  const ref = refs(deps.db).client(clientId);
  const client = (await ref.get()).data() as (ClientDoc & { logmealEnabled?: boolean }) | undefined;
  if (!client || client.logmealUserId === null) return true;
  const wanted = shouldBeEnabled(client.status);
  if (client.logmealEnabled === wanted) return true;
  await deps.logmeal.setEnabled(client.logmealUserId, wanted); // reads before it toggles
  await ref.update({ logmealEnabled: wanted });
  return true;
}
