# F-19 | Connect to Health Connect (Mandatory)

## Overview
When the user saves their data for the first time, the system asks whether they want to connect to Google Health Connect to store their data and keep it available across supported services. The app explains that it does not store user data on its own servers; the data stays on the phone, and the connection to Google Health Connect helps preserve historical records and share them with other compatible applications.

## User story
As a user, I want to decide whether my nutrition history is synced to Health Connect so I can understand the privacy tradeoff and keep my data available across apps while still keeping the app local-first.

## Main behavior
- On first data save, the app offers a Health Connect connection prompt.
- The prompt explains that the application does not store customer data anywhere on the server; all data remains on the phone.
- The app explains that the Google Health Connect connection helps save historical data and share data with other applications.
- If the user declines the proposal, they can manage this connection later in the profile screen.
- If the user declines, the application stores user data only locally on the phone.
- If the user confirms, the app shows the system Health Connect authorization dialog.
- After confirmation, every new entry is synchronized with Health Connect automatically.
- If Health Connect integration is enabled, the app reads user data from Health Connect, including calorie and macro goals.

## Acceptance criteria
- The app asks the user to connect to Health Connect when they first save data.
- The explanation clearly states that the app does not store customer data on external servers and that data remains on the phone.
- The explanation clearly states that Health Connect helps preserve historical data and share it with other supported applications.
- If the user declines, the app keeps working in local-only mode and the setting can be changed later in the profile.
- If the user accepts, the standard Health Connect authorization dialog appears.
- New log entries synchronize automatically after authorization.
- The app reads calorie and macro goals from Health Connect when integration is enabled.

## MVP status
Necessary for MVP: Yes
