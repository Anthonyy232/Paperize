package com.anthonyla.paperize.core

import kotlinx.coroutines.CancellationException

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = exception.message) : Result<Nothing>()

    fun getOrNull(): T? = if (this is Success) data else null

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }

    companion object {
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}
