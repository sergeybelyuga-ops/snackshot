# F-20 | Google Health Connect Sync

## Overview
When the user has connected to Google Health Connect (F-19), the app synchronizes the user's meal nutrition records with it, so they remain available to other compatible health applications. The local app database stays the source of truth. Goals are not synchronized in the MVP.

## User story
As a user, I want my nutrition history to stay in sync with Google Health Connect so I can keep my data consistent across apps.

## Main behavior
- After each new log entry, the app writes the corresponding calories and macros to Health Connect as a `NutritionRecord`.
- If the user changes a logged value in the application, the system updates the related Health Connect record.
- If the user deletes a log entry in the application, the system removes the corresponding Health Connect record as well.
- When the connection is first made (or after the local data was deleted), existing Health Connect nutrition records are imported into the local database.
- Changes made in Health Connect are pulled into the app when it becomes active.
- The user can trigger a manual synchronization at any time from the app.
- Calorie and macro goals stay local (F-16); reading or writing goals is post-MVP.

## Acceptance criteria
- Each saved meal entry is written to Health Connect with the correct calorie and macro totals.
- Editing a meal in the app updates the corresponding record in Health Connect.
- Deleting a meal in the app removes the corresponding record from Health Connect.
- Sync runs when the app becomes active, and the user can manually start it.
- The sync works without requiring a cloud backend and remains local-first.

## MVP status
Necessary for MVP: Yes (meal records only)
