package com.anthonyla.paperize.core

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class ResultTest {
    @Test
    fun `captured failure retains the original exception when unwrapped`() {
        val failure = IllegalStateException("boom")
        val result = Result.runCatching { throw failure }
        assertSame(failure, assertThrows(IllegalStateException::class.java) { result.getOrThrow() })
    }

    @Test(expected = CancellationException::class)
    fun `runCatching propagates cancellation`() {
        Result.runCatching { throw CancellationException() }
    }
}
