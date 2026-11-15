> Source: https://docs.logmeal.com/docs/guides-essential-concepts-users-and-access-tokens

# User Types & Access Tokens

> Tokens determine who is making a request and what resources they can access in the LogMeal API and Platform.

## User Types

| User type | Usage |
|---|---|
| **APICompany** | Account-level manager created automatically when you sign up. Responsible for creating and managing APIUsers and APIUserManagers. Only one exists per subscription. |
| **APIUser** | Represents an end user of your app. Each final user must have their own APIUser. A user-specific language and country can be assigned during creation. APIUsers can log meals, submit intakes, run food recognition, and track nutritional history. |
| **APIUserManager** | Supervisor user such as a healthcare professional or nutritionist. Available only in higher-tier plans such as Monitor or higher. Can view and manage intakes of multiple APIUsers. |

## Access Tokens

Each user has a unique, non-transferable secret token used to authenticate API calls.

- **APICompany token:** Issued when subscribing to the API and found in the Profile section of the dashboard. Used to create and manage APIUsers and APIUserManagers.
- **APIUser token:** Issued automatically when creating an APIUser. Required for recognition endpoints such as image analysis and intake submission. APIUsers can also update their profile information.
- **APIUserManager token:** Issued automatically when creating an APIUserManager. Grants access to dashboards and endpoints for managing user intakes and goals.

> **Best practice:** Always keep tokens secret. Treat them like passwords. Do not expose them in client-side code.

## Creating Users

You can create users through the LogMeal Users Dashboard or the User Management API.

### Web Dashboard

1. Log in with your company account at the [LogMeal Login Page](https://www.logmeal.com/login).
2. Go to the [LogMeal Users Dashboard](https://www.logmeal.com/api/users).
3. Choose whether to add an APIUser or APIUserManager.
4. Fill in details such as language, username, email, and password if required.
5. After creation, LogMeal assigns an ID and token.

### User Management API Endpoints

Use the APICompany token to call user management endpoints:

- Create APIUser: [POST /users/signUp](https://docs.logmeal.com/reference/post_v2-users-signup#/)
- Create APIUserManager: [POST /managers](https://docs.logmeal.com/reference/post_v2-managers#/)
- Assign APIUserManager access to an APIUser: [POST /managers/userAccess](https://docs.logmeal.com/reference/post_v2-managers-useraccess#/)
- Modify APIUser profile: [POST /users/modifyUserProfileInfo](https://docs.logmeal.com/reference/post_v2-managers-modifyuserprofileinfo-userid#/)
- Enable or disable APIUser accounts: [POST /users/toggleEnabled/{userId}](https://docs.logmeal.com/reference/post_v2-users-toggleenabled-userid#/)

The profile endpoint can update country and language. Country ensures nutritional values and datasets are adapted to local standards; language personalizes dish, ingredient, and nutritional indicator names.

When an APIUser is disabled, the user can no longer log in to LogMeal Apps or Platform or use credentials to access APIs. When re-enabled, the same user can resume usage with the existing configuration.

## Example Workflows

### App end user

1. A mobile app creates an APIUser for each registered user.
2. The app uses that APIUser token for calls such as [POST /image/segmentation/complete](https://docs.logmeal.com/reference/post_v2-image-segmentation-complete#/) or [GET /intake/{imageId}](https://docs.logmeal.com/reference/get_v2-intake-imageid#/).
3. The user can view intake history and nutrition insights and update profile preferences such as language and country.

### Nutrition platform

A clinic creates APIUserManagers for dietitians or healthcare professionals. Each manager can view the intake history of multiple APIUsers and use the LogMeal Platform to analyze data, set goals, and monitor adherence.

### Company-level control

An administrator uses the APICompany token to onboard users, assign supervisors, and manage organizational settings.

## Next Steps

- [Image Pre-processing](./image-pre-processing.md)
- [Plans & Limits](../logmeal-plans-limits.md)
- [API Documentation](https://docs.logmeal.com/reference/getting-started#/)
