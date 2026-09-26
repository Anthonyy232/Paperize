package com.anthonyla.paperize.core.util

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.data.repository.WallpaperRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DocumentAccessInstrumentedTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val provider = TestProvider()
    private val resolver = ContentResolver.wrap(provider)
    private val context = object : ContextWrapper(appContext) {
        override fun getContentResolver(): ContentResolver = resolver
    }
    private val documentUri = Uri.parse("content://test/document/image")
    private val treeUri = Uri.parse("content://test/tree/root")

    @Test fun missingRequiresACompleteSuccessfulQuery() {
        provider.answer = { MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)) }
        assertTrue(documentUri.isDocumentMissing(resolver))
        provider.answer = { MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf<Any>("photo")) } }
        assertFalse(documentUri.isDocumentMissing(resolver))
        provider.answer = { null }
        assertFalse(documentUri.isDocumentMissing(resolver))
        provider.answer = { throw SecurityException("Permission revoked") }
        assertFalse(documentUri.isDocumentMissing(resolver))
        provider.answer = { throw IllegalStateException("Offline") }
        assertFalse(documentUri.isDocumentMissing(resolver))
    }

    @Test fun loadingAndErrorResponsesDoNotProveDeletion() {
        for (metadata in listOf(
            Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, true) },
            Bundle().apply { putString(DocumentsContract.EXTRA_ERROR, "Offline") }
        )) {
            provider.answer = {
                MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { extras = metadata }
            }
            assertFalse(documentUri.isDocumentMissing(resolver))
        }
    }

    @Test fun scanRejectsPartialResultsAndClosesItsCursor() = runBlocking {
        val cursor = directoryCursor().apply {
            addRow(arrayOf<Any>("image", "photo.png", "image/png", 1L))
            extras = Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, true) }
        }
        provider.answer = { cursor }
        try {
            treeUri.scanFolderImages(context)
            fail("Expected incomplete scan failure")
        } catch (_: java.io.IOException) { }
        assertTrue(cursor.isClosed)
    }

    @Test fun scanDetectsCyclesAndKeepsMetadata() = runBlocking {
        provider.answer = { uri ->
            when (DocumentsContract.getDocumentId(uri)) {
                "root" -> directoryCursor().apply {
                    addRow(arrayOf<Any>("child", "Child", DocumentsContract.Document.MIME_TYPE_DIR, 0L))
                    addRow(arrayOf<Any>("image", "photo.PNG", "image/png", 42L))
                }
                else -> directoryCursor().apply {
                    addRow(arrayOf<Any>("root", "Root", DocumentsContract.Document.MIME_TYPE_DIR, 0L))
                    addRow(arrayOf<Any>("image", "photo.PNG", "image/png", 42L))
                }
            }
        }
        val found = treeUri.scanFolderImages(context)
        assertEquals(1, found.size)
        assertEquals("photo.PNG", found.single().name)
        assertEquals(42L, found.single().lastModified)
        assertEquals(2, provider.queries)
    }

    @Test fun failureInChildFolderDoesNotReturnPartialImport() = runBlocking {
        provider.answer = { uri ->
            if (DocumentsContract.getDocumentId(uri) == "root") directoryCursor().apply {
                addRow(arrayOf<Any>("image", "photo.png", "image/png", 42L))
                addRow(arrayOf<Any>("child", "Child", DocumentsContract.Document.MIME_TYPE_DIR, 0L))
            } else null
        }
        try {
            treeUri.scanFolderImages(context)
            fail("Expected child folder failure")
        } catch (_: java.io.IOException) { }
    }

    @Test fun cancellingAnActiveScanCancelsTheProviderQuery() = runBlocking {
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        provider.onQuery = { signal ->
            signal?.setOnCancelListener { cancelled.countDown() }
            started.countDown()
            check(cancelled.await(5, TimeUnit.SECONDS)) { "Provider query was not cancelled" }
            signal?.throwIfCanceled()
        }
        val scan = launch(Dispatchers.IO) { treeUri.scanFolderImages(context) }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        scan.cancel()
        scan.join()
        assertEquals(0L, cancelled.count)
    }

    @Test fun cleanupCountsDuplicateRowsAndPreservesOtherAlbums() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PaperizeDatabase::class.java).build()
        try {
            db.albumDao().insertAlbum(AlbumEntity("one", "One", null, 0L, 0L))
            db.albumDao().insertAlbum(AlbumEntity("two", "Two", null, 0L, 0L))
            val uri = documentUri.toString()
            val missing = WallpaperEntity("first", "one", null, uri, "photo.png", 0L)
            // More than one page of duplicate URIs caught the old offset/counting bug.
            db.wallpaperDao().insertWallpapers((0..104).map { missing.copy(id = "missing-$it") })
            db.wallpaperDao().insertWallpaper(missing.copy(id = "other-album", albumId = "two"))
            provider.answer = { MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)) }
            val repository = WallpaperRepositoryImpl(context, db.wallpaperDao(), db.wallpaperQueueDao(), db.wallpaperCurrentDao())
            assertEquals(Result.Success(105), repository.validateAndRemoveInvalidWallpapers("one"))
            assertEquals(0, db.wallpaperDao().getWallpaperCountByAlbum("one"))
            assertNotNull(db.wallpaperDao().getWallpaperById("other-album"))
        } finally { db.close() }
    }

    private fun directoryCursor() = MatrixCursor(arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    ))

    private class TestProvider : ContentProvider() {
        var queries = 0
        var answer: (Uri) -> Cursor? = { null }
        var onQuery: ((CancellationSignal?) -> Unit)? = null
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            queries++
            return answer(uri)
        }
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?, cancellationSignal: CancellationSignal?): Cursor? {
            onQuery?.invoke(cancellationSignal)
            return query(uri, projection, selection, selectionArgs, sortOrder)
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
