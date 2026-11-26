package app.snackshot.domain.metrics

import java.time.LocalDate

/** Daily counters of the product metrics payload (WBS §6.2, §6.3). The key is the payload field. */
enum class MetricCounter(val key: String) {
    PHOTO_RECOGNITIONS("photoRecognitions"),
    RECOGNITION_FAILURES("recognitionFailures"),
    QUICK_ADDS("quickAdds"),
    HIGH_CONFIDENCE("highConfidence"),
    SAVED_ZERO_EDIT("savedZeroEdit"),
    SAVED_GRAMS_EDITED("savedGramsEdited"),
    SAVED_NAME_CHANGED("savedNameChanged"),
    MEALS_LOGGED("mealsLogged"),
}

/** Measurements reported as p50 / p95 in the daily payload. */
enum class MetricSample(val key: String) {
    RECOGNITION_LATENCY_MS("recognitionLatencyMs"),
    TIME_TO_LOG_SEC("timeToLogSec"),
}

/** Everything recorded for one local day, before it is sent as one aggregate. */
data class DailyMetrics(val day: LocalDate, val counters: Map<MetricCounter, Long>, val samples: Map<MetricSample, List<Long>>)

/**
 * Local storage for anonymous product metrics. Holds only counts and durations: no food names,
 * images, nutrition values, or identifiers (WBS §6).
 */
interface MetricsStore {
    suspend fun increment(day: LocalDate, counter: MetricCounter, by: Long = 1)

    suspend fun addSample(day: LocalDate, sample: MetricSample, value: Long)

    /** Days that have data and are before [today], oldest first: ready to report. */
    suspend fun daysBefore(today: LocalDate): List<LocalDate>

    suspend fun dailyMetrics(day: LocalDate): DailyMetrics

    /** Removes a day after it was reported, or all data when the user switches metrics off. */
    suspend fun deleteDay(day: LocalDate)

    suspend fun deleteAll()
}
