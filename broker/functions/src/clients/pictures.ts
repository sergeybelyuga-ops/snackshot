import { Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import type { Deps } from "../app.ts";
import { HttpError, unavailable } from "../http/errors.ts";
import { LogMealError } from "../logmeal/client.ts";
import { writeAudit } from "../store/audit.ts";
import { withClientLease } from "../store/lease.ts";
import { type ClientDoc, type DeviceDoc, refs } from "../store/model.ts";
import { syncLogMealEnabled } from "./logmeal-sync.ts";
import { findClientByUid } from "./provisioning.ts";

/** Picture reports are kept this long for idempotency, then removed by a Firestore TTL policy. */
const REPORT_TTL_DAYS = 30;

/**
 * `POST /v1/logmeal/picture` (doc 11 §6.1a fast path): one successful recognition. Idempotent by
 * `requestId`, so the app can retry a report after being offline. At the allowance the client
 * becomes TrialEnded and its APIUser is disabled; a recognition already in flight completes.
 */
export async function reportPicture(deps: Deps, uid: string, requestId: string) {
  const snap = await findClientByUid(deps.db, uid);
  if (!snap) throw new HttpError(404, "not_provisioned", "No AI access yet");
  const clientId = snap.id;
  const r = refs(deps.db);
  const now = deps.now();

  const result = await deps.db.runTransaction(async (tx) => {
    const clientSnap = await tx.get(r.client(clientId));
    const client = clientSnap.data() as ClientDoc;
    const reportRef = deps.db.collection("picture_reports").doc(`${clientId}_${requestId}`);
    const report = await tx.get(reportRef);
    const deviceSnap = await tx.get(r.device(client.deviceId));
    if (report.exists) return { client, ended: false };
    if (client.status === "waitlisted" || client.status === "provisioning" || client.status === "cancelled") {
      throw new HttpError(409, "no_ai_access", "This client has no AI access to report pictures for");
    }

    const picturesUsed = client.picturesUsed + 1;
    const ended = client.status === "trial" && picturesUsed >= client.pictureAllowance;
    const update: Partial<ClientDoc> = { picturesUsed };
    if (ended) Object.assign(update, { status: "trial_ended", statusReason: "trial_allowance_used", statusChangedAt: Timestamp.fromDate(now) });

    tx.create(reportRef, { clientId, at: Timestamp.fromDate(now), expireAt: Timestamp.fromMillis(now.getTime() + REPORT_TTL_DAYS * 86_400_000) });
    tx.update(r.client(clientId), update);
    // The device keeps the higher count, so Delete all data cannot reset the trial.
    const device = deviceSnap.data() as DeviceDoc | undefined;
    tx.set(r.device(client.deviceId), { clientId, picturesUsed: Math.max(device?.picturesUsed ?? 0, picturesUsed) } satisfies DeviceDoc);
    if (ended) writeAudit(deps.db, { clientId, action: "trial_ended", actor: "picture", reason: "trial_allowance_used", before: client, after: { ...client, ...update } }, now, tx);
    return { client: { ...client, ...update } as ClientDoc, ended };
  });

  if (result.ended) await syncLogMealEnabled(deps, clientId); // retried later if LogMeal is down
  return { status: result.client.status, pictures: { used: result.client.picturesUsed, limit: result.client.pictureAllowance } };
}

/**
 * `DELETE /v1/account` (doc 11 §5.3): "Delete all data" in Settings. Deletes the APIUser and
 * erases the client record; only `devices/{deviceId}.picturesUsed` stays, so the device does
 * not get a new trial. The APIUser goes first: if LogMeal fails, nothing is erased and the app
 * retries.
 */
export async function deleteAccount(deps: Deps, uid: string): Promise<void> {
  const snap = await findClientByUid(deps.db, uid);
  if (!snap) return; // nothing to delete: already gone, or never used AI
  const clientId = snap.id;
  const r = refs(deps.db);

  await withClientLease(deps.db, clientId, deps.now, async () => {
    const client = (await r.client(clientId).get()).data() as ClientDoc | undefined;
    if (!client) return;
    if (client.logmealUserId !== null) {
      try {
        await deps.logmeal.deleteUser(client.logmealUserId);
      } catch (error) {
        if (!(error instanceof LogMealError)) throw error;
        logger.warn("Account deletion failed at LogMeal; nothing erased", { clientId, status: error.status });
        throw unavailable("logmeal_unavailable", "AI access could not be removed; try again later");
      }
    }
    await deps.db.runTransaction(async (tx) => {
      const deviceSnap = await tx.get(r.device(client.deviceId));
      const device = deviceSnap.data() as DeviceDoc | undefined;
      tx.set(r.device(client.deviceId), { clientId: null, picturesUsed: Math.max(device?.picturesUsed ?? 0, client.picturesUsed) } satisfies DeviceDoc);
      tx.delete(r.client(clientId));
      writeAudit(deps.db, { clientId, action: "account_deleted", actor: "app", reason: "delete_all_data", before: client, after: null }, deps.now(), tx);
    });
  });
}
