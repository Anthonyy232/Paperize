package com.anthonyla.paperize.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.core.constants.Constants
import android.database.Cursor
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

fun Uri.isValid(contentResolver: ContentResolver): Boolean {
    if (scheme != "content") return false
    
    return try {
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

/** Only a successful, complete query with no rows proves that a document was removed. */
fun Uri.isDocumentMissing(contentResolver: ContentResolver): Boolean {
    if (scheme != ContentResolver.SCHEME_CONTENT) return false
    return try {
        contentResolver.query(this, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                cursor.requireComplete()
                !cursor.moveToFirst()
            } ?: false
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Offline providers, revoked permissions and query failures do not prove deletion.
        false
    }
}

private fun Cursor.requireComplete() {
    val metadata = extras
    if (metadata.getBoolean(DocumentsContract.EXTRA_LOADING, false) ||
        metadata.getString(DocumentsContract.EXTRA_ERROR) != null) {
        throw IOException("Document provider has not returned a complete result")
    }
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
 * Traverses directories iteratively, querying metadata once per directory.
 * Rejects incomplete provider results and cancels in-flight queries with the caller.
 * [onProgress] reports every [PROGRESS_REPORT_INTERVAL] discoveries and on completion.
 */
suspend fun Uri.scanFolderImages(context: Context, onProgress: ((found: Int) -> Unit)? = null): List<ScannedImage> {
    val rootDocumentId = DocumentsContract.getTreeDocumentId(this)

    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    )

    val results = mutableListOf<ScannedImage>()
    var lastReported = 0
    val pendingDirs = ArrayDeque<String>()
    val visitedDocuments = mutableSetOf(rootDocumentId)
    pendingDirs.addLast(rootDocumentId)

    while (pendingDirs.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        val parentDocumentId = pendingDirs.removeLast()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(this, parentDocumentId)
        val cursor = suspendCancellableCoroutine<Cursor> { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val result = context.contentResolver.query(childrenUri, projection, null, null, null, signal)
                    ?: throw IOException("Document provider could not read the folder")
                continuation.resume(result) { _, value, _ -> value.close() }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
        cursor.use {
            cursor.requireComplete()
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val documentId = cursor.getString(idColumn) ?: continue
                if (!visitedDocuments.add(documentId)) continue
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
    }
    onProgress?.invoke(results.size)
    return results
}

private const val PROGRESS_REPORT_INTERVAL = 512

fun generateId(): String = UUID.randomUUID().toString()
