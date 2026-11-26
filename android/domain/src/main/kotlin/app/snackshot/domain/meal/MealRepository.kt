package app.snackshot.domain.meal

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** The local meal log, the app's source of truth (doc 08). */
interface MealRepository {
    /** Meals eaten in `[from, until)`, newest first. Emits again on every save or delete. */
    fun observeMeals(from: Instant, until: Instant): Flow<List<MealEntry>>

    fun observeMeal(id: MealId): Flow<MealEntry?>

    suspend fun getMeal(id: MealId): MealEntry?

    /**
     * Creates or replaces the meal and marks it for Health Connect upload in the same
     * transaction. Saving a deleted meal again (undo) brings it back with a newer version.
     */
    suspend fun save(entry: MealEntry)

    /** Deletes the meal; a meal already in Health Connect stays queued for a remote delete. */
    suspend fun delete(id: MealId)
}

/** Health Connect sync bookkeeping, used by the sync engine (WBS 8.2). */
interface SyncStateRepository {
    suspend fun get(mealId: MealId): SyncState?

    /** Meals waiting for an upsert or a remote delete, oldest change first. */
    suspend fun pending(): List<SyncState>

    /** Records a finished upload; see [SyncState.uploaded]. No-op if the meal has no sync state. */
    suspend fun markUploaded(mealId: MealId, uploadedVersion: Long, healthConnectRecordId: String)

    /** Forgets a meal whose remote delete finished, unless it was saved again meanwhile. */
    suspend fun markRemoteDeleted(mealId: MealId)
}
