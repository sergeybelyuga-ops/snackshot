# F-32 | Free AI Trial & Paywall

## Overview
Every user gets **90 free AI-recognized pictures** in total (a one-time trial, not monthly). After the 90th picture, AI recognition is locked and a subscription paywall is shown. In the MVP the paywall is a screen only ("Subscriptions are coming soon"); real payments through Google Play Billing come after the MVP. Quick add (F-11), history, dashboards, goals, and Google Health Connect sync are always free.

The MVP is a closed pilot capped at **5 users**. When the pilot is full, new users who try AI are placed on a waitlist and can still log meals with Quick add.

Design details: [11_logmeal-access-token-management.md](../Architecture/11_logmeal-access-token-management.md) §5.1, §6.1a, §6.4.

## User story
As a new user, I want to try AI meal recognition for free and always know how many free pictures I have left, so I can decide whether a subscription is worth it.

## Main behavior
- **One picture** = one successful AI recognition of a photo or gallery image. Failed or cancelled recognitions do not count. Editing the result, changing grams, or re-saving does not count.
- S-02 Home and the camera screen show a small counter: "57 of 90 free pictures left". Settings shows the same counter under "AI recognition".
- At 10 pictures left, the app shows a one-time heads-up after the result is saved.
- After the 90th picture, the next AI action opens the **S-19 Paywall**:
  - MVP: title "You've used your 90 free AI pictures", text "Subscriptions are coming soon. You can keep logging meals with Quick add.", buttons **Quick add**, **Close**.
  - Post-MVP: subscription offer with price and **Subscribe** (Google Play Billing), plus **Restore purchase**.
- If the pilot is full, the first AI action opens **S-20 Waitlist**: "AI recognition is in a closed pilot. You're on the waitlist — we'll enable it as soon as a place frees up." with the same buttons (**Quick add**, **Close**).
- A recognition already running when the limit is reached completes normally.
- LogMeal's own limit of 20 pictures per day per user still applies; when reached, show "Daily AI limit reached — try again in N h" with **Quick add**.

## Acceptance criteria
- The counter is correct after app restarts and after reinstalling on the same phone.
- The paywall appears on the first AI action after 90 counted pictures and never blocks Quick add, history, dashboards, goals, or Google Health Connect.
- The waitlist screen appears when the pilot cap is reached and disappears once an admin approves the user.
- No payment UI or payment SDK is shipped in the MVP.
- Paywall and waitlist states are announced to screen readers and meet the shared state contract (loading, error with retry).

## MVP status
Necessary for MVP: Yes (paywall as a screen only)

## Related feature IDs
- F-01 AI Food Identification
- F-30 Account-free App Access
- F-33 Broker Admin Console
- F-11 Quick Add Calories
