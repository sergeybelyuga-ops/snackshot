// Seeds the local emulators (project demo-snackshot) for manual testing
// (docs/Architecture/12_dev-and-test-environment.md §10.1 step 6):
// settings, an admin user, and sample clients in trial, trial_ended and waitlisted.
//
//   cd broker; npm run emulators      # in one terminal
//   cd broker; npm run seed           # in another
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { createHash } from "node:crypto";
import { EmulatorTokenCipher } from "../src/crypto/token-cipher.ts";
import { type ClientDoc, DEFAULT_SETTINGS, refs } from "../src/store/model.ts";

process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";
process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9099";
const projectId = "demo-snackshot"; // the demo- prefix: no real project can be touched

initializeApp({ projectId });
const db = getFirestore();
const auth = getAuth();
const r = refs(db);
const cipher = new EmulatorTokenCipher();
const now = Timestamp.now();
const deviceId = (name: string) => createHash("sha256").update(`seed-${name}`).digest("hex");

await r.settings().set(DEFAULT_SETTINGS);

const admin = await auth.getUserByEmail("admin@example.com").catch(() =>
  auth.createUser({ email: "admin@example.com", password: "admin-password", displayName: "Seed admin" }),
);
await auth.setCustomUserClaims(admin.uid, { admin: true });

type Sample = Pick<ClientDoc, "status" | "picturesUsed" | "statusReason"> & { id: string; token: boolean; logmealUserId: number | null };
const samples: Sample[] = [
  { id: "seed-trial", status: "trial", picturesUsed: 12, statusReason: "provisioned", token: true, logmealUserId: 800001 },
  { id: "seed-trial-ended", status: "trial_ended", picturesUsed: 90, statusReason: "trial_allowance_used", token: true, logmealUserId: 800002 },
  { id: "seed-waitlisted", status: "waitlisted", picturesUsed: 0, statusReason: "pilot_full", token: false, logmealUserId: null },
];

for (const s of samples) {
  const doc: ClientDoc = {
    status: s.status,
    deviceId: deviceId(s.id),
    authUids: [],
    picturesUsed: s.picturesUsed,
    pictureAllowance: DEFAULT_SETTINGS.pictureAllowance,
    logmealUserId: s.logmealUserId,
    logmealUsername: s.logmealUserId ? `snk_seed_${s.logmealUserId}` : null,
    tokenCiphertext: s.token ? await cipher.encrypt("fake-apiuser-token", s.id) : null,
    tokenVersion: s.token ? 1 : 0,
    plan: "trial",
    language: "eng",
    country: "RS",
    isTest: false,
    createdAt: now,
    statusChangedAt: now,
    statusReason: s.statusReason,
    waitlistedAt: s.status === "waitlisted" ? now : null,
  };
  await r.client(s.id).set(doc);
  await r.device(doc.deviceId).set({ clientId: s.id, picturesUsed: s.picturesUsed });
}

console.log(`Seeded ${projectId}: settings, admin@example.com (admin claim), ${samples.length} clients.`);
