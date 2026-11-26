package app.snackshot.domain.meal

import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Meal photos on the device (doc 02 §8.4). Only the processed copy is kept; the raw camera
 * capture lives in a temporary file until preprocessing succeeds. Gallery originals are never
 * copied or touched.
 */
interface MealImageStore {
    /** A new empty file for the camera to write the raw capture to (cache, never backed up). */
    suspend fun newCaptureFile(): File

    /** Deletes a raw capture. Call as soon as preprocessing succeeds, or when the flow is cancelled. */
    suspend fun deleteCapture(file: File)

    /** Stores the processed JPEG and returns its reference for [MealEntry.imageRef]. */
    suspend fun saveProcessed(jpeg: ByteArray): String

    /** The file behind [ref] for display, or null when it was deleted (retention, user) or [ref] is invalid. */
    fun fileFor(ref: String): File?

    suspend fun delete(ref: String)
}

/** The retention rules applied by the cleanup at app start (doc 02 §8.4). */
object ImageRetention {
    /** Processed photos are kept this long, then deleted; the meal keeps its values without a photo. */
    val PROCESSED: Duration = Duration.ofDays(365)

    /** A processed photo no meal or draft refers to (a discarded flow that was not cleaned up). */
    val ORPHAN: Duration = Duration.ofDays(1)

    /** A raw capture left behind by a crash or a killed process. */
    val CAPTURE: Duration = Duration.ofDays(1)

    fun isExpired(storedAt: Instant, now: Instant, keep: Duration): Boolean = storedAt.plus(keep) < now
}

/** Result of one retention run, for logs and tests. Counts only; no file names. */
data class RetentionReport(val expiredDeleted: Int, val orphansDeleted: Int, val capturesDeleted: Int, val mealRefsCleared: Int)
