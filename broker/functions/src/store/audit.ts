import { Timestamp, type Firestore, type Transaction } from "firebase-admin/firestore";
import type { AuditDoc, ClientDoc } from "./model.ts";
import { refs } from "./model.ts";

/** Fields copied into audit records: state, never tokens or ciphertext. */
const AUDITED: (keyof ClientDoc)[] = ["status", "statusReason", "picturesUsed", "pictureAllowance", "isTest", "logmealUserId", "tokenVersion", "language", "country"];

export function auditView(client: Partial<ClientDoc> | null): Partial<ClientDoc> | null {
  if (!client) return null;
  const view: Record<string, unknown> = {};
  for (const key of AUDITED) if (client[key] !== undefined) view[key] = client[key];
  return view as Partial<ClientDoc>;
}

/** Appends one audit record (doc 11 §8: append-only, every state change and admin action). */
export function writeAudit(
  db: Firestore,
  entry: Omit<AuditDoc, "at" | "before" | "after"> & { before: Partial<ClientDoc> | null; after: Partial<ClientDoc> | null },
  now: Date,
  tx?: Transaction,
) {
  const doc: AuditDoc = { ...entry, before: auditView(entry.before), after: auditView(entry.after), at: Timestamp.fromDate(now) };
  const ref = refs(db).audit();
  if (tx) tx.create(ref, doc);
  else return ref.create(doc);
  return undefined;
}
