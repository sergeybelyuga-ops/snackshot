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
npm test             # Auth + Firestore emulators, then node --test in functions/
npm --prefix functions run lint; npm --prefix functions run typecheck
```

- **The tests use Node's built-in runner** (`node:test`), and Node runs the TypeScript directly. There is no bundler and no native binary, because Smart App Control on the dev PC blocks unsigned native modules such as Vitest's rolldown.
- **LogMeal is the fake server, started in-process for each suite.** It's the same code the emulator uses, so no LogMeal call or secret is needed.
- **The Firestore emulator needs Java 21 on `PATH`.**

## CI/CD

- **`broker-ci`:** on every push or PR that touches `broker/**`, `tools/fake-logmeal/**` or `test-fixtures/**`. It runs lint, typecheck, build and the emulator tests.
- **`deploy-broker` to dev:** automatic on every push to `main` that touches `broker/**`, after the tests pass. It authenticates through Workload Identity Federation as `github-deploy@`, then deploys the functions and the Firestore rules and indexes. A smoke check expects the JSON 404 from `/api/v1/smoke-check`.
- **`deploy-broker` to prod:** manual, with `env: prod`, and waits for the owner's approval.
