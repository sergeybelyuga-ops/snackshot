package app.snackshot.ui.capture

import androidx.lifecycle.SavedStateHandle
import app.snackshot.domain.meal.FoodRecognizer
import app.snackshot.domain.meal.InvalidPhoto
import app.snackshot.domain.meal.MealSource
import app.snackshot.domain.meal.MealType
import app.snackshot.domain.meal.PendingMeal
import app.snackshot.domain.meal.PhotoPreprocessor
import app.snackshot.domain.meal.PhotoSource
import app.snackshot.domain.meal.PortionCalibrator
import app.snackshot.domain.meal.PreprocessResult
import app.snackshot.domain.meal.Recognition
import app.snackshot.domain.meal.RecognitionFailure
import app.snackshot.domain.meal.RecognitionOutcome
import app.snackshot.domain.meal.RecognitionRef
import app.snackshot.domain.nutrition.NutritionValues
import app.snackshot.ui.FakeAccess
import app.snackshot.ui.FakeImages
import app.snackshot.ui.FakePendingMeals
import app.snackshot.ui.MutableClock
import app.snackshot.ui.TEST_NOW
import app.snackshot.ui.TEST_ZONE
import app.snackshot.ui.meal.PictureReporter
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RecognitionViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock()
    private val pending = FakePendingMeals()
    private val access = FakeAccess()
    private val images by lazy { FakeImages(tmp.root) }
    private val recognizer = FakeRecognizer()
    private val preprocessor by lazy { FakePreprocessor(images) }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun capture() = tmp.newFile("raw.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    private fun TestScope.viewModel(handle: SavedStateHandle) = RecognitionViewModel(
        handle = handle,
        preprocessor = preprocessor,
        recognizer = recognizer,
        calibrator = PortionCalibrator.PROVIDER_ONLY,
        pending = pending,
        images = images,
        reporter = PictureReporter(access, this),
        clock = clock,
        zone = { TEST_ZONE },
    )

    private fun photoHandle() = SavedStateHandle(mapOf("source" to "PHOTO", "capturePath" to capture().path))

    private val apple = Recognition(
        foodName = "apple",
        confidence = 0.73,
        grams = 182.0,
        values = NutritionValues(94.6, 0.5, 25.1, 0.3),
        ref = RecognitionRef(1000001, 1),
    )

    @Test
    fun `a recognized photo becomes the review draft and is reported once`() = runTest(dispatcher) {
        recognizer.outcomes += RecognitionOutcome.Success(apple)

        val vm = viewModel(photoHandle())
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.recognized)
        assertEquals(StepState.DONE, s.check)
        val draft = (pending.state.value as PendingMeal.Reviewing).draft
        assertEquals("Apple", draft.name)
        assertEquals(182.0, draft.grams)
        assertEquals(MealSource.PHOTO, draft.source)
        assertEquals(TEST_NOW, draft.eatenAt)
        // 13:00 in Belgrade.
        assertEquals(MealType.LUNCH, draft.mealType)
        assertEquals(s.imageRef, draft.imageRef)
        assertEquals(1, access.reported.size)
    }

    @Test
    fun `a network failure offers one retry, and a second failure offers Quick add`() = runTest(dispatcher) {
        recognizer.outcomes += RecognitionOutcome.Failure(RecognitionFailure.OFFLINE)
        recognizer.outcomes += RecognitionOutcome.Failure(RecognitionFailure.OFFLINE)

        val vm = viewModel(photoHandle())
        advanceUntilIdle()
        assertEquals(RecognitionFailure.OFFLINE, vm.state.value.failure)
        assertTrue(vm.state.value.canRetry)
        val awaiting = pending.state.value as PendingMeal.AwaitingRecognition
        assertEquals(1, awaiting.failedAttempts)

        vm.retry()
        vm.state.first { it.failure != null && !it.retrying }

        assertFalse(vm.state.value.canRetry)
        assertEquals(2, (pending.state.value as PendingMeal.AwaitingRecognition).failedAttempts)
        assertEquals(2, recognizer.calls)
        assertTrue("nothing is reported for failures", access.reported.isEmpty())
    }

    @Test
    fun `the daily limit is not retried`() = runTest(dispatcher) {
        recognizer.outcomes += RecognitionOutcome.Failure(RecognitionFailure.DAILY_LIMIT, Duration.ofHours(5))

        val vm = viewModel(photoHandle())
        advanceUntilIdle()

        assertEquals(RecognitionFailure.DAILY_LIMIT, vm.state.value.failure)
        assertEquals(Duration.ofHours(5), vm.state.value.retryAfter)
        assertFalse(vm.state.value.canRetry)
        vm.retry()
        advanceUntilIdle()
        assertEquals(1, recognizer.calls)
    }

    @Test
    fun `no food in the photo asks for another one`() = runTest(dispatcher) {
        recognizer.outcomes += RecognitionOutcome.Failure(RecognitionFailure.NO_FOOD)

        val vm = viewModel(photoHandle())
        advanceUntilIdle()

        assertEquals(Unusable.NO_FOOD, vm.state.value.unusable)
        assertNull(vm.state.value.failure)
    }

    @Test
    fun `an unusable photo is never sent`() = runTest(dispatcher) {
        preprocessor.invalid = InvalidPhoto.TOO_SMALL

        val vm = viewModel(photoHandle())
        advanceUntilIdle()

        assertEquals(Unusable.TOO_SMALL, vm.state.value.unusable)
        assertEquals(StepState.FAILED, vm.state.value.check)
        assertEquals(0, recognizer.calls)
        assertNull(pending.state.value)
    }

    @Test
    fun `after process death the pending photo is recognized again from storage`() = runTest(dispatcher) {
        val ref = images.saveProcessed(byteArrayOf(-1, -40, 1))
        pending.save(PendingMeal.AwaitingRecognition(MealSource.GALLERY, ref, TEST_NOW, TEST_ZONE.rules.getOffset(TEST_NOW)))
        recognizer.outcomes += RecognitionOutcome.Success(apple)

        // The restored route still names the picker item, but the photo was already processed.
        val vm = viewModel(SavedStateHandle(mapOf("source" to "GALLERY", "pickedUri" to "content://picked/1", "processed" to true)))
        vm.state.first { it.recognized }

        assertEquals(0, preprocessor.calls)
        assertEquals(MealSource.GALLERY, (pending.state.value as PendingMeal.Reviewing).draft.source)
    }

    @Test
    fun `returning to a failed draft shows the failure without spending another recognition`() = runTest(dispatcher) {
        val ref = images.saveProcessed(byteArrayOf(-1, -40, 1))
        pending.save(
            PendingMeal.AwaitingRecognition(
                MealSource.PHOTO,
                ref,
                TEST_NOW,
                TEST_ZONE.rules.getOffset(TEST_NOW),
                failedAttempts = 1,
                lastFailure = RecognitionFailure.SERVICE,
            ),
        )

        val vm = viewModel(SavedStateHandle(mapOf("source" to "PHOTO")))
        vm.state.first { it.failure != null }

        assertEquals(RecognitionFailure.SERVICE, vm.state.value.failure)
        assertTrue(vm.state.value.canRetry)
        assertEquals(0, recognizer.calls)
    }

    @Test
    fun `with nothing pending the screen closes`() = runTest(dispatcher) {
        val vm = viewModel(SavedStateHandle(mapOf("source" to "PHOTO")))
        advanceUntilIdle()

        assertTrue(vm.state.value.closed)
    }

    private class FakeRecognizer : FoodRecognizer {
        val outcomes = ArrayDeque<RecognitionOutcome>()
        var calls = 0

        override suspend fun recognize(jpeg: ByteArray): RecognitionOutcome {
            calls++
            return outcomes.removeFirst()
        }

        override suspend fun confirmQuantity(ref: RecognitionRef, grams: Double) = true
    }

    private class FakePreprocessor(private val images: FakeImages) : PhotoPreprocessor {
        var invalid: InvalidPhoto? = null
        var calls = 0

        override suspend fun process(source: PhotoSource): PreprocessResult {
            calls++
            if (source is PhotoSource.Capture) images.deleteCapture(source.file)
            invalid?.let { return PreprocessResult.Invalid(it) }
            val jpeg = byteArrayOf(-1, -40, 7, 7)
            return PreprocessResult.Processed(images.saveProcessed(jpeg), jpeg, 1000, 750, 90)
        }
    }
}
