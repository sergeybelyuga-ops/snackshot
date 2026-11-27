package app.snackshot.domain.meal

import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow

/**
 * An unfinished meal flow, persisted so it survives backgrounding and process death
 * (doc 02 §1.5). There is at most one at a time.
 */
sealed interface PendingMeal {
    /** The processed image reference, if the flow has one; it is owned by the draft until saved. */
    val imageRef: String?

    /**
     * A photo was processed and stored, but there is no recognition result yet: recognition is
     * running, was interrupted, or failed. Restoring retries it or offers Quick add (S-05).
     */
    data class AwaitingRecognition(
        val source: MealSource,
        override val imageRef: String,
        val capturedAt: Instant,
        val zoneOffset: ZoneOffset,
        val failedAttempts: Int = 0,
        /** Why the last attempt failed, so S-05 can show it again after the app was closed. */
        val lastFailure: RecognitionFailure? = null,
    ) : PendingMeal {
        init {
            require(source.isAiRecognition) { "Only photo and gallery meals wait for recognition" }
            require(failedAttempts >= 0) { "failedAttempts must not be negative" }
        }
    }

    /** S-15 review, S-07 edit sheet, or S-09 Quick add, with every correction made so far. */
    data class Reviewing(val draft: MealDraft) : PendingMeal {
        override val imageRef: String? get() = draft.imageRef
    }
}

/** Persistence for the single [PendingMeal]. */
interface PendingMealStore {
    /** The pending flow, or null. Emits after every change. */
    val pending: Flow<PendingMeal?>

    /** Replaces the pending flow. Call after every change the user would not want to lose. */
    suspend fun save(meal: PendingMeal)

    /** The meal was saved: forget the draft, and keep its image, which the meal now owns. */
    suspend fun complete()

    /** The user discarded the flow: forget the draft and delete its image. */
    suspend fun discard()
}
