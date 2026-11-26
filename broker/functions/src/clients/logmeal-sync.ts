import { FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import type { Deps } from "../app.ts";
import { LeaseBusy, withClientLease } from "../store/lease.ts";
import { type ClientDoc, type ClientStatus, refs } from "../store/model.ts";

/** Statuses whose APIUser is enabled in LogMeal (doc 11 §5.1); every other status is disabled. */
export const ENABLED_STATUSES: ClientStatus[] = ["trial", "active"];

export const shouldBeEnabled = (status: ClientStatus) => ENABLED_STATUSES.includes(status);

/**
 * Brings LogMeal in step with the registry, which holds the intent: enables or disables the
 * APIUser (reading before toggling), or deletes it for a cancelled client and wipes the token.
 * A change sets `logmealPending`; this clears it once LogMeal agrees. Best effort: a failure is
 * retried by the next request and by the hourly `reconcileClients` job.
 */
export async function syncLogMealEnabled(deps: Deps, clientId: string): Promise<boolean> {
  try {
    return await withClientLease(deps.db, clientId, deps.now, () => syncUnderLease(deps, clientId));
  } catch (error) {
    if (error instanceof LeaseBusy) return false; // the lease holder finishes the change
    logger.warn("LogMeal not in step yet; will retry", { clientId, error: String(error) });
    return false;
  }
}

/** For callers that already hold the client's lease. */
export async function syncUnderLease(deps: Deps, clientId: string): Promise<boolean> {
  const ref = refs(deps.db).client(clientId);
  const client = (await ref.get()).data() as ClientDoc | undefined;
  if (!client) return true;

  if (client.logmealUserId !== null && client.status === "cancelled") {
    await deps.logmeal.deleteUser(client.logmealUserId); // soft delete in LogMeal; idempotent
    await ref.update({ logmealUserId: null, tokenCiphertext: null, logmealEnabled: FieldValue.delete(), logmealPending: false });
    return true;
  }
  if (client.logmealUserId !== null) {
    const wanted = shouldBeEnabled(client.status);
    if (client.logmealEnabled !== wanted) {
      await deps.logmeal.setEnabled(client.logmealUserId, wanted);
      await ref.update({ logmealEnabled: wanted });
    }
  }
  if (client.logmealPending) await ref.update({ logmealPending: false });
  return true;
}

/** The hourly job: every client whose LogMeal change has not gone through yet. */
export async function reconcilePending(deps: Deps, limit = 50): Promise<{ checked: number; inStep: number }> {
  const snap = await refs(deps.db).clients().where("logmealPending", "==", true).limit(limit).get();
  let inStep = 0;
  for (const doc of snap.docs) if (await syncLogMealEnabled(deps, doc.id)) inStep++;
  return { checked: snap.size, inStep };
}
