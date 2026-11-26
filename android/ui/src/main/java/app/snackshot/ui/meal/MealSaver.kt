package app.snackshot.ui.meal

import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealEntry
import app.snackshot.domain.meal.MealRepository
import java.time.Clock
import javax.inject.Inject

/**
 * The shared save path (WBS 5.2): Quick add now, photo results (6.x) and history edits (7.x) later.
 * Saving locally is always first; the Health Connect prompt (8.1) hooks in after it.
 */
class MealSaver @Inject constructor(private val meals: MealRepository, private val clock: Clock) {
    /** Saves a valid draft and returns the saved meal. */
    suspend fun save(draft: MealDraft): MealEntry {
        val entry = draft.toEntry(clock.instant())
        meals.save(entry)
        return entry
    }
}
