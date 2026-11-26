import type { NextFunction, Request, Response } from "express";
import type { Deps } from "../app.ts";
import { forbidden, unauthenticated } from "./errors.ts";

/** The verified caller: an anonymous Firebase user of the Snackshot app. */
export type Caller = { uid: string };

declare module "express-serve-static-core" {
  interface Request {
    caller?: Caller;
  }
}

/**
 * Every app endpoint requires an anonymous Firebase ID token and, outside the emulator, an
 * App Check token (doc 11 §8). No endpoint asks for personal data.
 */
export function requireAppCaller(deps: Deps) {
  return async (req: Request, _res: Response, next: NextFunction) => {
    try {
      if (deps.config.appCheckRequired) {
        const appCheckToken = req.header("X-Firebase-AppCheck");
        if (!appCheckToken) throw unauthenticated("An App Check token is required");
        await deps.appCheck.verifyToken(appCheckToken).catch(() => {
          throw unauthenticated("Invalid App Check token");
        });
      }
      const header = req.header("Authorization") ?? "";
      const idToken = header.startsWith("Bearer ") ? header.slice(7) : "";
      if (!idToken) throw unauthenticated();
      const decoded = await deps.auth.verifyIdToken(idToken).catch(() => {
        throw unauthenticated("Invalid or expired Firebase ID token");
      });
      // The app signs in anonymously only; other providers are the admin console's.
      if (decoded.firebase?.sign_in_provider !== "anonymous") throw forbidden("anonymous_only", "Only the app's anonymous identity may call this endpoint");
      req.caller = { uid: decoded.uid };
      next();
    } catch (error) {
      next(error);
    }
  };
}

export function callerOf(req: Request): Caller {
  if (!req.caller) throw unauthenticated();
  return req.caller;
}
