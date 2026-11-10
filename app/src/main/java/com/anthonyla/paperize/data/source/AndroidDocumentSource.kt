package com.anthonyla.paperize.data.source

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.core.runCatchingCancellable
import com.anthonyla.paperize.domain.source.DocumentSource
import com.anthonyla.paperize.domain.source.SourceFolder
import com.anthonyla.paperize.domain.source.SourceImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class AndroidDocumentSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DocumentSource {
    override suspend fun retainReadPermission(uri: String): Unit = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override suspend fun readImage(uri: String): SourceImage = withContext(Dispatchers.IO) {
        val document = DocumentFile.fromSingleUri(context, uri.toUri())
        SourceImage(uri, document?.name ?: uri.substringAfterLast('/'), document?.lastModified() ?: 0L)
    }

    override suspend fun readFolder(uri: String, onProgress: (Int) -> Unit): SourceFolder = withContext(Dispatchers.IO) {
        val tree = uri.toUri()
        SourceFolder(
            DocumentFile.fromTreeUri(context, tree)?.name ?: uri.substringAfterLast('/'),
            tree.scanFolderImages(context, onProgress)
        )
    }

    override suspend fun isMissing(uri: String, isTree: Boolean): Boolean = withContext(Dispatchers.IO) {
        val source = uri.toUri()
        val document = if (isTree) {
            DocumentsContract.buildDocumentUriUsingTree(source, DocumentsContract.getTreeDocumentId(source))
        } else source
        document.isDocumentMissing(context.contentResolver)
    }
}

/** Only a successful, complete query with no rows proves that a document was removed. */
internal fun Uri.isDocumentMissing(contentResolver: ContentResolver): Boolean {
    if (scheme != ContentResolver.SCHEME_CONTENT) return false
    // Offline providers, revoked permissions and query failures do not prove deletion.
    return runCatchingCancellable {
        contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                cursor.requireComplete()
                !cursor.moveToFirst()
            } ?: false
    }.getOrDefault(false)
}

private fun Cursor.requireComplete() {
    val metadata = extras
    if (metadata.getBoolean(DocumentsContract.EXTRA_LOADING, false) ||
        metadata.getString(DocumentsContract.EXTRA_ERROR) != null) {
        throw IOException("Document provider has not returned a complete result")
    }
}

/**
 * Traverses directories iteratively, querying metadata once per directory.
 * Rejects incomplete provider results and cancels in-flight queries with the caller.
 * [onProgress] reports every [PROGRESS_REPORT_INTERVAL] discoveries and on completion.
 */
internal suspend fun Uri.scanFolderImages(context: Context, onProgress: (found: Int) -> Unit = {}): List<SourceImage> {
    val rootDocumentId = DocumentsContract.getTreeDocumentId(this)

    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    )

    val results = mutableListOf<SourceImage>()
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
                            SourceImage(
                                uri = DocumentsContract.buildDocumentUriUsingTree(this, documentId).toString(),
                                name = name,
                                modifiedAt = if (cursor.isNull(modifiedColumn)) 0L else cursor.getLong(modifiedColumn)
                            )
                        )
                        if (results.size - lastReported >= PROGRESS_REPORT_INTERVAL) {
                            lastReported = results.size
                            onProgress(results.size)
                        }
                    }
                }
            }
        }
    }
    onProgress(results.size)
    return results
}

private const val PROGRESS_REPORT_INTERVAL = 512
