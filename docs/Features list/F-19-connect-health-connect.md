# F-19 | Connect to Google Health Connect

> **Changed 18 November 2026.** The connection is optional (it was previously labelled "Mandatory"), it is managed in Settings, and goals are not read from Google Health Connect in the MVP. The file name is kept so existing links keep working.

## Overview
Snackshot works fully without Google Health Connect. When the user saves a meal for the first time, the app offers to connect to Google Health Connect so that their meal logs are also kept in Google's health data and can be shared with other compatible applications. The app explains that it does not store user data on its own servers; the data stays on the phone.

**The connection process (the only one):** the user decides to connect → signs in to the **Google Health app** with their Google account (inside that app, never inside Snackshot) → links Snackshot as a connected app and grants the permissions in the system dialog. This process is called **"connecting to Google Health Connect"** in all project documents.

## User story
As a user, I want to decide whether my nutrition history is synchronized with Google Health Connect so I can understand the privacy trade-off and keep my data available across apps while the app stays local-first.

## Main behavior
- On the first meal save, the app offers the Google Health Connect connection prompt (S-10).
- The prompt explains that the application does not store customer data on any server; all data remains on the phone.
- The prompt explains that Google Health Connect keeps a copy of the meal history and can share it with other applications.
- If the user declines, the app stores data only locally on the phone, and the user can connect later in **Settings**.
- If the user confirms, the app shows the system Health Connect authorization dialog. If the Google Health app requires sign-in, the user is directed to that app's own flow.
- After authorization, every new, edited, or deleted meal entry is synchronized automatically (F-20).
- The connection can be checked, disconnected, and reconnected in **Settings**.
- Calorie and macro goals are set inside Snackshot (F-16) and are not read from or written to Google Health Connect in the MVP.

## Acceptance criteria
- The app offers the connection when the user first saves a meal, and never blocks local logging.
- The explanation clearly states that the app does not store customer data on external servers and that data remains on the phone.
- The explanation clearly states that Google Health Connect keeps a copy of the history and can share it with other supported applications.
- If the user declines, the app keeps working in local-only mode, and the connection can be set up later in Settings.
- If the user accepts, the standard Health Connect authorization dialog appears.
- New log entries synchronize automatically after authorization.
- Revoked permissions are detected and handled without data loss.

## MVP status
Necessary for MVP: Yes (optional for the user)
