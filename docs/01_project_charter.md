# Project Charter

**Snackshot: AI-Powered Nutrition & Calorie Tracker**

**Status:** APPROVED
**Document Version:** 1.4
**Date:** August 23, 2026 (v1.2: November 14, 2026; v1.3: November 15, 2026; v1.4: November 18, 2026)

## 1. Project Purpose & Justification

To develop a mobile application, **Snackshot** (native Android for the MVP, iOS planned after the MVP; no web or desktop version), that utilizes artificial intelligence to identify food items and estimate nutritional parameters (calories, macros) directly from user-captured images.

The primary differentiator of this application is that it has **no backend for user data**. Meal logs, nutrition history, goals, and photos stay on the device (meal logs optionally synchronized with Google Health Connect), which avoids server hosting costs for user data, reduces privacy exposure, and gives users a private, local-first tracking experience. The only server component is a minimal **LogMeal Access Broker** that protects the AI provider credentials and tracks each anonymous client's AI usage; it stores no health data and no personal data (see [LogMeal access token management](Architecture/11_logmeal-access-token-management.md)).

## 2. Project Objectives & Success Criteria

* **Objective 1:** Implement an accurate AI vision integration that minimizes manual food entry.
* **Objective 2:** Achieve seamless read/write synchronization with Google Health Connect (specifically the `NutritionRecord` data type).
* **Objective 3:** Build a native Android UI ensuring high responsiveness and low-friction logging, with platform-neutral logic kept ready for a later iOS version.
* **Objective 4:** Deliver a functioning Minimum Viable Product (MVP) with the full MVP scope; the timeline is extended as needed (decision of November 15, 2026).

## 3. High-Level Requirements

* Camera and phone-gallery integration and UI workflow for capturing and selecting food images.
* Integration with a third-party AI image recognition API for food identification and volume estimation.
* Local data storage and an optional synchronization engine interfacing directly with Google Health Connect APIs.
* Quick manual entry of calories and macros, always available and free, also as the fallback when AI is unavailable.
* Macro dashboard (Calories, Protein, Fat, Carbs) for day, week, month, three months, and year, reading directly from local device history, with optional user-defined goals.

## 4. Scope & Architecture Constraints

As a system built for zero cloud-storage footprint for health data, the architecture strictly mandates that no user dietary logs or personal health information will be persisted on external servers. The LogMeal Access Broker stores only an anonymous client ID, a hashed device ID, the client's own LogMeal user ID and encrypted token, AI usage counters, trial/subscription status, and anonymous daily product metrics (for example AI response time and usage statistics) that contain no personal data. All historical tracking resides in the app's local database on the device, which is the source of truth; Google Health Connect is an optional synchronized copy of the meal logs. The application will act as a stateless client routing image payloads to the AI processing layer and formatting the returned data for local storage.

## 5. In Scope (MVP)

The MVP scope is limited to the features that are necessary for the product to validate the core value proposition: quick, private, AI-assisted meal logging directly into Google Health Connect.

### 5.1 Included in MVP

* AI food identification from a camera photo or a phone-gallery image (one dish or product per photo).
* Visual estimate of food portion size or weight.
* Fast in-app correction flow for recognized food items and portion estimates.
* Quick manual calorie and macro entry (Quick add), always free.
* Macronutrient dashboard showing Calories, Protein, Fat, and Carbs for day, week, month, three months, and year, with optional goals that the user sets in the app (stored only locally, not synchronized).
* Optional Google Health Connect integration for read/write nutrition logging; the app works fully without it.
* No registration or sign-in: the app and AI recognition are usable immediately; identity is a silent anonymous ID tied to the device.
* Free AI trial of 90 recognized pictures per user, followed by a subscription paywall (paywall screen only in the MVP; payments post-MVP).
* LogMeal Access Broker (Firebase) with a basic admin console to manage per-user LogMeal access.
* Meal history feed showing logged meals and their macro totals.
* Anonymous product metrics sent through the LogMeal Access Broker (daily aggregates such as AI response time and usage statistics, no personal data).

### 5.2 Out of Scope for MVP

