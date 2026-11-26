# LogMeal Access Broker

The broker holds the LogMeal APICompany token and gives each Snackshot client only its own APIUser token, with the 90-picture trial, the 5-client pilot cap and the client lifecycle. Design: [docs/Architecture/11](../docs/Architecture/11_logmeal-access-token-management.md); environments and tests: [12](../docs/Architecture/12_dev-and-test-environment.md) §3.3, §5, §7.

| Path | What |
| --- | --- |
| `firebase.json`, `.firebaserc` | Functions (codebase `broker`, Node 22), Firestore rules and indexes, emulators. The default project is `demo-snackshot`, so a deploy must name `--project dev` or `--project prod`. |
| `firestore.rules` | Deny-all: only the functions (Admin SDK) read and write. |
| `functions/` | Cloud Functions in TypeScript. One HTTPS function, `api`, serves `/v1/...` in `europe-west3` and runs as `broker@<project>`. |
| `functions/.env.<projectId>` | `LOGMEAL_MODE` per project: `fake` (emulator), `test-user` (dev), `company` (prod). No secrets in these files. |
| `functions/scripts/seed.ts` | Emulator seed data. |
| `scripts/` | `dev.ts` (local loop); owner-only `create-test-user.ts` and `put-secret.ts`, never run in CI. |
| `../tools/fake-logmeal/` | Fake LogMeal API on the recorded fixtures, used by the emulator, the tests and the Android local flavor. |

## LogMeal modes and secrets

| Mode | Project | APIUsers | Secret (Secret Manager) |
| --- | --- | --- | --- |
| `fake` | `demo-snackshot` (emulator) | Created on the fake server | none |
| `test-user` | `snackshot-dev-app` | Every client gets the test APIUser; `signUp`, `toggleEnabled`, `deleteAPIUser`, `modifyUserProfileInfo` are logged and skipped | `LOGMEAL_TEST_USER_TOKEN` |
| `company` | `snackshot-prod` | One per client | `LOGMEAL_COMPANY_TOKEN` |

`config.ts` refuses unsafe combinations: `fake` outside the emulator, `company` outside prod, plain HTTP, or App Check switched off in a cloud project.

APIUser tokens are stored in Firestore only as Cloud KMS ciphertext (`broker/apiuser-token` in europe-west3; only `broker@` can use the key). The ciphertext is bound to its client ID, so it can't be moved to another record. The emulator uses a local AES-GCM stand-in.

## Cost controls (WBS 3.6, doc 11 §6.3)

- **Kill switch:** the Remote Config parameter `ai_recognition_enabled`, published in both projects with the default `true`. Set it to `false` in the Firebase console (Remote Config, then Publish) to stop AI recognition for everybody without touching any LogMeal user.
  - The app reads the switch itself (WBS 4.x).
  - The broker reads it too, cached for a minute, and refuses to create clients or APIUsers while it's off (503 `ai_disabled`).
  - `provision` and `status` return `aiEnabled`.
  - If the template can't be read, the broker treats AI as on.
  - On the emulator the switch is `settings/broker.aiRecognitionEnabled`.
- **Credit check:** `checkLogMealCredits` runs every 6 hours. It reads the company credits from LogMeal's free `info/limitations` headers and logs one `logmeal_credit_alarm` warning per threshold per month (70 % and 90 % from `settings/broker.creditAlarmThresholds`, plus 100 %). The state is in `settings/credit_alarm`.
- **E-mail:** a Cloud Monitoring log-match alert policy turns that log entry into an e-mail. Set it up once with `node broker/scripts/setup-credit-alert.ts --email <owner address>`. The script is idempotent and covers both projects.
- **Reconcile:** `reconcileClients` runs hourly and finishes LogMeal changes that failed at the time (3.5).

## Local loop

```powershell
cd broker
npm ci; npm --prefix functions ci
npm run dev          # fake LogMeal :8090 + emulators (Auth 9099, Functions 5001, Firestore 8080, UI :4000)
npm run seed         # in a second terminal: settings, admin@example.com (admin claim), sample clients
```

The broker is then at `http://127.0.0.1:5001/demo-snackshot/europe-west3/api` (the Android `local` flavor URL, via `tools/adb-reverse.ps1`). Start the fake server in a failure mode with `npm run dev -- --scenario 429`. Other scenarios are `401`, `403-116` and `500`, plus the dishes `banana`, `bread`, `tangerine` and `tomato`. A single request can override the scenario with the `X-Fake-Scenario` header.

## Tests

```powershell
cd broker
npm test             # Auth + Firestore emulators, then node --test in functions/ (endpoints, state machine, rules)
npm run test:e2e     # the built api function in the Functions emulator, as deployed
npm --prefix functions run lint; npm --prefix functions run typecheck
```

| Suite | Covers |
| --- | --- |
| `setup.test.ts` | Config guard rails, the LogMeal client against the fake (read-before-toggle, errors without tokens), test-user mode, token ciphers (KMS and emulator) |
| `provision.test.ts` | Auth and App Check, validation, Trial, reinstall re-attach, pilot cap and ordered waitlist (also under concurrency), double tap, device counter after Delete all data, status and token versions |
| `profile.test.ts` | Language and country to LogMeal and the registry, waitlisted clients, LogMeal failures |
| `pictures.test.ts` | Picture idempotency (also concurrent), trial end at 90 and disable, in-flight pictures, Delete all data |
| `state-machine.test.ts` | Allowed transitions, idempotency, audit, cancel, waitlist approval, LogMeal down and the hourly reconcile, per-client leases |
| `cost.test.ts` | Kill switch (emulator and Remote Config), credit alarms per threshold and month |
| `rules.test.ts` | Deny-all for every collection: signed out, anonymous app user, admin claim |
| `e2e/api.e2e.ts` | The deployed function shape end to end |

The `metrics` endpoint and its tests come with WBS 10.3.

- **The tests use Node's built-in runner** (`node:test`), and Node runs the TypeScript directly. There is no bundler and no native binary, because Smart App Control on the dev PC blocks unsigned native modules such as Vitest's rolldown.
- **LogMeal is the fake server, started in-process for each suite.** It's the same code the emulator uses, so no LogMeal call or secret is needed.
- **The Firestore emulator needs Java 21 on `PATH`.**

## CI/CD

- **`broker-ci`:** on every push or PR that touches `broker/**`, `tools/fake-logmeal/**` or `test-fixtures/**`. It runs lint, typecheck, build and the emulator tests.
- **`deploy-broker` to dev:** automatic on every push to `main` that touches `broker/**`, after the tests pass. It authenticates through Workload Identity Federation as `github-deploy@`, then deploys the functions and the Firestore rules and indexes. A smoke check expects the JSON 404 from `/api/v1/smoke-check`.
- **`deploy-broker` to prod:** manual, with `env: prod`, and waits for the owner's approval.
