import type { Firestore, Timestamp } from "firebase-admin/firestore";

/**
 * Firestore collections of the broker (doc 11 §5.3, §8; WBS 3.1). Client SDKs have no access
 * (firestore.rules denies everything); only these functions read and write them.
 */
export const Collections = {
  clients: "clients",
  devices: "devices",
  usage: "usage",
  audit: "audit",
  settings: "settings",
  metricsDaily: "metrics_daily",
  pictureReports: "picture_reports",
} as const;

export type ClientStatus = "trial" | "trial_ended" | "active" | "paused" | "suspended" | "cancelled" | "waitlisted";

/** `clients/{clientId}` */
export type ClientDoc = {
  status: ClientStatus;
  /** SHA-256(ANDROID_ID + app salt), sent by the app; never the raw ANDROID_ID. */
  deviceId: string;
  /** Anonymous Firebase UIDs, one per install on this device. */
  authUids: string[];
  picturesUsed: number;
  pictureAllowance: number;
  logmealUserId: number | null;
  /** Opaque, no personal data. */
  logmealUsername: string | null;
  /** KMS ciphertext; null while waitlisted or after cancellation. */
  tokenCiphertext: string | null;
  tokenVersion: number;
  plan: "trial";
  language: string;
  country: string;
  isTest: boolean;
  createdAt: Timestamp;
  statusChangedAt: Timestamp;
  statusReason: string;
  /** Order on the waitlist: when the device first tried AI. */
  waitlistedAt: Timestamp | null;
};

/** `devices/{deviceId}`: survives "Delete all data", so deleting does not grant a new trial. */
export type DeviceDoc = {
  clientId: string | null;
  picturesUsed: number;
};

/** `settings/broker` */
export type BrokerSettings = {
  maxClients: number;
  pictureAllowance: number;
  /** Company credits per month and alarm thresholds (doc 11 §6.3). */
  monthlyCredits: number;
  creditAlarmThresholds: number[];
};

export const DEFAULT_SETTINGS: BrokerSettings = {
  maxClients: 5,
  pictureAllowance: 90,
  monthlyCredits: 200,
  creditAlarmThresholds: [0.7, 0.9],
};

/** `audit/{autoId}`: append-only, one record per state change or admin action. */
export type AuditDoc = {
  clientId: string;
  action: string;
  actor: string;
  reason: string;
  before: Partial<ClientDoc> | null;
  after: Partial<ClientDoc> | null;
  at: Timestamp;
};

export const refs = (db: Firestore) => ({
  client: (id: string) => db.collection(Collections.clients).doc(id),
  device: (deviceId: string) => db.collection(Collections.devices).doc(deviceId),
  settings: () => db.collection(Collections.settings).doc("broker"),
  audit: () => db.collection(Collections.audit).doc(),
  clients: () => db.collection(Collections.clients),
});

export async function readSettings(db: Firestore): Promise<BrokerSettings> {
  const snap = await refs(db).settings().get();
  return { ...DEFAULT_SETTINGS, ...(snap.data() as Partial<BrokerSettings> | undefined) };
}
