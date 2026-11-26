import { logger } from "firebase-functions";
import type { Deps } from "../app.ts";
import { HttpError, unavailable } from "../http/errors.ts";
import { LogMealError } from "../logmeal/client.ts";
import { writeAudit } from "../store/audit.ts";
import { withClientLease } from "../store/lease.ts";
import { refs } from "../store/model.ts";
import { findClientByUid, getClient } from "./provisioning.ts";

/** Statuses whose APIUser still exists in LogMeal, so its profile is kept in step. */
const HAS_APIUSER = ["trial", "trial_ended", "active", "paused", "suspended"];

/**
 * `POST /v1/logmeal/profile` (doc 11 §0 decision 12, §8): the user changed the language or
 * country used for recognition in Settings. LogMeal is updated first, then the registry, so a
 * LogMeal failure leaves both unchanged and the app can retry.
 */
export async function updateProfile(deps: Deps, uid: string, language: string, country: string) {
  const snap = await findClientByUid(deps.db, uid);
  if (!snap) throw new HttpError(404, "not_provisioned", "No AI access yet; the language and country are sent at provisioning");
  const clientId = snap.id;

  await withClientLease(deps.db, clientId, deps.now, async () => {
    const client = await getClient(deps.db, clientId);
    if (client.language === language && client.country === country) return;
    if (HAS_APIUSER.includes(client.status) && client.logmealUserId !== null) {
      try {
        await deps.logmeal.setProfile(client.logmealUserId, language, country);
      } catch (error) {
        if (!(error instanceof LogMealError)) throw error;
        logger.warn("Profile update failed at LogMeal", { clientId, status: error.status, code: error.code });
        throw unavailable("logmeal_unavailable", "The recognition language could not be changed; try again later");
      }
    }
    // Waitlisted and provisioning clients: stored now, sent to LogMeal when the APIUser is created.
    await refs(deps.db).client(clientId).update({ language, country });
    await writeAudit(deps.db, { clientId, action: "profile_updated", actor: "app", reason: "settings", before: client, after: { ...client, language, country } }, deps.now());
  });
  return { language, country };
}
