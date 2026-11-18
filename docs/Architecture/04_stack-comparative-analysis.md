# Technology Stack Comparative Analysis for Snackshot

**Decision:** Select the implementation stack for Snackshot's Android MVP and planned post-MVP iOS adaptation.  
**Analysis date:** 10 September 2026  
**Recommendation:** Build the MVP as a native Android application with Kotlin, Jetpack Compose, Android Health Connect SDK, CameraX, and a provider adapter for LogMeal. Keep business logic, nutrition calculations, API contracts, and persistence boundaries modular so selected parts can be extracted into Kotlin Multiplatform shared modules when iOS work begins. Do not introduce a cross-platform UI framework, web target, or desktop target in the MVP.

> **Status note (18 November 2026):** this analysis was written for the original one-month, ~28-hour MVP window. That constraint was lifted on 15 November 2026; the current plan is ~75.5 h plus contingency, with the pilot release on 7 February 2027 ([04 WBS](../04_wbs_and_mvp_implementation_plan.md)). The stack recommendation is unchanged; the decision gate in §10 now refers to the WBS budget.

## 1. Decision question

Which technology stack gives Snackshot the fastest and lowest-risk path to an Android MVP while preserving a practical route to an iOS application after the MVP?

## 2. Frame of reference

Snackshot is an Android-first, privacy-centric nutrition application. The MVP must support camera-based food logging, LogMeal recognition and nutrition requests, user correction, local nutrition history, and Health Connect synchronization. The project has an approximately one-month delivery window and an estimated 28 hours of development capacity. The target platform decision is Android for the MVP, with iOS planned after the MVP; web and desktop are out of scope. Sources: [project charter](../01_project_charter.md) and [architecture answers](02_answers-architecture-checklist.md).

The most technically sensitive integrations are native Android concerns: Google Health Connect, camera permissions and capture, Android local persistence, and the LogMeal Android Depth SDK if depth-based quantity detection is enabled. The architecture must also honor the local-first boundary: the app's local data is the source of truth, Health Connect is an optional synchronization target, and LogMeal is a stateless processing dependency rather than the application's historical datastore. Source: [architecture checklist](01_must-answer-architecture-checklist.md), [architecture answers](02_answers-architecture-checklist.md), and [AI model comparative analysis](03_ai-model-comparative-analysis.md).

## 3. Grounds for comparison

The candidate stacks are compared against the requirements that can determine MVP success:

- **MVP delivery speed:** How much setup, framework learning, and integration work is required within approximately 28 hours?
- **Android integration fit:** Access to Health Connect, CameraX, Android permissions, local storage, and LogMeal's Android SDK.
- **Runtime and UX quality:** Native camera, responsive correction flow, reliable lifecycle handling, and predictable performance.
- **Post-MVP iOS path:** Ability to share useful logic without forcing the MVP to implement iOS prematurely.
- **Maintenance and risk:** Dependency layers, plugin quality, upgrade risk, debugging, and security boundaries.
- **Scope fit:** Compatibility with Android-only MVP and explicit exclusion of web and desktop targets.
- **Team and ecosystem fit:** Kotlin/Android documentation, testing, tooling, and likely implementation complexity.

### Thesis

> If a native Android Kotlin stack can satisfy the MVP directly and a modular architecture can preserve later Kotlin Multiplatform extraction, it is preferable to introducing cross-platform UI or plugin layers before iOS is required.

## 4. Scope, evidence, and limitations

The comparison uses the saved stack research:

- [Kotlin Multiplatform and cross-platform frameworks](stack/kotlin-cross-platform-frameworks.md)
- [Best tech stack for mobile app development](stack/best-tech-stack-mobile-development.md)

It also uses the project charter, architecture checklist, architecture answers, and LogMeal selection described in [03_ai-model-comparative-analysis.md](03_ai-model-comparative-analysis.md).

The supplied stack articles are general technology guidance, not controlled benchmarks for Snackshot. They do not provide measured build time, Health Connect integration results, LogMeal Depth SDK compatibility results, or team-specific skill evidence. Scores below are therefore architecture judgments, not performance measurements. The recommendation must be validated with a small Android integration spike.

The project charter contains an earlier broad statement describing Snackshot as cross-platform and web-enabled, but the later explicit product decision narrows the target: Android is the MVP platform, iOS is post-MVP, and web and desktop are not planned. This analysis follows the explicit platform decision recorded in the architecture answers.

## 5. Candidate stacks

### Option A: Native Android Kotlin

