# F-30 | Account-free App Access (anonymous identity)

> **Changed 14 November 2026.** This feature previously required Google sign-in before Health Connect. That conflicted with the architecture decision that Snackshot never requires a Google account ([02_answers-architecture-checklist.md](../Architecture/02_answers-architecture-checklist.md) §2.1–2.3). It now describes account-free access. The file name is kept so existing links keep working.

## Overview
The user can start using the app, including AI food recognition, immediately after installation, with no registration, sign-in, or personal data. The app creates a silent anonymous identity (Firebase Anonymous Authentication) and links it to a hashed device ID so that the LogMeal Access Broker can give the user their own LogMeal access, count their free pictures, and pause or cancel their AI access when needed. Health Connect / Google Health still needs no sign-in inside Snackshot; any Google account login happens inside the Google Health app.

Design details: [11_logmeal-access-token-management.md](../Architecture/11_logmeal-access-token-management.md) §5.3.

## User story
As a user, I want to open the app and log my first meal with AI right away, without creating an account, so I can see the value before committing anything.

## Main behavior
- No registration, login, or sign-out screens exist in the MVP.
- On the **first AI action** (Take a photo or a gallery image), the app silently signs in anonymously and asks the broker for AI access, sending only a hashed device ID and the app language.
- The broker answers with one of: trial access (with the remaining free pictures), waitlisted (pilot full), or trial ended (paywall).
- The per-user LogMeal token is stored encrypted on the device (Android Keystore) and is never shown to the user.
- After uninstalling and reinstalling on the **same phone**, the user is recognized again by the device ID: the same LogMeal access and the same free-picture counter are restored. Reinstalling does not reset the free trial.
- On a **new phone or after a factory reset**, the user starts as a new anonymous client. Local meal history can be restored only through Health Connect sync.
- "Delete all data" in Settings deletes the local data and the user's broker record and LogMeal user. The device's used-picture counter is kept, so deleting data does not grant a new trial.
- Users who only log manually never contact the broker.

## Acceptance criteria
- A new user can reach the camera and get an AI result without any account or sign-in step.
- No personal data (name, e-mail, phone, Google account) is collected or sent to the broker.
- The LogMeal company credential never exists in the app; only the user's own LogMeal token is stored, encrypted with an Android Keystore key and excluded from Android backup.
- Reinstalling on the same phone restores AI access and keeps the picture counter.
- If the broker is unreachable, AI capture shows a retryable error and manual logging keeps working.

## MVP status
Necessary for MVP: Yes

## Post-MVP options
- Optional "Link Google account" (Firebase `linkWithCredential`) if users want to move a free trial between phones. Paying users move between phones by restoring their Google Play purchase, so no account is needed.

## Related feature IDs
- F-01 AI Food Identification
- F-32 Free AI Trial & Paywall
- F-33 Broker Admin Console
- F-19 Connect to Health Connect (Mandatory)
