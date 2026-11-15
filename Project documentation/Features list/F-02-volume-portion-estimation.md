# F-02 | Volume / Portion Estimation

## Overview
Estimate how much food is present in the image so the app can calculate a realistic weight and nutritional values.

## User story
As a user, I want the app to estimate portion size accurately so I do not have to manually enter every gram or serving size.

## Core logic
- Estimate portion based on dish size and food fullness.
- Use the previous history of similar dishes to calibrate assumptions.
- Use reference data for common raw items and standard averages.
- Present the estimated values to the user for review and quick correction.

## Acceptance criteria
- The system provides a weight estimate after image analysis.
- The estimate is adjusted using prior user behavior when available.
- The user can override the estimate manually.
- The result feeds directly into the macro calculation.

## MVP status
Necessary for MVP: Yes