- Kotlin.
- Jetpack Compose for Android UI.
- Android SDK and AndroidX libraries.
- CameraX for camera capture and lifecycle-aware image handling.
- Health Connect Android SDK.
- Local application persistence behind a repository boundary, with Health Connect synchronization as a separate integration.
- LogMeal REST API through a provider adapter; LogMeal Android Depth SDK only if the quantity-detection gate passes.

This option uses native Android APIs directly. The stack research identifies native development as providing high performance, easy access to device APIs, and straightforward custom views and animations. Source: [best tech stack for mobile app development](stack/best-tech-stack-mobile-development.md).

### Option B: Kotlin Multiplatform shared core with native UIs

- Kotlin Multiplatform for shared domain logic, nutrition calculations, API contracts, validation, and selected data/persistence abstractions.
- Native Android UI and Android integrations for the MVP.
- Native iOS UI and HealthKit integrations after the MVP.
- Compose Multiplatform is optional, not required for the MVP UI.

The stack research describes this reusable shared-platform model as sharing networking, storage, analytics, logging, computation, state management, and screen-controller code while retaining native Android and iOS UIs. Kotlin Multiplatform can also integrate platform-specific APIs and reuse existing Kotlin expertise. Source: [best tech stack for mobile app development](stack/best-tech-stack-mobile-development.md) and [Kotlin Multiplatform article](stack/kotlin-cross-platform-frameworks.md).

### Option C: Kotlin Multiplatform with Compose Multiplatform shared UI

- Kotlin Multiplatform for shared logic.
- Compose Multiplatform for shared Android and iOS UI.
- Platform-specific code for Health Connect, HealthKit, camera, permissions, and LogMeal SDK details.

The Kotlin research states that Compose Multiplatform can share business logic and UI, potentially sharing up to 100% of application code. This maximizes future code reuse, but it adds a cross-platform UI layer before the product needs iOS.

### Option D: Flutter

- Dart and Flutter's rendering engine.
- One codebase targeting Android and iOS, with optional web and desktop targets.
- Plugins or platform channels for Health Connect, camera, and LogMeal integration.

The supplied research describes Flutter as a mature, popular framework with hot reload, Material Design support, its own rendering engine, and a single codebase for mobile, web, and desktop. The tradeoff is dependency on framework/plugin updates for new operating-system functionality and potential platform-plugin limitations. Sources: [Kotlin Multiplatform article](stack/kotlin-cross-platform-frameworks.md) and [best tech stack for mobile app development](stack/best-tech-stack-mobile-development.md).

### Option E: React Native

- JavaScript or TypeScript with React Native.
- Native-rendered UI primitives.
- Community packages or native modules for Health Connect, camera, and LogMeal SDK integration.

The supplied research describes React Native as a widely used open-source framework with Fast Refresh, native-rendered UI components, debugging support, and a large community. The tradeoff for Snackshot is the additional JavaScript-to-native boundary for Android-specific health, camera, and depth integrations. Source: [Kotlin Multiplatform article](stack/kotlin-cross-platform-frameworks.md).

### Option F: Ionic/Capacitor or another web-oriented cross-platform stack

- HTML, CSS, and JavaScript with Ionic and Capacitor/Cordova plugins.
- Shared code for mobile and potentially web.
- Plugin bridges for camera, storage, and native services.

The supplied research identifies the benefits of web technology familiarity and shared code, but also notes possible performance issues, third-party device API bugs, slower native plugins, framework limitations, and the need to wait for framework updates. This option is especially misaligned with Snackshot because web is explicitly out of scope. Source: [best tech stack for mobile app development](stack/best-tech-stack-mobile-development.md).

### Option G: .NET MAUI or Uno Platform

- C# and XAML.
- Shared projects targeting Android and iOS, with additional desktop or WebAssembly targets available.
- Native platform APIs through framework abstractions.

The Kotlin research describes .NET MAUI and Uno as capable cross-platform .NET stacks with multi-targeting, hot reload, native platform access, and active ecosystems. They are reasonable choices for teams already invested in .NET, but the current project evidence is Kotlin/Android-oriented and provides no C# team or integration evidence. Source: [Kotlin Multiplatform article](stack/kotlin-cross-platform-frameworks.md).

## 6. Point-by-point comparison

Scores use a 1-to-5 scale, where 5 means the strongest fit for this project. They represent documented capability and architecture judgment, not measured benchmarks.

