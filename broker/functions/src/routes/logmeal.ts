import { Router } from "express";
import type { Deps } from "../app.ts";
import { updateProfile } from "../clients/profile.ts";
import { provision, status } from "../clients/provisioning.ts";
import { callerOf, requireAppCaller } from "../http/auth.ts";
import { badRequest } from "../http/errors.ts";
import { COUNTRY, DEVICE_ID, LANGUAGE, matching, objectBody } from "../http/validate.ts";

/** `/v1/logmeal/...` (doc 11 §8). */
export function logmealRoutes(deps: Deps): Router {
  const router = Router();
  router.use(requireAppCaller(deps));

  router.post("/provision", async (req, res) => {
    const body = objectBody(req.body);
    const view = await provision(deps, {
      uid: callerOf(req).uid,
      deviceId: matching(body, "deviceId", DEVICE_ID, "a lowercase SHA-256 hex digest"),
      language: matching(body, "language", LANGUAGE, "a three-letter LogMeal language code"),
      country: matching(body, "country", COUNTRY, "an ISO 3166-1 alpha-2 country code"),
    });
    res.setHeader("Cache-Control", "no-store");
    res.json(view);
  });

  router.get("/status", async (req, res) => {
    const known = req.query.knownTokenVersion;
    let knownTokenVersion: number | null = null;
    if (known !== undefined) {
      knownTokenVersion = Number(known);
      if (!Number.isInteger(knownTokenVersion) || knownTokenVersion < 0) throw badRequest("knownTokenVersion must be a non-negative integer");
    }
    res.setHeader("Cache-Control", "no-store");
    res.json(await status(deps, callerOf(req).uid, knownTokenVersion));
  });

  router.post("/profile", async (req, res) => {
    const body = objectBody(req.body);
    res.json(
      await updateProfile(
        deps,
        callerOf(req).uid,
        matching(body, "language", LANGUAGE, "a three-letter LogMeal language code"),
        matching(body, "country", COUNTRY, "an ISO 3166-1 alpha-2 country code"),
      ),
    );
  });

  return router;
}
