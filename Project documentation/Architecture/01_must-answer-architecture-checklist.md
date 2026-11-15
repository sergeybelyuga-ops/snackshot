# Must-Answer Architecture Checklist

## Purpose
This checklist defines the technical questions and required architecture artifacts that must be answered before the MVP implementation begins. It complements the project charter and feature list and is intended to reduce uncertainty around Health Connect, AI recognition, local-first storage, and the app workflow.

## Must-answer questions

### 1. Platform and app structure
- What is the target platform for the MVP: Android only or cross-platform from the beginning?
- What technology stack will be used for the client application?
- What are the main app modules or layers: onboarding, AI chat, camera flow, dashboard, history, settings, Health Connect integration?
- What is the navigation model between screens and flows?
- What is the local app state model and lifecycle model?

### 2. Health Connect and Google account dependency
- Is a Google account mandatory for Health Connect access?
- If yes, when is the Google account login required: during onboarding, before first save, or before Health Connect authorization?
- What happens if the user has no Google account or declines access?
- Can users enable, disable, or re-enable Health Connect integration in profile settings later?
- What is the fallback behavior when Health Connect is unavailable, denied, or unsupported?

### 3. Data ownership and local-first architecture
- What data stays only on the device?
- What data is written to Health Connect?
- What data is intentionally not stored anywhere external to the device?
- What is the source of truth for nutrition records: local app data, Health Connect, or both?
- How is local-first storage organized and persisted?
- How do we handle consistency when the same record exists in both local storage and Health Connect?

### 4. Data model and sync logic
- What exact fields are stored for each meal entry?
- Which fields map directly to Health Connect nutrition records?
- How are calories, protein, carbs, and fat represented in the app data model?
- How are timestamps, photo references, and user edits tracked?
- What is the sync strategy for create, update, and delete actions?
- Is sync automatic, manual, or both?
- How often are goals refreshed from Health Connect?
- What is the conflict resolution strategy when app data and Health Connect data differ?

### 5. AI food recognition and portion estimation
- Which AI service or model is used for food classification and portion estimation?
- What is the request/response contract with the AI provider?
- What image types are supported and what preprocessing is required?
- What input categories are supported: meal, packaged product, raw fruit/vegetable, drink, or mixed list?
- What is the confidence threshold for accepted results?
- What is the fallback path when the AI result is uncertain or wrong?
- How are container size, fullness, and user history used in portion estimation?
- How is the app’s “Zero-Edit” metric measured?

### 6. Nutrition calculation logic
- Which source defines calories and macros for recognized foods?
- How are composite meals broken into ingredients or components?
- How are macros calculated from portion size or weight?
- What happens when the user manually edits the calculated values?
- How are historical aggregation and weekly averages calculated?
- How are daily totals displayed and updated in real time?

### 7. User flows and UX dependencies
- What is the first-time onboarding flow?
- What is the exact camera workflow and permission model?
- What is the flow for AI chat input: text, audio, image, and gallery?
- What is the flow for manual text search and quick manual entry?
- How does the app handle invalid input, failed AI recognition, and user cancelation?
- How can the user edit or delete a previously saved log entry?

### 8. Privacy, security, and compliance
- Does the app store user data on external servers?
- Where is personal nutrition data stored and how is it protected?
- What consent text is shown before Health Connect authorization?
- What is the user data retention model?
- Does the app handle PII or health data that could require compliance review?
- What are the privacy guarantees for local-only mode?

### 9. Performance, reliability, and scalability
- What is the target recognition latency from photo capture to result?
- What is the acceptable success rate for AI recognition and zero-edit logging?
- How does the app behave if the AI service is slow or unavailable?
- What is the retry strategy after failed requests?
- How will the app behave with larger histories, weekly dashboards, and multiple food entries?
- What are the reliability requirements for sync failures and retries?

### 10. Metrics and validation
- Which metrics are tracked in app analytics?
- Which metrics are tracked from the data store or Health Connect history?
- How are North Star metrics measured for recognition quality, speed, zero-edit rate, habit formation, and time-to-log?
- What testing flow is required on physical Android devices before MVP launch?

## Required architecture documents
The following documents should be produced as part of the technical design before implementation begins.

### 1. Context and system view
- Context diagram
- System landscape diagram
- High-level architecture overview

### 2. C4 model
- C4 Context diagram
- C4 Container diagram
- C4 Component diagram
- C4 Code-level diagram only where needed for key business logic

### 3. Deployment architecture
- Deployment diagram
- Environment diagram for local development, test, and production
- Device and OS requirements for Android and Health Connect compatibility

### 4. Data architecture
- Data flow diagram
- Entity relationship diagram for app data and Health Connect mapping
- Local data storage design
- Sync flow diagram for create/update/delete operations
- Goal synchronization flow diagram

### 5. Integration architecture
- AI provider integration diagram
- Health Connect integration diagram
- External dependency map
- API contract documentation for the AI service
- Error handling and retry strategy documentation

### 6. Security and privacy architecture
- Privacy design overview
- Permission model for camera, gallery, and Health Connect
- Data storage and retention policy
- Security review notes for Health Connect and Google account usage

### 7. Operational architecture
- Monitoring and logging strategy
- Error reporting and alerting plan
- Performance targets and test plan
- Release checklist for MVP deployment

## Minimum artifacts required before MVP implementation
At a minimum, the project should have:
- One architecture overview document
- One C4 context diagram
- One deployment diagram
- One data flow diagram
- One Health Connect integration design
- One AI integration design
- One local storage and sync model
- One privacy and permissions summary

## Recommended order of work
1. Confirm the platform and stack.
2. Confirm Google account and Health Connect requirements.
3. Define the local-first data model.
4. Define the sync model and conflict rules.
5. Design the AI integration contract.
6. Document the deployment and environment setup.
7. Validate the architecture against MVP scope and North Star metrics.

## Output expectation
This document should be used as the checklist for the architecture phase. If any item is still unknown, it should be marked as a decision required before implementation starts.
