package app.snackshot.domain.meal

import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException

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
 * WBS 6.5, in this order:
 * 1. the grams the user saved last time for the same food, since people tend to eat the same
 *    portion again and the user has already corrected that one;
 * 2. LogMeal's grams when the recognition is confident ([NutritionEstimate.HIGH_CONFIDENCE]);
 * 3. the reference weight of a single food ([ReferencePortions]);
 * 4. LogMeal's grams even when the recognition is uncertain;
 * 5. [PortionCalibrator.DEFAULT_GRAMS].
 *
 * Grams outside the review's 1–5,000 g are skipped. History is only a hint, so a failed lookup
 * falls through to the next source and never fails the recognition.
 */
class HistoryPortionCalibrator(
    private val lastGrams: suspend (foodName: String) -> Double?,
    private val referenceGrams: (foodName: String) -> Double? = ReferencePortions::gramsFor,
) : PortionCalibrator {
    constructor(meals: MealRepository) : this(meals::lastGramsFor)

    override suspend fun portionFor(recognition: Recognition): CalibratedPortion {
        val name = recognition.foodName
        val history = try {
            lastGrams(name)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val provider = recognition.grams
        return history.portion(PortionSource.LAST_LOGGED)
            ?: provider.takeIf { recognition.confidence >= NutritionEstimate.HIGH_CONFIDENCE }.portion(PortionSource.PROVIDER)
            ?: referenceGrams(name).portion(PortionSource.REFERENCE)
            ?: provider.portion(PortionSource.PROVIDER)
            ?: CalibratedPortion(PortionCalibrator.DEFAULT_GRAMS, PortionSource.DEFAULT)
    }

    private fun Double?.portion(source: PortionSource): CalibratedPortion? =
        this?.takeIf { it.isFinite() && it in MealDraft.MIN_GRAMS..MealDraft.MAX_GRAMS }?.let { CalibratedPortion(it, source) }
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
