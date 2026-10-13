package com.anthonyla.paperize.core

import kotlinx.coroutines.CancellationException

/** [runCatching] for suspend callers: coroutine cancellation propagates instead of becoming a failure. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
