import { randomUUID } from "node:crypto";
import { Timestamp, type Firestore } from "firebase-admin/firestore";
import { HttpError } from "../http/errors.ts";
import { refs } from "./model.ts";

/** How long one job may hold a client; a crashed job's lease expires after this. */
export const LEASE_MS = 30_000;

export class LeaseBusy extends HttpError {
  constructor() {
    super(503, "client_busy", "Another change to this client is in progress; retry shortly");
  }
}

/**
 * Runs [job] while holding the client's lease, so LogMeal calls and state changes for one
 * client never interleave (doc 11 §5.1: serialized, idempotent jobs per client). Firestore
 * transactions cannot span LogMeal calls, so the lease is a field on the client document.
 */
export async function withClientLease<T>(db: Firestore, clientId: string, now: () => Date, job: () => Promise<T>): Promise<T> {
  const ref = refs(db).client(clientId);
  const holder = randomUUID();
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new HttpError(404, "client_not_found", "Client not found");
    const lease = snap.get("lease") as { holder: string; until: Timestamp } | undefined;
    if (lease && lease.until.toMillis() > now().getTime()) throw new LeaseBusy();
    tx.update(ref, { lease: { holder, until: Timestamp.fromMillis(now().getTime() + LEASE_MS) } });
  });
  try {
    return await job();
  } finally {
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (snap.exists && (snap.get("lease.holder") as string | undefined) === holder) tx.update(ref, { lease: null });
    });
  }
}
