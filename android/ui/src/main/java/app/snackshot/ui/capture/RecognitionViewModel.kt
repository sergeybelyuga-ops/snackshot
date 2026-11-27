package app.snackshot.ui.capture

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snackshot.domain.meal.FoodRecognizer
import app.snackshot.domain.meal.InvalidPhoto
import app.snackshot.domain.meal.MealDraft
import app.snackshot.domain.meal.MealImageStore
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PendingMealStore
import app.snackshot.domain.meal.PhotoPreprocessor
import app.snackshot.domain.meal.PhotoSource
import app.snackshot.domain.meal.PortionCalibrator
import app.snackshot.domain.meal.PreprocessResult
import app.snackshot.domain.meal.RecognitionFailure
import app.snackshot.domain.meal.RecognitionOutcome
import app.snackshot.domain.meal.fromRecognition
import app.snackshot.ui.meal.PictureReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One row of S-05: the image check, then LogMeal recognition. */
enum class StepState { WAITING, RUNNING, DONE, FAILED }

/** What S-05 shows. */
data class RecognitionState(
    val imageRef: String? = null,
    /** "1000 px · JPG · 412 KB" once the image is checked. */
    val imageInfo: ImageInfo? = null,
    val check: StepState = StepState.RUNNING,
    val recognition: StepState = StepState.WAITING,
    /** The one manual retry is running. */
    val retrying: Boolean = false,
    /** Recognition has run past the expected window (doc 10 §5.4: p98 < 3 s). */
    val slow: Boolean = false,
    val failure: RecognitionFailure? = null,
    /** When LogMeal's daily limit resets. */
    val retryAfter: Duration? = null,
    /** The single retry is still available (doc 10 §4.6). */
    val canRetry: Boolean = false,
    /** The photo can't be used: nothing is sent, or nothing was found. */
    val unusable: Unusable? = null,
    /** Recognized: the draft is saved and S-15 opens. */
    val recognized: Boolean = false,
    /** Nothing is pending (for example, the draft was discarded elsewhere): S-05 closes. */
    val closed: Boolean = false,
)

data class ImageInfo(val longSidePx: Int, val kilobytes: Int)

enum class Unusable { UNREADABLE, TOO_SMALL, TOO_LARGE, NO_FOOD }

/**
 * S-05 Recognition progress (doc 10 §4.6, WBS 6.4). Preprocesses the photo (6.2), keeps it as a
 * pending draft that survives process death, recognizes it (6.3), and opens the review with the
 * result. A failure offers one retry; after that, or when a retry can't help, Quick add keeps
 * the photo and time.
 */
