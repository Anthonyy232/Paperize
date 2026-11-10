package com.anthonyla.paperize.core.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.util.UUID

fun Uri.isValid(contentResolver: ContentResolver): Boolean {
    if (scheme != "content") return false

    return try {
        contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    } catch (_: Exception) {
        // Fallback to opening the stream if the provider doesn't support the specific query
        try {
            contentResolver.openFileDescriptor(this, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
}

fun generateId(): String = UUID.randomUUID().toString()
