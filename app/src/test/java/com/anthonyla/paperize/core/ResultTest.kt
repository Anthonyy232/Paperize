package com.anthonyla.paperize.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Result sealed class
 */
class ResultTest {

    // ============================================================
    // Test: Success
    // ============================================================

    @Test
    fun `Success isSuccess returns true`() {
        val result: Result<String> = Result.Success("data")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Success isError returns false`() {
        val result: Result<String> = Result.Success("data")
        assertFalse(result.isError)
    }

    @Test
    fun `Success isLoading returns false`() {
        val result: Result<String> = Result.Success("data")
        assertFalse(result.isLoading)
    }

    @Test
    fun `Success getOrNull returns data`() {
        val result: Result<String> = Result.Success("test data")
        assertEquals("test data", result.getOrNull())
    }

    @Test
    fun `Success exceptionOrNull returns null`() {
        val result: Result<String> = Result.Success("data")
        assertNull(result.exceptionOrNull())
    }

    // ============================================================
    // Test: Error
    // ============================================================

    @Test
    fun `Error isError returns true`() {
        val result: Result<String> = Result.Error(Exception("error"))
        assertTrue(result.isError)
    }

    @Test
    fun `Error isSuccess returns false`() {
        val result: Result<String> = Result.Error(Exception("error"))
        assertFalse(result.isSuccess)
    }

    @Test
    fun `Error isLoading returns false`() {
        val result: Result<String> = Result.Error(Exception("error"))
        assertFalse(result.isLoading)
    }

    @Test
    fun `Error getOrNull returns null`() {
        val result: Result<String> = Result.Error(Exception("error"))
        assertNull(result.getOrNull())
    }

    @Test
    fun `Error exceptionOrNull returns exception`() {
        val exception = Exception("test error")
        val result: Result<String> = Result.Error(exception)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `Error message defaults to exception message`() {
        val result = Result.Error(Exception("my error message"))
        assertEquals("my error message", result.message)
    }

    @Test
    fun `Error can have custom message`() {
        val result = Result.Error(Exception("original"), message = "custom message")
        assertEquals("custom message", result.message)
    }

    // ============================================================
    // Test: Loading
    // ============================================================

    @Test
    fun `Loading isLoading returns true`() {
        val result: Result<String> = Result.Loading
        assertTrue(result.isLoading)
    }

    @Test
    fun `Loading isSuccess returns false`() {
        val result: Result<String> = Result.Loading
        assertFalse(result.isSuccess)
    }

    @Test
    fun `Loading isError returns false`() {
        val result: Result<String> = Result.Loading
        assertFalse(result.isError)
    }

    @Test
    fun `Loading getOrNull returns null`() {
        val result: Result<String> = Result.Loading
        assertNull(result.getOrNull())
    }

    // ============================================================
    // Test: onSuccess callback
    // ============================================================

    @Test
    fun `onSuccess executes callback for Success`() {
        var callbackExecuted = false
        var receivedData: String? = null
        
        Result.Success("my data").onSuccess { 
            callbackExecuted = true
            receivedData = it
        }
        
        assertTrue(callbackExecuted)
        assertEquals("my data", receivedData)
    }

    @Test
    fun `onSuccess does not execute callback for Error`() {
        var callbackExecuted = false
        
        Result.Error(Exception("error")).onSuccess { 
            callbackExecuted = true
        }
        
        assertFalse(callbackExecuted)
    }

    @Test
    fun `onSuccess returns same result for chaining`() {
        val result = Result.Success("data")
        val returned = result.onSuccess { }
        assertSame(result, returned)
    }

    // ============================================================
    // Test: onError callback
    // ============================================================

    @Test
    fun `onError executes callback for Error`() {
        var callbackExecuted = false
        val exception = Exception("test error")
        
        Result.Error(exception).onError { 
            callbackExecuted = true
            assertEquals(exception, it)
        }
        
        assertTrue(callbackExecuted)
    }

    @Test
    fun `onError does not execute callback for Success`() {
        var callbackExecuted = false
        
        Result.Success("data").onError { 
            callbackExecuted = true
        }
        
        assertFalse(callbackExecuted)
    }

    @Test
    fun `onError returns same result for chaining`() {
        val result: Result<String> = Result.Error(Exception("error"))
        val returned = result.onError { }
        assertSame(result, returned)
    }

    // ============================================================
    // Test: map extension
    // ============================================================

    @Test
    fun `map transforms Success data`() {
        val result = Result.Success(5)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped.isSuccess)
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `map preserves Error`() {
        val exception = Exception("error")
        val result: Result<Int> = Result.Error(exception, "custom message")
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped.isError)
        assertEquals(exception, mapped.exceptionOrNull())
    }

    @Test
    fun `map preserves Loading`() {
        val result: Result<Int> = Result.Loading
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped.isLoading)
    }

    // ============================================================
    // Test: flatMap extension
    // ============================================================

    @Test
    fun `flatMap transforms Success to Success`() {
        val result = Result.Success(5)
        val flatMapped = result.flatMap { Result.Success(it.toString()) }
        
        assertTrue(flatMapped.isSuccess)
        assertEquals("5", flatMapped.getOrNull())
    }

    @Test
    fun `flatMap transforms Success to Error`() {
        val result = Result.Success(5)
        val flatMapped = result.flatMap { Result.Error(Exception("failed")) }
        
        assertTrue(flatMapped.isError)
    }

    @Test
    fun `flatMap preserves Error`() {
        val exception = Exception("original error")
        val result: Result<Int> = Result.Error(exception)
        val flatMapped = result.flatMap { Result.Success(it.toString()) }
        
        assertTrue(flatMapped.isError)
        assertEquals(exception, flatMapped.exceptionOrNull())
    }

    @Test
    fun `flatMap preserves Loading`() {
        val result: Result<Int> = Result.Loading
        val flatMapped = result.flatMap { Result.Success(it.toString()) }
        
        assertTrue(flatMapped.isLoading)
    }

    // ============================================================
    // Test: runCatching companion
    // ============================================================

    @Test
    fun `runCatching returns Success when block succeeds`() {
        val result = Result.runCatching { 42 }
        
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching returns Error when block throws`() {
        val result = Result.runCatching { throw IllegalStateException("boom") }
        
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
