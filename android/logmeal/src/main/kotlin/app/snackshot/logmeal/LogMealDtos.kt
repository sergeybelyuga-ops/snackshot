package app.snackshot.logmeal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The parts of LogMeal's answers Snackshot reads (fixtures: test-fixtures/logmeal). Unknown keys are ignored.

@Serializable
internal data class ErrorDto(val code: Int? = null)

/** `segmentation/complete`: one entry per food item LogMeal found, each with its ranked candidates. */
@Serializable
internal data class SegmentationDto(val imageId: Long, @SerialName("segmentation_results") val items: List<SegmentItemDto> = emptyList())

@Serializable
internal data class SegmentItemDto(
    @SerialName("food_item_position") val position: Int,
    @SerialName("recognition_results") val candidates: List<CandidateDto> = emptyList(),
)

@Serializable
internal data class CandidateDto(val name: String, val prob: Double)

/** `nutritionalInfo`: totals for the photo, and per item keyed by `food_item_position`. */
@Serializable
internal data class NutritionDto(
    @SerialName("serving_size") val servingSize: Double? = null,
    val hasNutritionalInfo: Boolean = true,
    @SerialName("nutritional_info") val info: NutrientsDto? = null,
    @SerialName("nutritional_info_per_item") val perItem: List<ItemNutritionDto> = emptyList(),
)

@Serializable
internal data class ItemNutritionDto(
    @SerialName("food_item_position") val position: Int,
    @SerialName("serving_size") val servingSize: Double? = null,
    val hasNutritionalInfo: Boolean = true,
    @SerialName("nutritional_info") val info: NutrientsDto? = null,
)

@Serializable
internal data class NutrientsDto(val totalNutrients: Map<String, NutrientDto> = emptyMap(), val calories: Double? = null)

@Serializable
internal data class NutrientDto(val quantity: Double? = null, val unit: String? = null)
