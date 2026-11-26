import { badRequest } from "./errors.ts";

/** SHA-256 hex of ANDROID_ID + app salt (doc 11 §5.3); the raw ANDROID_ID is never accepted. */
export const DEVICE_ID = /^[0-9a-f]{64}$/;
/** LogMeal language codes are three lowercase letters (eng, spa, …); the app maps the locale (WBS 4.2). */
export const LANGUAGE = /^[a-z]{3}$/;
/** ISO 3166-1 alpha-2. */
export const COUNTRY = /^[A-Z]{2}$/;

export function objectBody(body: unknown): Record<string, unknown> {
  if (typeof body !== "object" || body === null || Array.isArray(body)) throw badRequest("A JSON object body is required");
  return body as Record<string, unknown>;
}

export function matching(body: Record<string, unknown>, field: string, pattern: RegExp, description: string): string {
  const value = body[field];
  if (typeof value !== "string" || !pattern.test(value)) throw badRequest(`${field} must be ${description}`);
  return value;
}
