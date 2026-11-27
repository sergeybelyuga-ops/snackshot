package app.snackshot.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe destinations (doc 10 §2.2). Later increments add their screens here.

@Serializable data object WelcomeRoute

@Serializable data object PrivacyDetailsRoute

@Serializable data object HomeRoute

@Serializable data object DashboardRoute

@Serializable data object HistoryRoute

@Serializable data object SettingsRoute

/** S-09. [fromDraft]: opened from a failed recognition, keeping its photo and time (doc 10 §4.8). */
@Serializable data class QuickAddRoute(val fromDraft: Boolean = false)

/** S-03 camera (WBS 6.1). */
@Serializable data object CameraRoute

/**
 * S-05 recognition progress (WBS 6.4). [capturePath] (a camera capture) or [pickedUri] (a Photo
 * Picker item) starts a new recognition; with neither, it resumes the pending one.
 */
@Serializable data class RecognitionRoute(val source: String, val capturePath: String? = null, val pickedUri: String? = null)

/** S-15 meal review of the pending draft. */
@Serializable data object MealReviewRoute

@Serializable data object NutritionPreferencesRoute

/** One goal's detail; [goal] is a `GoalKind` name, read by `GoalDetailViewModel`. */
@Serializable data class GoalRoute(val goal: String)
