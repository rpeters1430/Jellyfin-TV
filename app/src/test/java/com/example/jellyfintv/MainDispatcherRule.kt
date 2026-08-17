package com.example.jellyfintv

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps [Dispatchers.Main] for a test dispatcher so `viewModelScope.launch { }` (which runs on
 * `Dispatchers.Main.immediate`) can execute inside JVM unit tests, which have no real Android
 * main-thread looper. Required by every ViewModel test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    // Exposed (not private) so tests with real `delay()` calls (e.g. debounced search) can
    // drive virtual time via `testDispatcher.scheduler.advanceUntilIdle()` - this scheduler is
    // what Dispatchers.Main actually runs on, and it's independent of runTest's own scheduler.
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
