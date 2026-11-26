// Fake LogMeal API on node:http, serving the recorded fixtures in test-fixtures/logmeal
// (docs/Architecture/12_dev-and-test-environment.md §3.1, §3.2). Zero credits, no secrets.
//
// One implementation for every consumer: the Android local flavor (via adb reverse), the
// broker emulator (LOGMEAL_MODE=fake), and the broker tests (started in-process).
//
//   node tools/fake-logmeal/server.ts [--port 8090] [--scenario apple|banana|bread|tangerine|tomato|
//                                      401|403-116|429|500] [--delay 2500]
//
// Per request, the header `X-Fake-Scenario` overrides the startup scenario.
// Plain TypeScript that Node runs with type stripping: annotations only, no enums.
import { readFileSync } from "node:fs";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const FAKE_COMPANY_TOKEN = "fake-company-token";
/** Accepted as an APIUser token in addition to the tokens issued by signUp. */
export const FAKE_USER_TOKEN = "fake-apiuser-token";

const FIXTURES = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "test-fixtures", "logmeal");

type Dish = { imageId: number; segmentation: string; nutrition: string };
const APPLE: Dish = { imageId: 1000001, segmentation: "ok-single-dish-apple", nutrition: "ok-apple" };
const DISHES: Record<string, Dish> = {
  apple: APPLE,
  tomato: { imageId: 1000002, segmentation: "one-tomato-three-items", nutrition: "one-tomato-three-items" },
  tangerine: { imageId: 1000003, segmentation: "ok-single-dish-tangerine", nutrition: "ok-tangerine" },
  banana: { imageId: 1000004, segmentation: "ok-single-dish-banana", nutrition: "ok-banana" },
  bread: { imageId: 1000005, segmentation: "ok-single-dish-bread", nutrition: "ok-bread" },
};
const ERROR_SCENARIOS = ["401", "403-116", "429", "500"];

export type FakeUser = { id: number; username: string; token: string; enabled: boolean; language: string; country: string | null };

export type FakeLogMeal = {
  url: string;
  /** APIUsers created through the company endpoints, by id. */
  users: Map<number, FakeUser>;
  /** Every request, for assertions: method and path only. */
  calls: { method: string; path: string }[];
  setScenario(scenario: string | null): void;
  /** Credits shown in `x-account-credits-*` headers. */
  setCredits(used: number, remaining: number): void;
  close(): Promise<void>;
};

export type FakeOptions = { port?: number; host?: string; scenario?: string | null; delayMs?: number };

