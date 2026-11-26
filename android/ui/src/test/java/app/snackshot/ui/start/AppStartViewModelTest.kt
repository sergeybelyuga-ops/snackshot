package app.snackshot.ui.start

import app.snackshot.domain.onboarding.OnboardingRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeOnboarding(initial: Boolean, var failReads: Int = 0) : OnboardingRepository {
        val stored = MutableStateFlow(initial)
        override val welcomeCompleted: Flow<Boolean> = flow {
            if (failReads > 0) {
                failReads--
                throw IOException("disk")
            }
            emit(stored.value)
        }

        override suspend fun completeWelcome() {
            stored.value = true
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `first run starts on Welcome`() = runTest(dispatcher) {
        val vm = AppStartViewModel(FakeOnboarding(initial = false))
        assertEquals(StartState.Loading, vm.state.value)
        advanceUntilIdle()
        assertEquals(StartState.Ready(welcomeCompleted = false), vm.state.value)
    }

    @Test
    fun `returning user starts on Home`() = runTest(dispatcher) {
        val vm = AppStartViewModel(FakeOnboarding(initial = true))
        advanceUntilIdle()
        assertEquals(StartState.Ready(welcomeCompleted = true), vm.state.value)
    }

    @Test
    fun `completing welcome is stored but does not move the start destination`() = runTest(dispatcher) {
        val repo = FakeOnboarding(initial = false)
        val vm = AppStartViewModel(repo)
        advanceUntilIdle()
        vm.completeWelcome()
        advanceUntilIdle()
        assertEquals(true, repo.stored.value)
        assertEquals(StartState.Ready(welcomeCompleted = false), vm.state.value)
    }

    @Test
    fun `storage error offers retry`() = runTest(dispatcher) {
        val vm = AppStartViewModel(FakeOnboarding(initial = true, failReads = 1))
        advanceUntilIdle()
        assertEquals(StartState.StorageError, vm.state.value)
        vm.retry()
        advanceUntilIdle()
        assertEquals(StartState.Ready(welcomeCompleted = true), vm.state.value)
    }
}
