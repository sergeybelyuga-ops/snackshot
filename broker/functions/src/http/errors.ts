import type { NextFunction, Request, Response } from "express";
import { logger } from "firebase-functions";

/** An error the client may see: a stable `error` code plus a short message, never internals. */
export class HttpError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export const badRequest = (message: string) => new HttpError(400, "invalid_request", message);
export const unauthenticated = (message = "A valid Firebase ID token is required") => new HttpError(401, "unauthenticated", message);
export const forbidden = (code: string, message: string) => new HttpError(403, code, message);
export const notFound = (code = "not_found", message = "Not found") => new HttpError(404, code, message);
export const unavailable = (code: string, message: string) => new HttpError(503, code, message);

export function errorHandler(error: unknown, req: Request, res: Response, _next: NextFunction) {
  if (error instanceof HttpError) {
    res.status(error.status).json({ error: error.code, message: error.message });
    return;
  }
  // express.json() errors: malformed or oversized bodies.
  const status = (error as { status?: number }).status;
  if (status === 400 || status === 413) {
    res.status(status).json({ error: "invalid_request", message: status === 413 ? "Request body too large" : "Malformed JSON" });
    return;
  }
  // Path and method only: bodies may hold device ids and responses may hold tokens.
  logger.error("Unhandled broker error", { method: req.method, path: req.path, error: String(error) });
  res.status(500).json({ error: "internal", message: "Internal error" });
}
