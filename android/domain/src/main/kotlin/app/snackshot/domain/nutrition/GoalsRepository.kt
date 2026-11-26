package app.snackshot.domain.nutrition

import kotlinx.coroutines.flow.Flow

/** Optional local goals (F-16). They never leave the device. */
interface GoalsRepository {
    /** The current goals; [NutritionGoals.NONE] until the user sets one. */
    val goals: Flow<NutritionGoals>

    /** Replaces all goals. Pass [NutritionGoals.NONE] to clear them. */
    suspend fun setGoals(goals: NutritionGoals)
}
