package com.anthonyla.paperize.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.core.constants.Constants
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
    } catch (_: Exception) {
        // Fallback to opening the stream if the provider doesn't support the specific query
        try {
            contentResolver.openFileDescriptor(this, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
}

fun Uri.getFileName(context: Context): String? {
    return DocumentFile.fromSingleUri(context, this)?.name
}

/**
 * One image file discovered while scanning a folder tree.
 *
 * [name] and [lastModified] are read from the same cursor row as [uri], so callers
 * do not need a follow-up per-file query to obtain them.
 */
data class ScannedImage(
    val uri: Uri,
    val name: String,
    val lastModified: Long
)

/**
 * Recursively scan a tree [Uri] for image files.
 *
 * Uses a single [DocumentsContract] cursor query per directory instead of
 * [DocumentFile.listFiles], which issues a separate IPC round-trip for every file and
 * for every attribute access (name, isDirectory, lastModified). For folders with tens of
 * thousands of files that difference is the dominant cost. Directories are traversed
 * iteratively to avoid deep-recursion stack overflow on heavily nested trees.
 *
 * Returned URIs are built from the same tree document id used by [DocumentFile], so they
 * are byte-for-byte identical to the previous implementation and remain stable across the app.
 *
 * [onProgress] is invoked with the running number of images found, throttled to roughly once
 * per [PROGRESS_REPORT_INTERVAL] discoveries so callers can show a live count without flooding.
 */
fun Uri.scanFolderImages(context: Context, onProgress: ((found: Int) -> Unit)? = null): List<ScannedImage> {
    val rootDocumentId = try {
        DocumentsContract.getTreeDocumentId(this)
    } catch (_: IllegalArgumentException) {
        return emptyList() // Not a tree URI
    }

    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    )

    val results = mutableListOf<ScannedImage>()
    var lastReported = 0
    val pendingDirs = ArrayDeque<String>()
    pendingDirs.addLast(rootDocumentId)

    while (pendingDirs.isNotEmpty()) {
        val parentDocumentId = pendingDirs.removeLast()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(this, parentDocumentId)
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn) ?: continue
                    if (cursor.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingDirs.addLast(documentId)
                    } else {
                        val name = cursor.getString(nameColumn) ?: continue
                        val extension = name.substringAfterLast('.', "").lowercase()
                        if (extension in Constants.SUPPORTED_IMAGE_EXTENSIONS) {
                            results.add(
                                ScannedImage(
                                    uri = DocumentsContract.buildDocumentUriUsingTree(this, documentId),
                                    name = name,
                                    lastModified = if (cursor.isNull(modifiedColumn)) 0L else cursor.getLong(modifiedColumn)
                                )
                            )
                            if (onProgress != null && results.size - lastReported >= PROGRESS_REPORT_INTERVAL) {
                                lastReported = results.size
                                onProgress(results.size)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Skip directories that can't be read and continue with the rest of the tree.
        }
    }
    onProgress?.invoke(results.size)
    return results
}

/** Report scan progress at most once per this many discovered images. */
private const val PROGRESS_REPORT_INTERVAL = 512

/**
 * UUID generation
 */
fun generateId(): String = UUID.randomUUID().toString()
