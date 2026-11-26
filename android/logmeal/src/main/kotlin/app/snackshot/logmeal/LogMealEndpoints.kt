package app.snackshot.logmeal

/** LogMeal v2 paths used by Snackshot (spike results: docs/Architecture/13_spike-1.1-results.md). */
object LogMealEndpoints {
    const val SEGMENTATION_COMPLETE = "/v2/image/segmentation/complete"
    const val NUTRITIONAL_INFO = "/v2/nutrition/recipe/nutritionalInfo"
    const val CONFIRM_QUANTITY = "/v2/nutrition/confirm/quantity"
    const val LIMITATIONS = "/v2/info/limitations"
}
