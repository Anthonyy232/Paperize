package com.anthonyla.paperize.core
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import com.anthonyla.paperize.core.ScreenMetricsCompat.getScreenSize
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Metadata
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.google.android.renderscript.Toolkit
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream


enum class Type { HOME, LOCK, SINGLE, REFRESH }

/**
 * Get the dimensions of the image from the uri
 */
fun Uri.getImageDimensions(context: Context): Size? {
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            var width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            var height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (width == 0 || height == 0) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(this),
                    null,
                    options
                )
                width = options.outWidth
                height = options.outHeight
            }
            Size(width, height)
        }
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error getting image dimensions: $e")
        null
    }
}

/**
 * Calculate the inSampleSize for the image
 */
fun calculateInSampleSize(imageSize: Size, width: Int, height: Int): Int {
    if (imageSize.width > width || imageSize.height > height) {
        val heightRatio = (imageSize.height.toFloat() / height.toFloat()).fastRoundToInt()
        val widthRatio = (imageSize.width.toFloat() / width.toFloat()).fastRoundToInt()
        return if (heightRatio < widthRatio) { heightRatio } else { widthRatio }
    }
    else { return 1 }
}

/**
 * Get the screen size of the device without orientation
 * https://stackoverflow.com/a/70087378
 */
object ScreenMetricsCompat {
    private val api: Api =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ApiLevel30() else Api()
    fun getScreenSize(context: Context): Size = api.getScreenSize(context)
    @Suppress("DEPRECATION")
    private open class Api {
        open fun getScreenSize(context: Context): Size {
            val display = context.getSystemService(WindowManager::class.java).defaultDisplay
            val metrics = if (display != null) {
                DisplayMetrics().also { display.getRealMetrics(it) }
            } else {
                Resources.getSystem().displayMetrics
            }
            return Size(metrics.widthPixels, metrics.heightPixels)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private class ApiLevel30 : Api() {
        override fun getScreenSize(context: Context): Size {
            val metrics: WindowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
            return Size(metrics.bounds.width(), metrics.bounds.height())
        }
    }
}

/**
 * Get device screen size with orientation
 */
fun getDeviceScreenSize(context: Context): Size {
    val orientation = context.resources.configuration.orientation
    val size = getScreenSize(context)
    return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        Size(size.width, size.height)
    } else {
        Size(size.height, size.width)
    }
}

/**
 * Retrieve a bitmap from a URI that is scaled down to the device's screen size
 */
fun retrieveBitmap(
    context: Context,
    wallpaper: Uri,
    width: Int,
    height: Int
): Bitmap? {
    val imageSize = wallpaper.getImageDimensions(context) ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSampleSize(
                    calculateInSampleSize(
                        imageSize,
                        width,
                        height
                    )
                )
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize =
                        calculateInSampleSize(imageSize, width, height)
                    inMutable = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    } else {
        context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize =
                    calculateInSampleSize(imageSize, width, height)
                inMutable = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }
}

/**
 * Scale a bitmap using the fit width method
 * The bitmap will fit into the given width while maintaining the same aspect ratio
 */
fun fitBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    return try {
        val aspectRatio = source.height.toFloat() / source.width.toFloat()
        val newHeight = (width * aspectRatio).toInt()
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val scaledBitmap = Bitmap.createScaledBitmap(source, width, newHeight, true)
        val top = (height - newHeight) / 2f
        canvas.drawBitmap(scaledBitmap, 0f, top, null)
        scaledBitmap.recycle()
        newBitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error fitting bitmap: $e")
        source
    }
}

/**
 * Scale a bitmap using the fit method
 */
fun fillBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    return try {
        val aspectRatio = source.width.toFloat() / source.height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height * aspectRatio) {
            newWidth = width
            newHeight = (width / aspectRatio).toInt()
        } else {
            newHeight = height
            newWidth = (height * aspectRatio).toInt()
        }
        val scaledBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
        val x = ((width - newWidth) / 2f)
        val y = ((height - newHeight) / 2f)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(scaledBitmap, x, y, null)
        scaledBitmap.recycle()
        bitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error filling bitmap: $e")
        source
    }
}

/**
 * Stretch the bitmap to fit the given width and height
 */
fun stretchBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    return try {
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val bitmap = Bitmap.createScaledBitmap(source, width, height, true)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        newBitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error stretching bitmap: $e")
        source
    }
}

/**
 * Darken the bitmap by a certain percentage - 0 is darkest, 100 is original
 */
fun darkenBitmap(source: Bitmap, percent: Int): Bitmap {
    return try {
        val safePercentage = (100 - percent).coerceIn(0, 100)
        val factor = 1 - safePercentage / 100f
        val canvas = Canvas(source)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(factor, factor, factor, 1f)
            })
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        source
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error darkening bitmap: $e")
        source
    }
}

/**
 * Blur the bitmap by a certain percentage
 */
fun blurBitmap(source: Bitmap, percent: Int): Bitmap {
    return try {
        val factor = percent.toFloat().div(100f) * 20f
        Toolkit.blur(source, factor.toInt())
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error darkening bitmap: $e")
        source
    }
}

