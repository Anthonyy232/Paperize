package com.anthonyla.paperize.core.util

import android.content.ContentResolver
import android.net.Uri
import java.util.UUID

fun Uri.isReadable(contentResolver: ContentResolver): Boolean {
    if (scheme != ContentResolver.SCHEME_CONTENT) return false
    return try {
        contentResolver.openFileDescriptor(this, "r")?.use { true } ?: false
    } catch (_: Exception) {
        false
    }
}

fun generateId(): String = UUID.randomUUID().toString()
