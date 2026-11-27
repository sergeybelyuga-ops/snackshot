package app.snackshot.domain.meal

import app.snackshot.domain.nutrition.NutritionValues
import java.time.Duration

/** Where a recognition lives at the provider, for a later grams correction (LogMeal `confirm/quantity`). */
data class RecognitionRef(val imageId: Long, val itemPosition: Int) {
    init {
        require(itemPosition >= 1) { "LogMeal item positions start at 1: $itemPosition" }
    }
}

/**
 * The provider's answer for one photo, before portion calibration (WBS 6.5). One dish or product
 * per photo (F-01): when the provider splits the photo into several items, only the most likely
 * one is kept.
 *
 * @property grams the provider's portion; null when it gave none. [values] are for this portion.
 * @property otherItems how many more items the provider saw in the photo and that were dropped.
 * @property dailyRemaining recognitions left today for this APIUser, when the provider said.
 */
data class Recognition(
    val foodName: String,
    val confidence: Double,
    val grams: Double?,
    val values: NutritionValues,
    val ref: RecognitionRef,
    val otherItems: Int = 0,
    val dailyRemaining: Int? = null,
) {
    init {
        require(foodName.isNotBlank()) { "A recognition has a food name" }
        require(confidence in 0.0..1.0) { "Confidence must be within 0..1: $confidence" }
        require(grams == null || (grams.isFinite() && grams > 0.0)) { "Recognized grams must be positive: $grams" }
        require(otherItems >= 0) { "otherItems must not be negative" }
    }
}

/** Why a photo was not recognized. Every case keeps the draft and offers Quick add (S-05). */
enum class RecognitionFailure {
    /** No connection, or the provider didn't answer in time, also after one retry. */
    OFFLINE,

    /** No LogMeal token, or LogMeal rejected it again after the broker was asked once (401, 403/116). */
    NO_ACCESS,

    /** LogMeal's daily limit for this APIUser (HTTP 429). */
    DAILY_LIMIT,

    /** LogMeal refused the image (format, size). */
    INVALID_IMAGE,

    /** The photo was read, but no food was found in it. */
    NO_FOOD,

    /** Anything else: a server error after one retry, or an answer that couldn't be read. */
    SERVICE,
}

sealed interface RecognitionOutcome {
    data class Success(val recognition: Recognition) : RecognitionOutcome

    /** [retryAfter] is when the daily limit resets, if the provider said. */
    data class Failure(val reason: RecognitionFailure, val retryAfter: Duration? = null) : RecognitionOutcome
}

/** Food recognition for one processed photo (WBS 6.3). */
interface FoodRecognizer {
    /** Recognizes one processed JPEG (WBS 6.2). Never throws for network or provider errors. */
    suspend fun recognize(jpeg: ByteArray): RecognitionOutcome

    /**
     * Tells the provider the portion the user corrected, so its own records match. Optional: values
     * are rescaled locally either way. False when it couldn't be sent.
     */
    suspend fun confirmQuantity(ref: RecognitionRef, grams: Double): Boolean
}
