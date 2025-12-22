package com.anthonyla.paperize.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.UUID

/**
 * URI extensions
 */
fun Uri.isValid(contentResolver: ContentResolver): Boolean {
    // Basic scheme check - we only handle content URIs
    if (scheme != "content") return false
    
    return try {
        // Optimization: Use query to check existence/accessibility without opening the file
        // This is significantly faster and less likely to hang on slow cloud providers
        contentResolver.query(this, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    } catch (e: Exception) {
        // Fallback to opening the stream if the provider doesn't support the specific query
        try {
            contentResolver.openFileDescriptor(this, "r")?.use { true } ?: false
        } catch (e2: Exception) {
            false
        }
    }
}

fun Uri.getFileName(context: Context): String? {
    return DocumentFile.fromSingleUri(context, this)?.name
}

/**
 * Scan a folder URI and return all image file URIs
 */
fun Uri.scanFolderForImages(context: Context): List<Uri> {
    val documentFile = DocumentFile.fromTreeUri(context, this) ?: return emptyList()
    if (!documentFile.isDirectory) return emptyList()

    val imageUris = mutableListOf<Uri>()
    val supportedTypes = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/avif"
    )

    fun scanDirectory(directory: DocumentFile) {
        directory.listFiles().forEach { file ->
            when {
                file.isDirectory -> scanDirectory(file) // Recursively scan subdirectories
                file.isFile && file.type in supportedTypes -> {
                    file.uri?.let { imageUris.add(it) }
                }
            }
        }
    }

    scanDirectory(documentFile)
    return imageUris.sortedBy { it.toString() } // Sort for consistency
}

/**
 * UUID generation
 */
fun generateId(): String = UUID.randomUUID().toString()
