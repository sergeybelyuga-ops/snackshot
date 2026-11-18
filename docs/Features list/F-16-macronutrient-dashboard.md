# F-16 | Macronutrient Dashboard

## Overview
Show the user how many calories and how much protein, fat, and carbohydrates they have consumed for a day, a week, a month, three months, and a year. Each range shows the totals and the average per day. If the user has set goals, the dashboard compares the intake with them.

## User story
As a user, I want to see my calories and macros for today and for longer periods, compared with my own goals when I have set them, so I can understand my meal quality over time.

## Main functionality
- A range selector with `D` (day), `W` (week), `M` (month), `3M` (three months), and `Y` (year), with previous, next, and current-period navigation.
- **D:** totals for the selected day and its meals.
- **W:** totals for all 7 days of the selected week and the daily average.
- **M:** daily totals for the calendar month and the daily average.
- **3M and Y:** monthly aggregates and the daily average; long ranges are aggregated by month.
- **Goals (optional):** the user can set daily calorie, protein, carbohydrate, and fat goals in Settings → Nutrition preferences. Goals are stored only in the app's local database and are **not** synchronized with Google Health Connect. When goals exist, the dashboard shows a goal band and macro progress; when they do not, it works normally and says that no goal is set.

## Acceptance criteria
- The app displays calories, protein, carbohydrates, and fat totals for the current day.
- The user can browse earlier days, weeks, months, three-month periods, and years.
- The weekly view includes all 7 days of the week with their own totals and the average daily values.
- The month, three-month, and year views show totals and the average daily values.
- The user can set, change, and clear goals; all dashboard views work without goals.
- Goals never leave the device.
- The values are derived from logged meals and updated automatically as entries are saved, edited, or deleted.
- Every chart has an accessible text summary.

## MVP status
Necessary for MVP: Yes

## Related feature IDs
- F-15 Calorie Tracking (post-MVP; target features beyond the optional goals above)
- F-25 Statistic Dashboard (post-MVP; trend analysis beyond these ranges)
- F-23 Calories & Macros Log Feed
