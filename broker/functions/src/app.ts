import express, { type Express } from "express";
import type { AppCheck } from "firebase-admin/app-check";
import type { Auth } from "firebase-admin/auth";
import type { Firestore } from "firebase-admin/firestore";
import type { BrokerConfig } from "./config.ts";
import type { TokenCipher } from "./crypto/token-cipher.ts";
import { errorHandler, notFound } from "./http/errors.ts";
import type { LogMealCompany } from "./logmeal/client.ts";
import { logmealRoutes } from "./routes/logmeal.ts";

/** Everything a request handler needs; tests build it against the emulators and the fake LogMeal. */
export type Deps = {
  config: BrokerConfig;
  db: Firestore;
  auth: Auth;
  appCheck: AppCheck;
  logmeal: LogMealCompany;
  cipher: TokenCipher;
  now: () => Date;
};

/** The broker HTTP API under `/v1` (doc 11 §8), served by the `api` function. */
export function createApp(deps: Deps): Express {
  const app = express();
  app.disable("x-powered-by");
  app.set("trust proxy", true);
  app.use(express.json({ limit: "16kb" }));

  const v1 = express.Router();
  v1.use("/logmeal", logmealRoutes(deps));
  app.use("/v1", v1);

  app.use((_req, _res, next) => next(notFound()));
  app.use(errorHandler);
  return app;
}