The following features are explicitly deferred beyond the MVP phase and are not required for the initial release:

* Mixed plate breakdown with multiple distinct food items in one photo, and packaged-product identification.
* AI chat logging (text) and voice-based meal logging.
* Manual text search for foods.
* Barcode scanning for packaged goods.
* Custom recipes and reusable meal templates.
* Calorie target algorithms and adaptive health coaching.
* Advanced statistical dashboards and trend analysis beyond the macro dashboard ranges listed in §5.1.
* Synchronization of goals with Google Health Connect.
* Weight/body tracking, water tracking, fasting timers, and other lifestyle features.

## 6. North Star Metrics

The primary success indicators for the MVP are the following metrics, which connect AI performance, UX quality, and long-term habit formation:

* **Quality of Food Recognition:** The accuracy of both identifying the correct food items and estimating the correct volume or portion size.
* **Speed of Food Recognition:** The latency between taking the photo and the AI returning the macro breakdown.
* **The "Zero-Edit" Rate:** The percentage of AI food scans that users save to Health Connect without making any manual adjustments. This perfectly bridges technical metrics with the user experience—if this number is high, the AI is fast, accurate, and frictionless.
* **Core Habit Formation:** The percentage of active users who successfully log at least two meals per day for 4 or more days a week. This proves the app is actively helping people build consistency.
* **Time-to-Log (End-to-End Latency):** The average total time it takes a user from opening the app to hitting "Save." This measures the efficiency of the entire UI flow, not just the AI API processing time.

## 7. Business Context & Strategy

| Category | Details |
| --- | --- |
| **Project Name** | Snackshot |
| **Monetization & API Cost Control** | The MVP phase is a closed pilot for friends and family, capped at **5 users** (Snackshot's own limit; LogMeal's special conditions place no limit on the number of LogMeal users). Each user gets **90 free AI-recognized pictures** in total; after that a subscription paywall is shown (MVP: screen only, "subscriptions coming soon"). Manual logging (Quick add) stays free. Post-MVP, subscriptions are sold through Google Play Billing. Per-user AI usage, pause, and cancellation are managed by the LogMeal Access Broker. |
| **Development Timeline & Effort** | **Start Date:** Monday, August 24, 2026<br>

<br>**Target MVP Release:** February 7, 2027 (pilot release). Extended by the decision of November 15, 2026 (the full MVP scope is kept and the timeline is extended) and re-planned after the scope revision of November 18, 2026. See [04 WBS and MVP Implementation Plan](04_wbs_and_mvp_implementation_plan.md).<br>

<br>**Available Capacity:** 7 hours per week (Mon: 2h, Wed: 2h, Sun: 3h). Total estimated MVP effort: ~75.5 hours plus ~6.5 hours contingency. |
| **Target Audience** | Adults of all genders, approximately 40 years old, who are motivated to lose weight, improve general health, and prefer a privacy-centric, low-friction tracking solution. |

## 8. Development Roadmap & Action Plan

1. **Market Research:**
* Check Google Play and find applications that do the same.
* Check pricing models of competitors.
* Check which functions this kind of application usually has.


2. **Business Requirements:**
* Define which features are necessary.
* Define basic goals of the application.
* Prepare MD Business Description Document.


3. **Solutions & Limitations Definition:**
* Health Connect usage constraints and boundaries.
* AI for picture recognition.
* Service to define calories and nutrition.
* Backend-less vs backend solution assessment (decided: no backend for user data; minimal credential broker only).
* Stack and language usage selection.


4. **Architecture & Landscape:**
* Prepare MD documentation of the architecture.
* Prepare MD description for the API of the external services.
* Prepare landscape diagram and deployment diagram.
* Prepare development and test environments description.
* Prepare WBS and plan of implementation (feature by feature) for the MVP and for future service development.


5. **Technical Specifications:** Prepare technical specifications for each business feature.
6. **MVP Implementation:**
* Start implementation using AI (GitHub Copilot) feature by feature.
* Test each feature and approach using local resources and a smartphone.


7. **Deployment:** Deployment and publication of the application in the Google Play Store.
