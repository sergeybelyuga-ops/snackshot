import { randomBytes } from "node:crypto";
import { FieldValue, Timestamp, type DocumentSnapshot, type Firestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import type { Deps } from "../app.ts";
import { HttpError, unavailable } from "../http/errors.ts";
import { LogMealError } from "../logmeal/client.ts";
import { writeAudit } from "../store/audit.ts";
import { LeaseBusy, withClientLease } from "../store/lease.ts";
import { type ClientDoc, type ClientStatus, type DeviceDoc, readSettings, refs, SLOT_STATUSES } from "../store/model.ts";

export type ProvisionInput = { uid: string; deviceId: string; language: string; country: string };

/** What the app sees (doc 11 §8). `provisioning` is never shown: the call waits or fails. */
export type ClientView = {
  status: Exclude<ClientStatus, "provisioning">;
  reason: string;
  token?: string;
  tokenVersion: number;
  pictures: { used: number; limit: number };
  isTest: boolean;
};

/** Statuses whose APIUser is enabled, so the app gets the token. */
const TOKEN_STATUSES: ClientStatus[] = ["trial", "active"];

export async function findClientByUid(db: Firestore, uid: string): Promise<DocumentSnapshot | null> {
  const snap = await refs(db).clients().where("authUids", "array-contains", uid).limit(1).get();
  return snap.docs[0] ?? null;
}

/**
 * `POST /v1/logmeal/provision` (doc 11 §5.3): idempotent. Returns the caller's client, re-attaches
 * a reinstall through the device, or creates a client: Trial when a pilot slot is free,
 * Waitlisted otherwise, TrialEnded when this device already used its pictures.
 */
export async function provision(deps: Deps, input: ProvisionInput): Promise<ClientView> {
  const { db } = deps;
  let clientId = (await findClientByUid(db, input.uid))?.id ?? null;
  if (!clientId) clientId = await attachOrCreate(deps, input);

  let client = await getClient(db, clientId);
  if (client.status === "waitlisted") client = await promoteIfFirst(deps, clientId, client);
  if (client.status === "provisioning") client = await completeProvisioning(deps, clientId, input);
  return viewOf(deps, clientId, client, { includeToken: true });
}

/** `GET /v1/logmeal/status`: the token only when the app's copy is out of date. */
export async function status(deps: Deps, uid: string, knownTokenVersion: number | null): Promise<ClientView> {
  const snap = await findClientByUid(deps.db, uid);
  if (!snap) throw new HttpError(404, "not_provisioned", "No AI access yet; call provision on the first AI action");
  const client = snap.data() as ClientDoc;
  if (client.status === "provisioning") throw new LeaseBusy();
  return viewOf(deps, snap.id, client, { includeToken: knownTokenVersion !== client.tokenVersion });
}

async function attachOrCreate(deps: Deps, input: ProvisionInput): Promise<string> {
  const { db, config } = deps;
  const r = refs(db);
  const settings = await readSettings(db);
  const now = Timestamp.fromDate(deps.now());

  return db.runTransaction(async (tx) => {
    const deviceSnap = await tx.get(r.device(input.deviceId));
    const device = deviceSnap.data() as DeviceDoc | undefined;

    // Reinstall or cleared data on the same phone: same client, same picture counter.
    if (device?.clientId) {
      const clientRef = r.client(device.clientId);
      if ((await tx.get(clientRef)).exists) {
        tx.update(clientRef, { authUids: FieldValue.arrayUnion(input.uid) });
        return device.clientId;
      }
    }

    // A new client. Every such transaction reads and writes the pilot lock, so they run one
    // at a time and the slot count below cannot race (doc 11 §6.3: at most maxClients).
    // Firestore wants every read before the first write.
    await tx.get(r.pilotLock());
    const used = await countSlots(db, tx);
    tx.set(r.pilotLock(), { touchedAt: now });
    const isTest = config.mode !== "company"; // dev and local clients all use the test APIUser
    const priorPictures = device?.picturesUsed ?? 0;

    let status: ClientStatus;
    let reason: string;
    if (priorPictures >= settings.pictureAllowance) {
      status = "trial_ended";
      reason = "device_allowance_used"; // "Delete all data" does not grant a new trial
    } else if (!isTest && used >= settings.maxClients) {
      status = "waitlisted";
      reason = "pilot_full";
    } else {
      status = "provisioning";
      reason = "provisioning";
    }

    const clientRef = r.clients().doc();
    const client: ClientDoc = {
      status,
      deviceId: input.deviceId,
      authUids: [input.uid],
      picturesUsed: priorPictures,
      pictureAllowance: settings.pictureAllowance,
      logmealUserId: null,
      logmealUsername: null,
      tokenCiphertext: null,
      tokenVersion: 0,
      plan: "trial",
      language: input.language,
      country: input.country,
      isTest,
      createdAt: now,
      statusChangedAt: now,
      statusReason: reason,
      waitlistedAt: status === "waitlisted" ? now : null,
      lease: null,
    };
    tx.create(clientRef, client);
    tx.set(r.device(input.deviceId), { clientId: clientRef.id, picturesUsed: priorPictures } satisfies DeviceDoc);
    writeAudit(db, { clientId: clientRef.id, action: "client_created", actor: "provision", reason, before: null, after: client }, deps.now(), tx);
    return clientRef.id;
  });
}

/** A waitlisted client gets the next free slot when it is first in line (doc 11 §5.1). */
async function promoteIfFirst(deps: Deps, clientId: string, client: ClientDoc): Promise<ClientDoc> {
  const { db } = deps;
  const r = refs(db);
  const settings = await readSettings(db);
  const promoted = await db.runTransaction(async (tx) => {
    await tx.get(r.pilotLock());
    const used = await countSlots(db, tx);
    const first = await tx.get(r.clients().where("status", "==", "waitlisted").orderBy("waitlistedAt").limit(1));
    if (used >= settings.maxClients || first.docs[0]?.id !== clientId) return false;
    tx.set(r.pilotLock(), { touchedAt: Timestamp.fromDate(deps.now()) });
    tx.update(r.client(clientId), { status: "provisioning", statusReason: "slot_available", statusChangedAt: Timestamp.fromDate(deps.now()) });
    writeAudit(db, { clientId, action: "waitlist_promoted", actor: "provision", reason: "slot_available", before: client, after: { status: "provisioning" } }, deps.now(), tx);
    return true;
  });
  return promoted ? getClient(db, clientId) : client;
}

/**
 * Creates the APIUser while holding the client's lease, so a double tap or a retry never makes
 * two. Each step is stored before the next, so a retry resumes instead of starting over.
 */
async function completeProvisioning(deps: Deps, clientId: string, input: ProvisionInput): Promise<ClientDoc> {
  const { db, logmeal, cipher } = deps;
  const ref = refs(db).client(clientId);
  try {
    await withClientLease(db, clientId, deps.now, async () => {
      let client = await getClient(db, clientId);
      if (client.status !== "provisioning") return;

      if (!client.tokenCiphertext) {
        const username = `snk_${randomBytes(8).toString("hex")}`; // opaque, no personal data
        const user = await logmeal.signUp(username, client.language);
        await ref.update({
          logmealUserId: user.id,
          logmealUsername: username,
          tokenCiphertext: await cipher.encrypt(user.token, clientId),
          tokenVersion: FieldValue.increment(1),
        });
        client = await getClient(db, clientId);
      }
      await logmeal.setProfile(client.logmealUserId!, client.language || input.language, client.country || input.country);

      const now = Timestamp.fromDate(deps.now());
      await ref.update({ status: "trial", statusReason: "provisioned", statusChangedAt: now, logmealEnabled: true }); // signUp creates it enabled
      await writeAudit(db, { clientId, action: "provisioned", actor: "provision", reason: "provisioned", before: { status: "provisioning" }, after: { status: "trial", logmealUserId: client.logmealUserId } }, deps.now());
    });
  } catch (error) {
    if (error instanceof LogMealError) {
      logger.warn("Provisioning failed at LogMeal; the client stays provisioning for a retry", { clientId, status: error.status, code: error.code });
      throw unavailable("logmeal_unavailable", "AI access could not be set up; try again later");
    }
    throw error;
  }
  const client = await getClient(db, clientId);
  if (client.status === "provisioning") throw new LeaseBusy();
  return client;
}

async function countSlots(db: Firestore, tx: FirebaseFirestore.Transaction): Promise<number> {
  const snap = await tx.get(refs(db).clients().where("isTest", "==", false).where("status", "in", SLOT_STATUSES));
  return snap.size;
}

export async function getClient(db: Firestore, clientId: string): Promise<ClientDoc> {
  const snap = await refs(db).client(clientId).get();
  if (!snap.exists) throw new HttpError(404, "client_not_found", "Client not found");
  return snap.data() as ClientDoc;
}

export async function viewOf(deps: Deps, clientId: string, client: ClientDoc, options: { includeToken: boolean }): Promise<ClientView> {
  if (client.status === "provisioning") throw new LeaseBusy();
  const view: ClientView = {
    status: client.status,
    reason: client.statusReason,
    tokenVersion: client.tokenVersion,
    pictures: { used: client.picturesUsed, limit: client.pictureAllowance },
    isTest: client.isTest,
  };
  if (options.includeToken && TOKEN_STATUSES.includes(client.status) && client.tokenCiphertext) {
    view.token = await deps.cipher.decrypt(client.tokenCiphertext, clientId);
  }
  return view;
}
