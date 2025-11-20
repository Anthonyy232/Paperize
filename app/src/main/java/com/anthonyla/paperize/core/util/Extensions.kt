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
    return try {
        contentResolver.openInputStream(this)?.use { true } ?: false
    } catch (e: Exception) {
        false
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
