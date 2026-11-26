import { logger } from "firebase-functions";
import type { BrokerConfig } from "../config.ts";

/** An APIUser as the broker needs it. The token is returned only by `signUp`. */
export type ApiUser = { id: number; username: string; enabled: boolean };

/** Company-account credit counters from the `x-account-credits-*` headers (doc 11 §6.3). */
export type Credits = { used: number; remaining: number };

/**
 * The LogMeal operations the broker performs with the APICompany token (doc 11 §1). Recognition
 * calls are made by the app with its own APIUser token and never pass through the broker.
 */
export interface LogMealCompany {
  signUp(username: string, language: string): Promise<{ id: number; token: string }>;
  setProfile(userId: number, language: string, country: string): Promise<void>;
  listUsers(): Promise<ApiUser[]>;
  /**
   * Makes `enabled` equal to [enabled]. `toggleEnabled` is a toggle, not a setter, so this
   * reads the current value first and toggles only when it differs (doc 11 §5.1).
   */
  setEnabled(userId: number, enabled: boolean): Promise<void>;
  deleteUser(userId: number): Promise<void>;
  credits(): Promise<Credits | null>;
}

export class LogMealError extends Error {
  readonly status: number;
  readonly code: number | null;

  constructor(path: string, status: number, code: number | null, detail: string) {
    // Never include tokens: only the path, the status and LogMeal's message.
    super(`LogMeal ${path} → HTTP ${status}${code === null ? "" : ` (code ${code})`}: ${detail.slice(0, 200)}`);
    this.name = "LogMealError";
    this.status = status;
    this.code = code;
  }
}

/** The real HTTP API; in `fake` mode the same client talks to tools/fake-logmeal. */
export class HttpLogMealCompany implements LogMealCompany {
  private readonly baseUrl: string;
  private readonly token: () => string;

  constructor(baseUrl: string, token: () => string) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  async signUp(username: string, language: string) {
    const body = await this.call<{ id?: number; token?: string }>("POST", "/v2/users/signUp", { username, language });
    if (typeof body.id !== "number" || typeof body.token !== "string" || !body.token) {
      throw new LogMealError("/v2/users/signUp", 200, null, "response without id or token");
    }
    return { id: body.id, token: body.token };
  }

  async setProfile(userId: number, language: string, country: string) {
    await this.call("POST", `/v2/users/modifyUserProfileInfo/${userId}`, { language, country });
  }

  async listUsers() {
    const users = await this.call<{ id: number; username: string; enabled: boolean }[]>("GET", "/v2/users/APIUsers");
    return users.map(({ id, username, enabled }) => ({ id, username, enabled }));
  }

  async setEnabled(userId: number, enabled: boolean) {
    const current = (await this.listUsers()).find((u) => u.id === userId);
    if (!current) throw new LogMealError(`/v2/users/APIUsers`, 404, null, `APIUser ${userId} not found`);
    if (current.enabled === enabled) return;
    const result = await this.call<{ enabled?: boolean }>("POST", `/v2/users/toggleEnabled/${userId}`);
    if (result.enabled !== enabled) throw new LogMealError(`/v2/users/toggleEnabled/${userId}`, 200, null, "toggle did not reach the wanted state");
  }

  async deleteUser(userId: number) {
    try {
      await this.call("DELETE", `/v2/users/deleteAPIUser/${userId}`);
    } catch (error) {
      if (error instanceof LogMealError && error.status === 404) return; // already gone
      throw error;
    }
  }

  async credits() {
    const res = await this.fetch("GET", "/v2/info/limitations");
    const used = Number(res.headers.get("x-account-credits-used"));
    const remaining = Number(res.headers.get("x-account-credits-remaining"));
    if (!res.ok) throw await errorFrom("/v2/info/limitations", res);
    return Number.isFinite(used) && Number.isFinite(remaining) && res.headers.has("x-account-credits-used") ? { used, remaining } : null;
  }

  private async call<T = unknown>(method: string, path: string, body?: unknown): Promise<T> {
    const res = await this.fetch(method, path, body);
    if (!res.ok) throw await errorFrom(path, res);
    const text = await res.text();
    return (text ? JSON.parse(text) : {}) as T;
  }

  private fetch(method: string, path: string, body?: unknown) {
    return fetch(`${this.baseUrl}${path}`, {
      method,
      headers: { Authorization: `Bearer ${this.token()}`, ...(body === undefined ? {} : { "Content-Type": "application/json" }) },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(15_000),
    });
  }
}

async function errorFrom(path: string, res: Response): Promise<LogMealError> {
  const text = await res.text().catch(() => "");
  let code: number | null = null;
  let message = text;
  try {
    const json = JSON.parse(text) as { code?: unknown; message?: unknown };
    if (typeof json.code === "number") code = json.code;
    if (typeof json.message === "string") message = json.message;
  } catch {
    // LogMeal's 401 has an empty body (test-fixtures/logmeal/errors/401-empty-body.txt)
  }
  return new LogMealError(path, res.status, code, message || res.statusText);
}

/**
 * `test-user` mode (dev): every client gets the single test APIUser. Management calls are
 * logged and skipped, so dev never creates, toggles or deletes APIUsers (doc 12 §3.3).
 */
export class TestUserLogMeal implements LogMealCompany {
  private readonly userId: number;
  private readonly token: () => string;
  private readonly http: HttpLogMealCompany;

  constructor(userId: number, token: () => string, baseUrl: string) {
    this.userId = userId;
    this.token = token;
    // Only for the credit headers, which an APIUser token can read on the free endpoint.
    this.http = new HttpLogMealCompany(baseUrl, token);
  }

  async signUp(_username: string, _language: string) {
    return { id: this.userId, token: this.token() };
  }

  async setProfile(userId: number, language: string, country: string) {
    skipped("modifyUserProfileInfo", { userId, language, country });
  }

  async listUsers() {
    return [{ id: this.userId, username: "test-user", enabled: true }];
  }

  async setEnabled(userId: number, enabled: boolean) {
    skipped("toggleEnabled", { userId, enabled });
  }

  async deleteUser(userId: number) {
    skipped("deleteAPIUser", { userId });
  }

  credits() {
    return this.http.credits();
  }
}

function skipped(operation: string, details: Record<string, unknown>) {
  logger.info(`test-user mode: skipped LogMeal ${operation}`, details);
}

export type LogMealSecrets = { companyToken: () => string; testUserToken: () => string };

export function createLogMeal(config: BrokerConfig, secrets: LogMealSecrets): LogMealCompany {
  switch (config.mode) {
    case "fake":
      return new HttpLogMealCompany(config.logmealBaseUrl, () => "fake-company-token");
    case "test-user":
      return new TestUserLogMeal(config.testUserId!, secrets.testUserToken, config.logmealBaseUrl);
    case "company":
      return new HttpLogMealCompany(config.logmealBaseUrl, secrets.companyToken);
  }
}
