package app.snackshot.datalocal.images

import app.snackshot.datalocal.db.SnackshotDatabase
import app.snackshot.domain.meal.ImageRetention
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.meal.RetentionReport
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Applies [ImageRetention] (doc 02 §8.4): processed photos older than 365 days are deleted and
 * their meals keep all values without a photo; unreferenced photos and leftover raw captures
 * are deleted after a day. Runs at app start. Meal rows are changed without a sync version
 * bump, because photos are not part of the Health Connect record.
 */
class ImageRetentionCleaner @Inject constructor(
    private val dirs: ImageDirectories,
    private val db: SnackshotDatabase,
    private val pending: PendingMealStore,
    private val clock: Clock,
) {
    suspend fun run(): RetentionReport = withContext(Dispatchers.IO) {
        val now = clock.instant()
        val meals = db.meals()
        val mealRefs = meals.imageRefs().toSet()
        val pendingRef = pending.pending.first()?.imageRef

        var expired = 0
        var orphans = 0
        var cleared = 0
        val present = mutableSetOf<String>()

        dirs.processed.listFiles().orEmpty().filter { it.isFile }.forEach { file ->
            val storedAt = Instant.ofEpochMilli(file.lastModified())
            val name = file.name
            if (name.endsWith(".tmp")) {
                // An interrupted write; never referenced.
                if (ImageRetention.isExpired(storedAt, now, ImageRetention.ORPHAN) && file.delete()) orphans++
                return@forEach
            }
            val ref = FileMealImageStore.refFor(name)
            when {
                ImageRetention.isExpired(storedAt, now, ImageRetention.PROCESSED) -> if (file.delete()) expired++
                ref !in mealRefs && ref != pendingRef && ImageRetention.isExpired(storedAt, now, ImageRetention.ORPHAN) ->
                    if (file.delete()) orphans++
                else -> present += ref
            }
        }
        // Meals whose photo is gone (expired now, or deleted earlier) show the placeholder.
        (mealRefs - present).forEach { ref -> cleared += meals.clearImageRef(ref) }

        val captures = dirs.captures.listFiles().orEmpty().count { file ->
            file.isFile && ImageRetention.isExpired(Instant.ofEpochMilli(file.lastModified()), now, ImageRetention.CAPTURE) && file.delete()
        }
        RetentionReport(expiredDeleted = expired, orphansDeleted = orphans, capturesDeleted = captures, mealRefsCleared = cleared)
    }
}
