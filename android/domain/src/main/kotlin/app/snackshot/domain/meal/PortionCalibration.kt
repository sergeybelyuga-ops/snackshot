package app.snackshot.domain.meal

import java.time.Instant
import java.time.ZoneOffset

/** Where the starting grams of a review come from (WBS 6.5). */
enum class PortionSource {
    /** LogMeal's own portion. */
    PROVIDER,

    /** The grams the user saved the last time for the same food. */
    LAST_LOGGED,

    /** A reference average weight for the food. */
    REFERENCE,

    /** Nothing better was known. */
    DEFAULT,
}

data class CalibratedPortion(val grams: Double, val source: PortionSource) {
    init {
        require(grams.isFinite() && grams in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS) { "Calibrated grams out of range: $grams" }
    }
}

/** WBS 6.5: the grams a fresh recognition starts from, before the user corrects them. */
fun interface PortionCalibrator {
    suspend fun portionFor(recognition: Recognition): CalibratedPortion

    companion object {
        /** When neither LogMeal nor anything else gives a portion. */
        const val DEFAULT_GRAMS = 100.0

        /** LogMeal's portion, or [DEFAULT_GRAMS]. */
        val PROVIDER_ONLY = PortionCalibrator { r ->
            r.grams?.takeIf { it in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS }
                ?.let { CalibratedPortion(it, PortionSource.PROVIDER) }
                ?: CalibratedPortion(DEFAULT_GRAMS, PortionSource.DEFAULT)
        }
    }
}

/**
 * The S-15 review draft for a recognition. LogMeal's values are for its own portion, so they are
 * rescaled to the calibrated grams; that becomes the estimate the user's corrections are measured
 * against (Zero-Edit, doc 02 §4.4). Without LogMeal grams, its values are taken as they are.
 */
fun MealDraft.Companion.fromRecognition(
    source: MealSource,
    recognition: Recognition,
    portion: CalibratedPortion,
    eatenAt: Instant,
    zoneOffset: ZoneOffset,
    mealType: MealType,
    imageRef: String,
): MealDraft {
    val providerGrams = recognition.grams
    val values = if (providerGrams != null) recognition.values.scaled(portion.grams / providerGrams) else recognition.values
    return fromEstimate(
        source = source,
        // LogMeal names are lowercase ("apple"); the meal starts with a capital, as typed names do.
        estimate = NutritionEstimate(
            recognition.foodName.replaceFirstChar {
                it.uppercaseChar()
            },
            portion.grams,
            values,
            recognition.confidence,
        ),
        eatenAt = eatenAt,
        zoneOffset = zoneOffset,
        mealType = mealType,
        imageRef = imageRef,
    )
}
