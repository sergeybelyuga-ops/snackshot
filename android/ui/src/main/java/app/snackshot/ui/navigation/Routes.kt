package app.snackshot.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe destinations (doc 10 §2.2). Later increments add their screens here.

@Serializable data object WelcomeRoute

@Serializable data object PrivacyDetailsRoute

@Serializable data object HomeRoute

@Serializable data object DashboardRoute

@Serializable data object HistoryRoute

@Serializable data object SettingsRoute

@Serializable data object QuickAddRoute

@Serializable data object NutritionPreferencesRoute

/** One goal's detail; [goal] is a `GoalKind` name, read by `GoalDetailViewModel`. */
@Serializable data class GoalRoute(val goal: String)