export async function startFakeLogMeal(options: FakeOptions = {}): Promise<FakeLogMeal> {
  const users = new Map<number, FakeUser>();
  const calls: { method: string; path: string }[] = [];
  const confirmed = new Set<number>();
  let scenario = options.scenario ?? null;
  let credits = { used: 12, remaining: 188 };
  let nextId = 900001;

  const fixture = (path: string): unknown => JSON.parse(readFileSync(join(FIXTURES, path), "utf8"));

  const server: Server = createServer((req, res) => {
    const handle = async () => {
      if (options.delayMs) await new Promise((r) => setTimeout(r, options.delayMs));
      const url = new URL(req.url ?? "/", "http://fake");
      calls.push({ method: req.method ?? "GET", path: url.pathname });
      const body = await readJson(req);
      const token = (req.headers.authorization ?? "").replace(/^Bearer /, "");
      const active = (req.headers["x-fake-scenario"] as string | undefined) ?? scenario;
      route(req.method ?? "GET", url.pathname, body, token, active, res);
    };
    handle().catch((error) => send(res, 500, { code: 500, message: String(error) }));
  });

  function rateHeaders(res: ServerResponse) {
    res.setHeader("ratelimit-limit", "20");
    res.setHeader("ratelimit-remaining", "19");
    res.setHeader("ratelimit-reset", "86400");
    res.setHeader("x-account-credits-used", String(credits.used));
    res.setHeader("x-account-credits-remaining", String(credits.remaining));
  }

  function userFor(token: string): FakeUser | "fixed" | null {
    if (token === FAKE_USER_TOKEN) return "fixed";
    for (const user of users.values()) if (user.token === token) return user;
    return null;
  }

  function route(method: string, path: string, body: any, token: string, active: string | null, res: ServerResponse) {
    // Company (APIUserManager) endpoints: the fake company token only.
    if (path.startsWith("/v2/users/")) {
      if (token !== FAKE_COMPANY_TOKEN) return send(res, 401, "");
      return company(method, path, body, res);
    }
    if (path === "/v2/info/limitations" && method === "GET") {
      if (token !== FAKE_COMPANY_TOKEN && !userFor(token)) return send(res, 401, "");
      rateHeaders(res);
      return send(res, 200, fixture("info-limitations.json"));
    }

    // APIUser endpoints.
    const user = userFor(token);
    if (!user || active === "401") return send(res, 401, readFileSync(join(FIXTURES, "errors", "401-empty-body.txt"), "utf8"));
    if ((user !== "fixed" && !user.enabled) || active === "403-116") {
      return send(res, 403, { code: 116, message: "The APIUser is disabled." });
    }
    if (active === "429") {
      rateHeaders(res);
      res.setHeader("ratelimit-remaining", "0");
      return send(res, 429, { code: 429, message: "Too many requests." });
    }
    if (active === "500") return send(res, 500, { code: 500, message: "Internal server error." });

    const dish: Dish = (active && !ERROR_SCENARIOS.includes(active) ? DISHES[active] : undefined) ?? APPLE;
    if (method === "POST" && path === "/v2/image/segmentation/complete") {
      rateHeaders(res);
      credits = { used: credits.used + 1, remaining: Math.max(0, credits.remaining - 1) };
      return send(res, 200, fixture(`segmentation-complete/${dish.segmentation}.json`));
    }
    if (method === "POST" && path === "/v2/nutrition/recipe/nutritionalInfo") {
      const imageId = Number(body?.imageId);
      const entry = Object.values(DISHES).find((d) => d.imageId === imageId) ?? dish;
      const name = entry.imageId === 1000001 && confirmed.has(imageId) ? "apple-after-confirm-116g" : entry.nutrition;
      return send(res, 200, fixture(`nutritional-info/${name}.json`));
    }
    if (method === "POST" && path === "/v2/nutrition/confirm/quantity") {
      if (typeof body?.quantity !== "object" || body.quantity === null) {
        return send(res, 400, fixture("errors/400-1614-quantity-not-object.json"));
      }
      confirmed.add(Number(body.imageId));
      return send(res, 200, fixture("quantity/confirm-ok.json"));
    }
    return send(res, 404, { code: 404, message: `Fake LogMeal has no ${method} ${path}` });
  }

  function company(method: string, path: string, body: any, res: ServerResponse) {
    const idFrom = (prefix: string) => Number(path.slice(prefix.length));
    if (method === "POST" && path === "/v2/users/signUp") {
      const username = String(body?.username ?? "");
      if (!username) return send(res, 400, { code: 400, message: "username is required" });
      if ([...users.values()].some((u) => u.username === username)) return send(res, 409, { code: 409, message: "Username already exists." });
      const id = nextId++;
      const user: FakeUser = { id, username, token: `fake-token-${id}`, enabled: true, language: String(body?.language ?? "eng"), country: null };
      users.set(id, user);
      return send(res, 200, { id, username, token: user.token });
    }
    if (method === "GET" && path === "/v2/users/APIUsers") {
      return send(res, 200, [...users.values()].map(({ id, username, token, enabled }) => ({ id, username, token, enabled })));
    }
    if (method === "POST" && path.startsWith("/v2/users/toggleEnabled/")) {
      const user = users.get(idFrom("/v2/users/toggleEnabled/"));
      if (!user) return send(res, 404, { code: 404, message: "User not found." });
      user.enabled = !user.enabled;
      return send(res, 200, { enabled: user.enabled });
    }
    if (method === "POST" && path.startsWith("/v2/users/modifyUserProfileInfo/")) {
      const user = users.get(idFrom("/v2/users/modifyUserProfileInfo/"));
      if (!user) return send(res, 404, { code: 404, message: "User not found." });
      if (body?.language) user.language = String(body.language);
      if (body?.country) user.country = String(body.country);
      return send(res, 200, { message: "Profile updated." });
    }
    if (method === "DELETE" && path.startsWith("/v2/users/deleteAPIUser/")) {
      const id = idFrom("/v2/users/deleteAPIUser/");
      if (!users.delete(id)) return send(res, 404, { code: 404, message: "User not found." });
      return send(res, 200, { message: "User deleted." });
    }
    return send(res, 404, { code: 404, message: `Fake LogMeal has no ${method} ${path}` });
  }

  await new Promise<void>((resolve) => server.listen(options.port ?? 0, options.host ?? "127.0.0.1", resolve));
  const address = server.address();
  const port = typeof address === "object" && address ? address.port : options.port;

  return {
    url: `http://${options.host ?? "127.0.0.1"}:${port}`,
    users,
    calls,
    setScenario: (s) => {
      scenario = s;
    },
    setCredits: (used, remaining) => {
      credits = { used, remaining };
    },
    close: () => new Promise((resolve, reject) => server.close((e) => (e ? reject(e) : resolve()))),
  };
}

async function readJson(req: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];
  for await (const chunk of req) chunks.push(chunk as Buffer);
  if (chunks.length === 0) return null;
  const text = Buffer.concat(chunks).toString("utf8");
  try {
    return JSON.parse(text);
  } catch {
    return null; // multipart image uploads: the fake ignores the picture
  }
}

function send(res: ServerResponse, status: number, body: unknown) {
  res.statusCode = status;
  if (typeof body === "string") {
    res.end(body);
    return;
  }
  res.setHeader("content-type", "application/json");
  res.end(JSON.stringify(body));
}

// CLI
if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  const arg = (name: string) => {
    const i = process.argv.indexOf(`--${name}`);
    return i > 0 ? process.argv[i + 1] : undefined;
  };
  const fake = await startFakeLogMeal({
    port: Number(arg("port") ?? 8090),
    host: arg("host") ?? "127.0.0.1",
    scenario: arg("scenario") ?? null,
    delayMs: Number(arg("delay") ?? 0),
  });
  console.log(`Fake LogMeal on ${fake.url} (scenario: ${arg("scenario") ?? "apple"})`);
}
