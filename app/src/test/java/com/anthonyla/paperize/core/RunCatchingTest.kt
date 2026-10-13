package com.anthonyla.paperize.core

import kotlinx.coroutines.CancellationException
import org.junit.Test

class RunCatchingTest {
    @Test(expected = CancellationException::class)
    fun `cancellation propagates instead of becoming a failure`() {
        runCatchingCancellable { throw CancellationException() }
    }
}
