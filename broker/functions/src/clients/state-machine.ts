import { Timestamp } from "firebase-admin/firestore";
import type { Deps } from "../app.ts";
import { HttpError } from "../http/errors.ts";
import { writeAudit } from "../store/audit.ts";
import { withClientLease } from "../store/lease.ts";
import { type ClientDoc, type ClientStatus, readSettings, refs, SLOT_STATUSES } from "../store/model.ts";
import { shouldBeEnabled, syncUnderLease } from "./logmeal-sync.ts";
import { completeProvisioning } from "./provisioning.ts";

/** The public statuses a transition can target; `provisioning` is reached only through them. */
export type TargetStatus = Exclude<ClientStatus, "provisioning">;

/**
 * Transitions the system may make on its own (doc 11 §5.1 state diagram, §5.2 triggers).
 * Admins may make any transition (§5.2 "Admin → Any") except leaving Cancelled, which is final:
 * its APIUser is deleted.
 */
export const ALLOWED: Record<ClientStatus, TargetStatus[]> = {
  provisioning: [],
  waitlisted: ["trial"],
  trial: ["trial_ended", "active", "suspended", "paused"],
  trial_ended: ["active", "cancelled"],
  active: ["paused", "suspended", "cancelled"],
  paused: ["active", "cancelled"],
  suspended: ["trial", "active"],
  cancelled: [],
};

export type TransitionOptions = { actor: string; reason: string; admin?: boolean };

export class TransitionError extends HttpError {
  constructor(from: ClientStatus, to: TargetStatus) {
    super(409, "invalid_transition", `A ${from} client cannot become ${to}`);
  }
}

/**
 * Moves a client to [to] as one serialized job (doc 11 §5.1): under the client lease, the new
 * status, its audit record and `logmealPending` are written in one transaction, then LogMeal is
 * brought in step. If LogMeal fails the status stays (it is the intent) and the change is
 * retried; `toggleEnabled` is never called blindly.
 */
export async function transition(deps: Deps, clientId: string, to: TargetStatus, options: TransitionOptions): Promise<ClientDoc> {
  const { db } = deps;
  const r = refs(db);

  await withClientLease(db, clientId, deps.now, async () => {
    const settings = await readSettings(db);
    const changed = await db.runTransaction(async (tx) => {
      const snap = await tx.get(r.client(clientId));
      if (!snap.exists) throw new HttpError(404, "client_not_found", "Client not found");
      const client = snap.data() as ClientDoc;
      if (client.status === to) return false; // idempotent
      const permitted = options.admin ? client.status !== "cancelled" && client.status !== "provisioning" : ALLOWED[client.status].includes(to);
      if (!permitted) throw new TransitionError(client.status, to);

      // An enabled status without an APIUser (an approved waitlisted client) is provisioned,
      // and only into a free pilot slot.
      const needsApiUser = shouldBeEnabled(to) && client.logmealUserId === null;
      if (needsApiUser && !client.isTest && SLOT_STATUSES.indexOf(client.status) < 0) {
        await tx.get(r.pilotLock());
        const used = (await tx.get(r.clients().where("isTest", "==", false).where("status", "in", SLOT_STATUSES))).size;
        if (used >= settings.maxClients) throw new HttpError(409, "pilot_full", "No pilot slot is free");
        tx.set(r.pilotLock(), { touchedAt: Timestamp.fromDate(deps.now()) });
      }

      const next = needsApiUser ? "provisioning" : to;
      const update: Partial<ClientDoc> = {
        status: next,
        statusReason: options.reason,
        statusChangedAt: Timestamp.fromDate(deps.now()),
        logmealPending: !needsApiUser,
      };
      if (to === "cancelled") update.tokenCiphertext = null; // the token is wiped at once; the APIUser follows
      tx.update(r.client(clientId), update);
      writeAudit(db, { clientId, action: `status_${to}`, actor: options.actor, reason: options.reason, before: client, after: { ...client, ...update, status: to } }, deps.now(), tx);
      return true;
    });
    if (!changed) return;

    const client = (await r.client(clientId).get()).data() as ClientDoc;
    if (client.status === "provisioning") {
      await completeProvisioning(deps, clientId, { leaseHeld: true, targetStatus: to as "trial" | "active" });
      return;
    }
    await syncUnderLease(deps, clientId).catch(() => false); // retried by the hourly job
  });

  return (await r.client(clientId).get()).data() as ClientDoc;
}
