package app.snackshot.logmeal

import app.snackshot.domain.access.LogMealCredentialProvider
import app.snackshot.domain.meal.FoodRecognizer
import app.snackshot.domain.meal.Recognition
import app.snackshot.domain.meal.RecognitionFailure
import app.snackshot.domain.meal.RecognitionOutcome
import app.snackshot.domain.meal.RecognitionRef
import app.snackshot.domain.nutrition.NutritionValues
import java.io.IOException
import java.time.Duration
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import okhttp3.Headers

/**
 * LogMeal recognition (WBS 6.3): `segmentation/complete` → `nutritionalInfo`, mapped to one
 * [Recognition] per photo.
 *
 * Every call is retried once after a network or server error, and after LogMeal rejects the token
 * (401, 403/116) the broker is asked once for a new one ([LogMealCredentialProvider]). The token
 * never appears in results, exceptions or logs.
 */
class LogMealRecognizer(private val api: LogMealApi, private val credentials: LogMealCredentialProvider) : FoodRecognizer {

    override suspend fun recognize(jpeg: ByteArray): RecognitionOutcome {
        val session = Session(credentials.token() ?: return RecognitionOutcome.Failure(RecognitionFailure.NO_ACCESS))

        val segmented = when (val step = session.call { api.segment(it, jpeg) }) {
            is Step.Failed -> return step.outcome(clientError = RecognitionFailure.INVALID_IMAGE)
            is Step.Ok -> step
        }
        val segmentation = decode(SegmentationDto.serializer(), segmented.body)
            ?: return RecognitionOutcome.Failure(RecognitionFailure.SERVICE)
        val ranked = segmentation.items
            .mapNotNull { item -> item.candidates.maxByOrNull { it.prob }?.takeIf { it.name.isNotBlank() }?.let { item to it } }
            .sortedByDescending { (_, best) -> best.prob }
        // One dish or product per photo (F-01): the most likely item; the rest are dropped.
        val (item, best) = ranked.firstOrNull() ?: return RecognitionOutcome.Failure(RecognitionFailure.NO_FOOD)

        // A failed nutrition call still returns the recognized food: the review asks for the values (doc 10 §4.7).
        val nutrition = (session.call { api.nutrition(it, segmentation.imageId) } as? Step.Ok)
            ?.let { decode(NutritionDto.serializer(), it.body) }
        val portion = nutrition?.portionFor(item.position, onlyItem = segmentation.items.size == 1)

        return RecognitionOutcome.Success(
            Recognition(
                foodName = best.name.trim(),
                confidence = best.prob.coerceIn(0.0, 1.0),
                grams = portion?.grams,
                values = portion?.values ?: NutritionValues.EMPTY,
                ref = RecognitionRef(segmentation.imageId, item.position),
                otherItems = ranked.size - 1,
                dailyRemaining = segmented.headers[HEADER_REMAINING]?.toIntOrNull(),
            ),
        )
    }

    override suspend fun confirmQuantity(ref: RecognitionRef, grams: Double): Boolean {
        require(grams.isFinite() && grams > 0.0) { "Grams must be positive: $grams" }
        val token = credentials.token() ?: return false
        return Session(token).call { api.confirmQuantity(it, ref.imageId, ref.itemPosition, grams) } is Step.Ok
    }

    private class Portion(val grams: Double?, val values: NutritionValues)

    /** The item's own portion; the photo totals only when LogMeal found a single item. */
    private fun NutritionDto.portionFor(position: Int, onlyItem: Boolean): Portion? {
        perItem.firstOrNull { it.position == position }?.let { item ->
            if (!item.hasNutritionalInfo) return null
            return Portion(item.servingSize.positiveOrNull(), item.info.values())
        }
        if (!onlyItem || !hasNutritionalInfo) return null
        return Portion(servingSize.positiveOrNull(), info.values())
    }

    private fun NutrientsDto?.values(): NutritionValues {
        if (this == null) return NutritionValues.EMPTY
        fun nutrient(key: String) = totalNutrients[key]?.quantity.validOrNull()
        return NutritionValues(
            energyKcal = nutrient(ENERGY) ?: calories.validOrNull(),
            proteinGrams = nutrient(PROTEIN),
            carbsGrams = nutrient(CARBS),
            fatGrams = nutrient(FAT),
        )
    }

    private fun Double?.validOrNull() = this?.takeIf { it.isFinite() && it >= 0.0 }

    private fun Double?.positiveOrNull() = this?.takeIf { it.isFinite() && it > 0.0 }

    private fun <T> decode(strategy: DeserializationStrategy<T>, body: String): T? = try {
        LogMealApi.json.decodeFromString(strategy, body)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private sealed interface Step {
        class Ok(val body: String, val headers: Headers) : Step

        class Failed(private val reason: RecognitionFailure?, private val retryAfter: Duration? = null) : Step {
            /** [reason], or [clientError] for a 4xx LogMeal didn't explain further. */
            fun outcome(clientError: RecognitionFailure) = RecognitionOutcome.Failure(reason ?: clientError, retryAfter)
        }
    }

    /** One recognition's token: refreshed through the broker at most once across all its calls. */
    private inner class Session(private var token: String) {
        private var refreshed = false

        suspend fun call(request: suspend (token: String) -> ApiResult): Step {
            var retried = false
            while (true) {
                val result = try {
                    request(token)
                } catch (e: IOException) {
                    if (retried) return Step.Failed(RecognitionFailure.OFFLINE)
                    retried = true
                    continue
                }
                when {
                    result is ApiResult.Ok -> return Step.Ok(result.body, result.headers)
                    result !is ApiResult.Error -> error("Unexpected result")
                    result.isTokenRejected -> {
                        if (refreshed) return Step.Failed(RecognitionFailure.NO_ACCESS)
                        refreshed = true
                        token = credentials.refreshAfterRejection() ?: return Step.Failed(RecognitionFailure.NO_ACCESS)
                    }
                    result.status == 403 -> return Step.Failed(RecognitionFailure.NO_ACCESS)
                    result.status == 429 -> return Step.Failed(RecognitionFailure.DAILY_LIMIT, result.headers.resetIn())
                    result.status >= 500 -> {
                        if (retried) return Step.Failed(RecognitionFailure.SERVICE)
                        retried = true
                    }
                    else -> return Step.Failed(null)
                }
            }
        }
    }

    private fun Headers.resetIn(): Duration? = get(HEADER_RESET)?.toLongOrNull()?.takeIf { it > 0 }?.let(Duration::ofSeconds)

    private companion object {
        const val ENERGY = "ENERC_KCAL"
        const val PROTEIN = "PROCNT"
        const val CARBS = "CHOCDF"
        const val FAT = "FAT"

        /** Rate-limit headers, sent only with `segmentation/complete` (spike §7). */
        const val HEADER_REMAINING = "ratelimit-remaining"
        const val HEADER_RESET = "ratelimit-reset"
    }
}