@HiltViewModel
class RecognitionViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val preprocessor: PhotoPreprocessor,
    private val recognizer: FoodRecognizer,
    private val calibrator: PortionCalibrator,
    private val pending: PendingMealStore,
    private val images: MealImageStore,
    private val reporter: PictureReporter,
    private val clock: Clock,
    private val zone: Provider<ZoneId>,
) : ViewModel() {
    private val _state = MutableStateFlow(RecognitionState())
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        job = viewModelScope.launch { start() }
    }

    /** The processed photo behind [RecognitionState.imageRef]. */
    fun fileFor(ref: String): File? = images.fileFor(ref)

    /** Retake or another photo: the pending draft and its photo are dropped first. */
    fun discard(then: () -> Unit) {
        job?.cancel()
        viewModelScope.launch {
            pending.discard()
            then()
        }
    }

    /** The single manual retry. */
    fun retry() {
        val s = _state.value
        if (!s.canRetry || job?.isActive == true) return
        _state.update { it.copy(retrying = true, canRetry = false, failure = null, recognition = StepState.RUNNING, slow = false) }
        job = viewModelScope.launch { resume(manualRetry = true) }
    }

    private suspend fun start() {
        val source = handle.get<String>(ARG_SOURCE)?.let(MealSource::valueOf) ?: MealSource.PHOTO
        val capture = handle.get<String>(ARG_CAPTURE)?.let(::File)
        val picked = handle.get<String>(ARG_PICKED)
        // After process death the route is restored with the same arguments; the capture is gone by then.
        val fresh = handle.get<Boolean>(KEY_PROCESSED) != true && (capture?.exists() == true || picked != null)
        if (!fresh) return resume(manualRetry = false)

        val photo = if (capture != null) PhotoSource.Capture(capture) else PhotoSource.Picked(picked!!)
        val capturedAt = clock.instant()
        when (val result = preprocessor.process(photo)) {
            is PreprocessResult.Invalid -> _state.update {
                it.copy(check = StepState.FAILED, unusable = result.reason.toUnusable())
            }
            is PreprocessResult.Processed -> {
                val offset = zone.get().rules.getOffset(capturedAt)
                pending.save(PendingMeal.AwaitingRecognition(source, result.imageRef, capturedAt, offset))
                handle[KEY_PROCESSED] = true
                latency("preprocessing: ${result.millis} ms, ${result.jpeg.size / 1024} KB")
                _state.update {
                    it.copy(
                        imageRef = result.imageRef,
                        imageInfo = ImageInfo(maxOf(result.width, result.height), result.jpeg.size / 1024),
                        check = StepState.DONE,
                    )
                }
                recognize(source, result.imageRef, result.jpeg, failedBefore = 0)
            }
        }
    }

    /** Continues the pending draft: recognizes it again, or opens its review if it already has a result. */
    private suspend fun resume(manualRetry: Boolean) {
        when (val meal = pending.pending.first()) {
            is PendingMeal.Reviewing -> _state.update { it.copy(recognized = true) }
            is PendingMeal.AwaitingRecognition -> {
                val jpeg = withContext(Dispatchers.IO) { images.fileFor(meal.imageRef)?.readBytes() }
                if (jpeg == null) {
                    // The photo is gone (deleted or cleaned up): nothing left to recognize.
                    pending.discard()
                    _state.update { it.copy(check = StepState.FAILED, unusable = Unusable.UNREADABLE) }
                    return
                }
                _state.update { it.copy(imageRef = meal.imageRef, check = StepState.DONE) }
                val lastFailure = meal.lastFailure
                if (!manualRetry && meal.failedAttempts > 0 && lastFailure != null) {
                    // Back on S-05 after a failure: show it again; the user decides whether to retry.
                    _state.update {
                        it.copy(
                            recognition = StepState.FAILED,
                            failure = lastFailure,
                            canRetry = meal.failedAttempts == 1 && lastFailure.retryHelps,
                        )
                    }
                    return
                }
                recognize(meal.source, meal.imageRef, jpeg, failedBefore = meal.failedAttempts)
            }
            null -> _state.update { it.copy(closed = true) }
        }
    }

    private suspend fun recognize(source: MealSource, imageRef: String, jpeg: ByteArray, failedBefore: Int) {
        _state.update { it.copy(recognition = StepState.RUNNING) }
        val slowTimer = viewModelScope.launch {
            delay(SLOW_AFTER_MS)
            _state.update { if (it.recognition == StepState.RUNNING) it.copy(slow = true) else it }
        }
        val started = System.nanoTime()
        val outcome = try {
            recognizer.recognize(jpeg)
        } finally {
            slowTimer.cancel()
        }
        latency("recognition: ${(System.nanoTime() - started) / 1_000_000} ms, ${outcome.javaClass.simpleName}")

        val meal = pending.pending.first() as? PendingMeal.AwaitingRecognition
        if (meal == null || meal.imageRef != imageRef) {
            // Discarded meanwhile (Quick add saved it, or the user left with Discard).
            _state.update { it.copy(closed = true) }
            return
        }
        when (outcome) {
            is RecognitionOutcome.Success -> {
                val r = outcome.recognition
                val local = meal.capturedAt.atZone(zone.get()).toLocalTime()
                val portion = calibrator.portionFor(r)
                latency("portion: ${portion.source}")
                val draft = MealDraft.fromRecognition(
                    source = source,
                    recognition = r,
                    portion = portion,
                    eatenAt = meal.capturedAt,
                    zoneOffset = meal.zoneOffset,
                    mealType = MealType.suggestFor(local),
                    imageRef = imageRef,
                )
                pending.save(PendingMeal.Reviewing(draft))
                reporter.report()
                _state.update { it.copy(recognition = StepState.DONE, retrying = false, slow = false, recognized = true) }
            }
            is RecognitionOutcome.Failure -> {
                if (outcome.reason == RecognitionFailure.NO_FOOD) {
                    _state.update { it.copy(recognition = StepState.FAILED, retrying = false, slow = false, unusable = Unusable.NO_FOOD) }
                    return
                }
                val attempts = failedBefore + 1
                pending.save(meal.copy(failedAttempts = attempts, lastFailure = outcome.reason))
                _state.update {
                    it.copy(
                        recognition = StepState.FAILED,
                        retrying = false,
                        slow = false,
                        failure = outcome.reason,
                        retryAfter = outcome.retryAfter,
                        canRetry = attempts == 1 && outcome.reason.retryHelps,
                    )
                }
            }
        }
    }

    /**
     * Latency for the I-4 measurement on a physical device (`adb logcat -s Snackshot`): durations and
     * outcome and where the portion came from, never the photo, the food or a token. Plain JVM tests have no Log.
     */
    private fun latency(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private val RecognitionFailure.retryHelps: Boolean
        get() = this == RecognitionFailure.OFFLINE || this == RecognitionFailure.SERVICE

    private fun InvalidPhoto.toUnusable() = when (this) {
        InvalidPhoto.UNREADABLE -> Unusable.UNREADABLE
        InvalidPhoto.TOO_SMALL -> Unusable.TOO_SMALL
        InvalidPhoto.TOO_LARGE -> Unusable.TOO_LARGE
    }

    companion object {
        const val ARG_SOURCE = "source"
        const val ARG_CAPTURE = "capturePath"
        const val ARG_PICKED = "pickedUri"
        private const val KEY_PROCESSED = "processed"
        private const val SLOW_AFTER_MS = 3_000L
        private const val TAG = "Snackshot"
    }
}
