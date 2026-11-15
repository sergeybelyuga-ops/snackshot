# Project Charter

**Snackshot: AI-Powered Nutrition & Calorie Tracker**

**Status:** APPROVED
**Document Version:** 1.2
**Date:** August 23, 2026 (v1.2: November 14, 2026)

## 1. Project Purpose & Justification

To develop a cross-platform mobile and web application, **Snackshot**, that utilizes artificial intelligence to identify food items and estimate nutritional parameters (calories, macros) directly from user-captured images.

The primary differentiator of this application is that it has **no backend for user data**. Meal logs, nutrition history, and photos stay on the device (optionally synchronized with Google Health Connect), which avoids server hosting costs for user data, reduces privacy exposure, and gives users a private, local-first tracking experience. The only server component is a minimal **LogMeal Access Broker** that protects the AI provider credentials and tracks each anonymous client's AI usage; it stores no health data (see [LogMeal access token management](Architecture/11_logmeal-access-token-management.md)).

## 2. Project Objectives & Success Criteria

* **Objective 1:** Implement an accurate AI vision integration that minimizes manual food entry.
* **Objective 2:** Achieve seamless read/write synchronization with Google Health Connect (specifically the `NutritionRecord` data type).
* **Objective 3:** Build a cross-platform UI ensuring high responsiveness and low-friction logging.
* **Objective 4:** Deliver a functioning Minimum Viable Product (MVP) within a one-month development window.

## 3. High-Level Requirements

* Camera integration and UI workflow for capturing and cropping food images.
* Integration with a third-party AI image recognition API for food identification and volume estimation.
* Local data storage and synchronization engine interfacing directly with Google Health Connect APIs.
* Fallback mechanisms for manual entry and barcode scanning.
* Daily macro dashboard (Calories, Protein, Fat, Carbs) reading directly from local device history.

## 4. Scope & Architecture Constraints

As a system built for zero cloud-storage footprint for health data, the architecture strictly mandates that no user dietary logs or personal health information will be persisted on external servers. The LogMeal Access Broker stores only an anonymous client ID, a hashed device ID, AI usage counters, and trial/subscription status. All historical tracking resides on the device within Health Connect. The application will act as a stateless client routing image payloads to the AI processing layer and formatting the returned data for local storage.

## 5. In Scope (MVP)

The MVP scope is limited to the features that are necessary for the product to validate the core value proposition: quick, private, AI-assisted meal logging directly into Google Health Connect.

### 5.1 Included in MVP

* AI food identification from a camera photo.
* Visual estimate of food portion size or weight.
* Fast in-app correction flow for recognized food items and portion estimates.
* Manual fallback for food lookup by text search.
* Quick manual calorie entry as a backup when AI recognition is uncertain.
* Macronutrient dashboard showing Calories, Protein, Fat, and Carbs.
* Mandatory Android Health Connect integration for local read/write nutrition logging.
* No registration or sign-in: the app and AI recognition are usable immediately; identity is a silent anonymous ID tied to the device.
* Free AI trial of 90 recognized pictures per user, followed by a subscription paywall (paywall screen only in the MVP; payments post-MVP).
* LogMeal Access Broker (Firebase) with a basic admin console to manage per-user LogMeal access.
* Meal history feed showing logged meals and their macro totals.

### 5.2 Out of Scope for MVP

The following features are explicitly deferred beyond the MVP phase and are not required for the initial release:

* Mixed plate breakdown with multiple distinct food items in one photo.
* AI chat or voice-based meal logging.
* Barcode scanning for packaged goods.
* Custom recipes and reusable meal templates.
* Calorie target algorithms and adaptive health coaching.
* Advanced statistical dashboards and trend analysis beyond basic macro logging.
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
| **Monetization & API Cost Control** | The MVP phase is a closed pilot for friends and family, capped at **5 users**. Each user gets **90 free AI-recognized pictures** in total; after that a subscription paywall is shown (MVP: screen only, "subscriptions coming soon"). Manual logging stays free. Post-MVP, subscriptions are sold through Google Play Billing. Per-user AI usage, pause, and cancellation are managed by the LogMeal Access Broker. |
| **Development Timeline & Effort** | **Start Date:** Monday, August 24, 2026<br>

<br>**Target MVP Release:** Late September 2026 (1 Month duration)<br>

<br>**Available Capacity:** 7 hours per week (Mon: 2h, Wed: 2h, Sun: 3h). Total estimated MVP effort: ~28 hours. |
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
