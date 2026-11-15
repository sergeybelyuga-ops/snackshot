# F-20 | Google Health Connect Sync

## Overview
When Health Connect integration is enabled, the app continuously synchronizes user nutrition data with Google Health Connect. This includes reading the user's calorie and macro goals and writing meal entries so they remain available to other compatible health applications.

## User story
As a user, I want my nutrition history and goals to stay in sync with Health Connect so I can keep my data consistent across apps and preserve a reliable local health record.

## Main behavior
- The app reads and regularly updates the user's calorie and macro goals from Health Connect.
- After each new log entry, the app writes the corresponding calories and macros to Health Connect.
- If the user changes a logged value in the application, the system updates the related Health Connect record.
- If the user deletes a log entry in the application, the system removes the corresponding Health Connect record as well.
- Synchronization happens after each relevant change so the app and Health Connect remain aligned.
- The user can trigger a manual synchronization at any time from the app to refresh goals and push updated logs to Health Connect.

## Acceptance criteria
- The app refreshes calorie and macro goals from Health Connect on a regular basis.
- Each saved meal entry is written to Health Connect with the correct calorie and macro totals.
- Editing a meal in the app updates the corresponding record in Health Connect.
- Deleting a meal in the app removes the corresponding record from Health Connect.
- The user can manually start a sync action to update data immediately.
- The sync works without requiring a cloud backend and remains local-first.

## MVP status
Necessary for MVP: Yes
