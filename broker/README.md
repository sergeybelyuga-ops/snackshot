# LogMeal Access Broker

Firebase project config, Firestore rules, Cloud Functions (TypeScript) in `functions/`, emulator seed data in `seed/`, owner-only scripts in `scripts/`.
See [docs/Architecture/11_logmeal-access-token-management.md](../docs/Architecture/11_logmeal-access-token-management.md) and [12](../docs/Architecture/12_dev-and-test-environment.md) §3.3, §7.2.

| Item | State |
| --- | --- |
| Firebase config, emulators, deny-all rules | WBS 1.4 |
| Cloud Functions, fake LogMeal, seed data, `broker-ci`, `deploy-broker` | WBS 3.1 |

## Local emulators

```powershell
cd broker
npm ci
npm run emulators        # project demo-snackshot: Auth 9099, Firestore 8080, UI http://localhost:4000
```

The Firestore emulator needs Java 21 on `PATH`. `.firebaserc` defaults to `demo-snackshot`, so a deploy must name `--project dev` or `--project prod` explicitly.