/**
 * Apply a vignette effect to the bitmap
 */
fun vignetteBitmap(source: Bitmap, percent: Int): Bitmap {
    return try {
        val image = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawBitmap(source, 0f, 0f, Paint())
        val rad: Int = if (source.width < source.height) {
            val o = (source.height * 2) / 100
            source.height - o * percent / 3
        } else {
            val o = (source.width * 2) / 100
            source.width - o * percent / 3
        }
        val rect = Rect(0, 0, source.width, source.height)
        val rectF = RectF(rect)
        val colors = intArrayOf(0, 0, Color.BLACK)
        val pos = floatArrayOf(0.0f, 0.1f, 1.0f)
        val linGradLR: Shader = RadialGradient(
            rect.centerX().toFloat(),
            rect.centerY().toFloat(),
            rad.toFloat(),
            colors,
            pos,
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            shader = linGradLR
            isAntiAlias = true
            isDither = true
            alpha = 128
        }
        canvas.drawRect(rectF, paint)
        image
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error applying vignette effect: $e")
        source
    }
}

/**
 * Apply a grey filter to the bitmap based on a percentage
 */
fun grayBitmap(bitmap: Bitmap, percent: Int): Bitmap {
    val factor = percent / 100f
    val colorMatrix = ColorMatrix().apply {
        setSaturation(1 - factor)
    }
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayBitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    return grayBitmap
}

/**
 * Calculate the brightness of a bitmap (0-100)
 * https://gist.github.com/httnn/b1d772caf76cdc0c11e2
 */
fun calculateBrightness(bitmap: Bitmap, pixelSpacing: Int = 1): Int {
    var brightness = 0.0
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (i in pixels.indices step pixelSpacing.coerceAtLeast(1)) {
        val color = pixels[i]
        val R = Color.red(color)
        val G = Color.green(color)
        val B = Color.blue(color)
        brightness += 0.299*R + 0.587*G + 0.114*B
    }
    return (brightness / (pixels.size / pixelSpacing)).toInt()
}

/**
 * Get all wallpapers from a folder URI
 */
suspend fun getWallpaperFromFolder(folderUri: String, context: Context): List<Wallpaper> = withContext(Dispatchers.IO) {
    val allowedExtensions = setOf("jpg", "jpeg", "png", "heif", "webp")
    val contentResolver = context.contentResolver
    val wallpapers = mutableListOf<Wallpaper>()

    val foldersToVisit = ArrayDeque<Uri>()
    val visitedFolders = mutableSetOf<Uri>()
    if (folderUri.isEmpty()) {
        return@withContext emptyList<Wallpaper>()
    }
    foldersToVisit.add(folderUri.toUri())
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    try {
        while (foldersToVisit.isNotEmpty()) {
            val currentFolderUri = foldersToVisit.removeFirst()
            if (currentFolderUri.toString().isEmpty()) { continue }
            visitedFolders.add(currentFolderUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri.toUri(),
                DocumentsContract.getTreeDocumentId(currentFolderUri)
            )
            val cursor = contentResolver.query(childrenUri, projection, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val documentId = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                    val displayName = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                    val mimeType = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                    val dateModified = it.getLong(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri.toUri(), documentId)
                    if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                        if (documentUri !in visitedFolders) {
                            foldersToVisit.add(DocumentsContract.buildTreeDocumentUri(currentFolderUri.authority, documentId))
                        }
                    } else {
                        val extension = displayName.substringAfterLast('.', "").lowercase()
                        if (extension in allowedExtensions) {
                            wallpapers.add(
                                Wallpaper(
                                    initialAlbumName = "",
                                    wallpaperUri = documentUri.toString().compress("content://com.android.externalstorage.documents/"),
                                    fileName = displayName.substringBeforeLast('.', displayName),
                                    dateModified = dateModified,
                                    order = wallpapers.size + 1,
                                    key = 0
                                )
                            )
                        }
                    }
                }
            }
        }
    } catch (_: SecurityException) {
        return@withContext emptyList<Wallpaper>()
    }
    return@withContext wallpapers
}

/**
 * Get the last modified date of a folder
 * @param folderUri URI of the folder
 * @param context Android context
 */
suspend fun getFolderLastModified(folderUri: String, context: Context): Long = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        folderUri.toUri(),
        DocumentsContract.getTreeDocumentId(folderUri.toUri())
    )
    val cursor = contentResolver.query(childrenUri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            return@use it.getLong(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
        }
    }
    return@withContext 0
}

/**
 * Get filename and last modified date from an image URI
 * @param context Android context
 * @param uriString URI string of the image
 */
