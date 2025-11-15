package com.anthonyla.paperize.core.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * String extensions
 */
fun String.toUriOrNull(): Uri? = try {
    this.toUri()
} catch (e: Exception) {
    null
}

fun String.isValidImageUri(contentResolver: ContentResolver): Boolean {
    val uri = this.toUriOrNull() ?: return false
    return try {
        contentResolver.openInputStream(uri)?.use { true } ?: false
    } catch (e: Exception) {
        false
    }
}

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

fun Uri.getFileExtension(): String? {
    val path = this.path ?: return null
    return path.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
}

fun Uri.isDirectory(context: Context): Boolean {
    return DocumentFile.fromTreeUri(context, this)?.isDirectory ?: false
}

fun Uri.getLastModified(context: Context): Long {
    return DocumentFile.fromSingleUri(context, this)?.lastModified() ?: 0L
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
 * Long (timestamp) extensions
 */
fun Long.toFormattedDate(pattern: String = "MMM dd, yyyy HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> this.toFormattedDate("MMM dd, yyyy")
    }
}

/**
 * List extensions
 */
fun <T> List<T>.replace(index: Int, item: T): List<T> {
    return toMutableList().apply { this[index] = item }
}

fun <T> List<T>.removeAt(index: Int): List<T> {
    return toMutableList().apply { removeAt(index) }
}

fun <T> List<T>.move(from: Int, to: Int): List<T> {
    return toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }
}

/**
 * Bitmap extensions
 */
fun Bitmap.scale(maxWidth: Int, maxHeight: Int): Bitmap {
    if (width <= maxWidth && height <= maxHeight) return this

    val ratio = minOf(
        maxWidth.toFloat() / width,
        maxHeight.toFloat() / height
    )

    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

/**
 * UUID generation
 */
fun generateId(): String = UUID.randomUUID().toString()

/**
 * Collection extensions
 */
fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

/**
 * Safe execution
 */
inline fun <T> runCatchingOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}