| Dimension | Weight | Native Android Kotlin | KMP shared core + native UI | KMP + Compose shared UI | Flutter | React Native | Ionic/Capacitor | .NET MAUI/Uno |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| MVP delivery speed | 30% | 5 | 4 | 3 | 3 | 3 | 3 | 2 |
| Health Connect, camera, and LogMeal integration | 25% | 5 | 4 | 3 | 3 | 2 | 2 | 2 |
| Runtime and native UX quality | 15% | 5 | 5 | 4 | 4 | 4 | 2 | 4 |
| Post-MVP iOS reuse | 15% | 2 | 5 | 5 | 5 | 5 | 5 | 5 |
| Maintenance and dependency risk | 10% | 5 | 4 | 3 | 3 | 3 | 2 | 3 |
| Fit with Android-only/no-web scope | 5% | 5 | 5 | 4 | 2 | 2 | 1 | 2 |
| **Weighted score** | **100%** | **4.55** | **4.45** | **3.55** | **3.25** | **2.95** | **2.45** | **2.65** |

### Interpretation

- **Native Android Kotlin** scores highest because it minimizes MVP integration risk and directly matches the current platform decision. Its weakness is duplicated future iOS implementation if no shared boundaries are preserved.
- **Kotlin Multiplatform with a native UI** is nearly tied and is the best long-term architecture if the team can afford a small amount of upfront modularization. It shares the most valuable non-UI logic while preserving native access for Health Connect and the camera.
- **Kotlin Multiplatform with Compose Multiplatform UI** maximizes code sharing but adds unnecessary cross-platform UI scope before iOS is required. Its score would improve when iOS development starts and shared UI becomes a concrete priority.
- **Flutter** is viable for a new cross-platform product, but its strongest advantages are unused when web and desktop are out of scope and Android-specific integrations are central to the MVP.
- **React Native** has strong community and refresh tooling, but the project would introduce JavaScript/TypeScript and native-module coordination without evidence that it improves this particular MVP.
- **Ionic/Capacitor** is the weakest fit because device integrations and performance are central, while web is explicitly excluded.
- **.NET MAUI/Uno** can be effective for a .NET team, but the repository contains no evidence of C# expertise or a reason to add that ecosystem.

## 7. Key patterns and differences

1. **The MVP platform decision changes the optimization target.** The question is not which framework supports the most platforms. It is which stack delivers a reliable Android app now while preserving a controlled iOS path later.
2. **Native integration dominates the first release.** Health Connect, camera permissions, local lifecycle, and LogMeal's Android Depth SDK are more important than maximum UI code sharing.
3. **Kotlin is the lowest-friction language choice.** Kotlin is the native Android language and is also the shared language in the reusable platform model described by the stack research. This allows the project to defer iOS UI decisions without abandoning future reuse.
4. **Shared core is more valuable than shared UI at this stage.** Nutrition calculations, LogMeal request/response models, validation, provider adapters, and repository contracts can be shared later without forcing Compose Multiplatform into the first Android release.
5. **Web and desktop support would add cost without product value.** Flutter, Ionic, Uno, and .NET MAUI offer broader targets, but those targets are not in Snackshot's product scope.
6. **A full KMP setup has an upfront cost.** The project should not create shared modules speculatively if they slow the MVP. The practical compromise is native Android Kotlin with clean module and interface boundaries that can be migrated into KMP after the first validated release.

## 8. Tradeoffs, risks, and variables

### Controllable variables

- Whether the MVP uses native Android UI only or introduces shared UI.
- How strictly the code is separated into domain, data, integration, and presentation layers.
- Whether LogMeal quantity detection is enabled only on compatible devices.
- Whether the application uses regular recognition plus manual quantity confirmation for unsupported devices.
- The amount of shared code extracted before iOS work begins.
- The chosen local persistence implementation and synchronization schedule.
- The number of Android devices used for Health Connect, camera, and ARCore testing.

### External variables

- Health Connect API and Android OS behavior.
- LogMeal SDK compatibility, plan access, rate limits, API changes, and asynchronous quantity latency.
- ARCore and device hardware availability for depth-based quantity estimation.
- Future iOS HealthKit requirements, which are not equivalent to Health Connect.
- Framework and plugin update cadence.
- Team familiarity with Kotlin, Android, Compose, KMP, Dart, JavaScript, or C#.

### Critical risks