suspend fun getImageMetadata(context: Context, uriString: String): Metadata = withContext(Dispatchers.IO) {
    return@withContext try {
        val uri = uriString.toUri()
        try {
            val file = DocumentFileCompat.fromSingleUri(context, uri)
            return@withContext Metadata(file?.name?.substringBeforeLast('.', file.name).toString(), file?.lastModified ?: 0)
        } catch (_: Exception) {
            val file = DocumentFile.fromSingleUri(context, uri)
            return@withContext Metadata(file?.name?.substringBeforeLast('.', file.name.toString()) ?: "", file?.lastModified() ?: 0)
        }
    } catch (_: Exception) {
        return@withContext Metadata("", 0)
    }
}

/**
 * Helper function to find the first valid URI from a list of wallpapers and folders
 */
suspend fun findFirstValidUri(
    context: Context,
    folders: List<Folder>,
    wallpapers: List<Wallpaper>
): String? = withContext(Dispatchers.IO) {
    // Check folder wallpapers first
    folders.forEach { folder ->
        folder.wallpapers.forEach { wallpaper ->
            isValidUri(context, wallpaper.wallpaperUri).let { return@withContext wallpaper.wallpaperUri }
        }
    }
    // Then check individual wallpapers
    wallpapers.forEach { wallpaper ->
        isValidUri(context, wallpaper.wallpaperUri).let { return@withContext wallpaper.wallpaperUri }
    }
    return@withContext null
}

/**
 * Get the folder name from the folder URI
 */
suspend fun getFolderNameFromUri(folderUri: String, context: Context): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        DocumentFileCompat.fromTreeUri(context, folderUri.toUri())?.name
    } catch (_: Exception) {
        DocumentFile.fromTreeUri(context, folderUri.toUri())?.name
    }
}

/**
 * Check if a URI is valid
 */
fun isValidUri(context: Context, uriString: String?): Boolean {
    val uri = uriString?.decompress("content://com.android.externalstorage.documents/")?.toUri()
    if (uri == null) { return false }
    return try {
        DocumentFileCompat.fromSingleUri(context, uri)?.exists() ?: false
    } catch (_: Exception) {
        DocumentFile.fromSingleUri(context, uri)?.exists() ?: false
    }
}

fun isDirectory(context: Context, uriString: String?): Boolean {
    val uri = uriString?.toUri()
    if (uri == null) { return false }
    return try {
        DocumentFileCompat.fromSingleUri(context, uri)?.isDirectory() ?: false
    } catch (_: Exception) {
        DocumentFile.fromSingleUri(context, uri)?.isDirectory ?: false
    }
}

/**
 * Compress a string
 */
fun String.compress(prefixToRemove: String, charset: Charset = Charsets.UTF_8): String {
    val modifiedInput = if (this.startsWith(prefixToRemove)) {
        this.removePrefix(prefixToRemove)
    } else { this }
    ByteArrayOutputStream().use { byteStream ->
        DeflaterOutputStream(byteStream).use { deflaterStream ->
            deflaterStream.write(modifiedInput.toByteArray(charset))
        }
        return Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT)
    }
}

/**
 * Decompress a string
 */
fun String.decompress(prefixToAdd: String, charset: Charset = Charsets.UTF_8): String {
    val compressedData = Base64.decode(this, Base64.DEFAULT)
    ByteArrayInputStream(compressedData).use { byteStream ->
        InflaterInputStream(byteStream).use { inflaterStream ->
            val decompressedBytes = inflaterStream.readBytes()
            return prefixToAdd + String(decompressedBytes, charset)
        }
    }
}

/**
 * Darkens the bitmap by the given percentage and returns it
 * 0 - lightest, 100 - darkest
 */
fun processBitmap(
    width: Int,
    height: Int,
    source: Bitmap,
    darken: Boolean,
    darkenPercent: Int,
    scaling: ScalingConstants,
    blur: Boolean,
    blurPercent: Int,
    vignette: Boolean,
    vignettePercent: Int,
    grayscale: Boolean,
    grayscalePercent: Int
): Bitmap? {
    try {
        var processedBitmap = source

        // Apply wallpaper scaling effects
        processedBitmap = when (scaling) {
            ScalingConstants.FILL -> fillBitmap(processedBitmap, width, height)
            ScalingConstants.FIT -> fitBitmap(processedBitmap, width, height)
            ScalingConstants.STRETCH -> stretchBitmap(processedBitmap, width, height)
            ScalingConstants.NONE -> processedBitmap
        }

        // Apply brightness effect
        if (darken && darkenPercent < 100) {
            processedBitmap = darkenBitmap(processedBitmap, darkenPercent)
        }

        // Apply blur effect
        if (blur && blurPercent > 0) {
            processedBitmap = blurBitmap(processedBitmap, blurPercent)
        }

        // Apply vignette effect
        if (vignette && vignettePercent > 0) {
            processedBitmap = vignetteBitmap(processedBitmap, vignettePercent)
        }

        // Apply gray effect
        if (grayscale && grayscalePercent > 0) {
            processedBitmap = grayBitmap(processedBitmap, grayscalePercent)
        }

        return processedBitmap
    } catch (e: Exception) {
        Log.e("PaperizeWallpaperChanger", "Error darkening bitmap", e)
        return null
    }
}