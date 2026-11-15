# F-33 | Broker Admin Console

## Overview
An internal web console, used only by the Snackshot owner, for managing each user's LogMeal access through the LogMeal Access Broker. It runs on Firebase Hosting and performs every action through admin Cloud Functions, which write an audit record for each change. Users are anonymous, so the console shows client IDs and hashed device IDs, never names or e-mails.

Design details: [11_logmeal-access-token-management.md](../Architecture/11_logmeal-access-token-management.md) §8.1.

## User story
As the app owner, I want to see each user's AI usage and status and be able to pause, resume, cancel, or reset their LogMeal access, so I can control costs and handle support requests without touching the LogMeal dashboard.

## Main behavior
- **Sign-in:** Google sign-in for admins only (Firebase custom claim `admin: true`).
- **Dashboard:** LogMeal credits used/remaining, clients vs pilot cap (x of 5), clients by status, pictures used across trials, credit alarms, and a **global AI kill switch**.
- **Clients:** list with client ID, LogMeal user ID, status and reason, pictures used / 90, device (hashed), last seen, created.
- **Client detail:** status history, usage by endpoint, masked token (last 4 characters + version) and actions: **Pause**, **Resume**, **Suspend**, **Rotate token**, **Grant extra pictures / reset trial**, **Delete** (frees a pilot slot). Every action asks for a reason and a confirmation.
- **Waitlist:** devices waiting for a pilot slot; **Approve** / **Reject**.
- **Audit log:** who, what, when, reason, before/after; CSV export.
- **Settings:** trial picture allowance (90), pilot cap (5), credit alarm thresholds, device-switch limit, alert e-mail.
- **Sync with LogMeal:** compares the broker registry with LogMeal's user list and flags differences (for example, a user changed manually in LogMeal's dashboard).

## Acceptance criteria
- Only accounts with the admin claim can open the console or call admin functions.
- Full LogMeal tokens and the LogMeal company token are never displayed.
- Every action is reflected in LogMeal (enable/disable/delete user) and in the audit log.
- The kill switch disables AI in all app installs within the Remote Config refresh interval, without changing LogMeal users.

## MVP status
Necessary for MVP: Yes, in a minimal form (Clients list + Client detail actions + Waitlist approve). Dashboard, Audit log UI, Settings UI, and Sync can follow after the MVP.

## Related feature IDs
- F-30 Account-free App Access
- F-32 Free AI Trial & Paywall
