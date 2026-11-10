package com.anthonyla.paperize.presentation

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Runs viewModelScope on a test dispatcher and stubs android.util.Log, which is absent on the JVM. */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTestRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
        unmockkAll()
    }
}