- **Overengineering the MVP:** Starting with shared UI or a full multi-platform project may consume the limited development budget before the Android meal-logging flow works.
- **Incorrect future-sharing assumptions:** Health Connect is Android-specific and iOS will require HealthKit integration. Not all storage or platform code can be shared unchanged.
- **Depth hardware fragmentation:** LogMeal quantity detection requires compatible depth/ARCore support; the app needs a regular-recognition and manual-quantity fallback.
- **API credential exposure:** LogMeal tokens must not be embedded as long-lived secrets in client-side code. **Resolved (14 November 2026):** a Firebase-based LogMeal Access Broker keeps the company token server-side and gives each anonymous client only its own revocable LogMeal user token, stored with Android Keystore encryption. See [11_logmeal-access-token-management.md](./11_logmeal-access-token-management.md).
- **Project charter inconsistency:** The charter's earlier cross-platform/web wording conflicts with the later Android-only MVP decision. The architecture decision should be treated as authoritative and the charter should be corrected separately.

## 9. Recommendation and rationale

### Recommended MVP stack

Use:

- **Language:** Kotlin.
- **Application target:** Android only.
- **UI:** Jetpack Compose with native Android components where integration requires them.
- **Camera:** CameraX or the selected Android camera API, with lifecycle-aware permission and capture handling.
- **Health data:** Android Health Connect SDK through a dedicated integration module.
- **Local data:** Local application persistence behind repository interfaces; Health Connect remains an optional synchronization target rather than the only persistence boundary.
- **AI:** LogMeal REST API behind a provider adapter. Add the LogMeal Android Depth SDK only after the device and plan validation succeeds.
- **Architecture:** Separate presentation, domain, local data, Health Connect, camera, and LogMeal integration boundaries.
- **Testing:** Unit tests for nutrition calculations and provider mapping, plus physical-device tests for camera, Health Connect permissions, sync, and representative LogMeal requests.

### Why not start with full cross-platform UI?

The MVP has only one required platform and a very small delivery budget. Full cross-platform UI would introduce framework and plugin decisions without reducing the immediate Android integration work. Native Kotlin provides direct access to the APIs that determine MVP feasibility and gives the team the shortest path to testing on a real Android device.

### How to preserve the iOS path

Keep the following logic platform-neutral from the beginning:

- Meal and nutrition domain models.
- Macro and calorie calculations.
- LogMeal request/response models and provider adapter contract.
- Recognition confidence and fallback policy.
- Repository interfaces for meals and user corrections.
- Synchronization abstractions, without assuming Health Connect exists on every platform.
- Validation and error types.

When iOS begins, evaluate extracting these modules into Kotlin Multiplatform. Keep Android Health Connect, iOS HealthKit, camera implementations, permissions, and platform UI in separate platform-specific modules.

## 10. Next steps and decision gates

1. Create the Android project in Kotlin with Jetpack Compose and a minimal module/package structure.
2. Build a vertical spike containing camera capture, image preprocessing, LogMeal regular recognition, result correction, local save, and Health Connect write.
3. Validate the selected Android device set and confirm whether ARCore is available for LogMeal quantity detection.
4. Test the fallback path for devices without depth support: regular image recognition plus manual grams or SMALL/MEDIUM/BIG quantity confirmation.
5. Measure build time, photo-to-result latency, correction time, Health Connect write success, and failure recovery on physical devices.
6. Add unit tests for nutrition calculations, provider mapping, and corrected-value persistence.
7. Do not introduce KMP shared modules until the Android vertical slice passes the MVP acceptance criteria, unless the team has a verified requirement to begin iOS work immediately.
8. Before starting iOS, revisit whether shared Kotlin modules or Compose Multiplatform provide enough benefit to justify their migration and maintenance cost.

### Decision gates

Proceed with the recommended native Android stack if:

- The camera-to-LogMeal-to-correction flow can be demonstrated on a physical Android device.
- Health Connect permissions and `NutritionRecord` writes work reliably.
- LogMeal tokens can be handled without exposing an unacceptable long-lived secret. ✅ Resolved by [11_logmeal-access-token-management.md](./11_logmeal-access-token-management.md).
- The regular-recognition fallback works on devices without depth support.
- The stack remains compatible with the MVP budget in the [WBS](../04_wbs_and_mvp_implementation_plan.md) (~75.5 h plus contingency).

Reconsider the stack if the project receives a firm requirement for iOS before MVP, if the team already has strong Flutter/React Native/.NET expertise, or if a required integration is unavailable in native Kotlin. Those conditions are not established in the current project evidence.

## 11. Final decision statement

Snackshot should use a **native Android Kotlin stack for the MVP**, centered on Jetpack Compose, CameraX, Health Connect, local repositories, and a LogMeal provider adapter. This choice best satisfies the immediate Android-only scope, native integration needs, privacy architecture, and 28-hour delivery constraint. The code should be organized for later extraction of platform-neutral logic into Kotlin Multiplatform, but the MVP should not pay the cost of shared UI, web support, desktop support, or a second platform before those capabilities are needed.
