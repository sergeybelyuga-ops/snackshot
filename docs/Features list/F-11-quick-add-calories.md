# F-11 | Quick Add Calories

## Overview
Let the user log a flat calorie/macro value manually when they want a quick entry without a food search or AI recognition.

## User story
As a user, I want to add calories quickly when I know the meal value already so I can log it in one step.

## Acceptance criteria
- The user can input a food name and values manually.
- The app stores the meal with the entered calorie and macro totals.
- This path works as a fallback when AI is not useful or not available: recognition failed, the free AI trial ended, the user is waitlisted, the broker is unreachable, or LogMeal's daily limit is reached.
- Quick add is always free and never needs AI access, the broker, or a network connection.
- The kcal value can be calculated from the entered macros on request.
- Values must be non-negative, and at least one nutrition value must be entered.

## Screens
S-02 Home (Manual add in the carousel) → S-09 Quick add → local save. S-05, S-19, and S-20 also open S-09.

## MVP status
Necessary for MVP: Yes (always free)
