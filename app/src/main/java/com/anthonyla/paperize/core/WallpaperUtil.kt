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
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
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
    private val api: Api = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ApiLevel30() else Api()
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
                decoder.setTargetSampleSize(calculateInSampleSize(imageSize, width, height))
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(imageSize, width, height)
                    inMutable = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    } else {
        context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(imageSize, width, height)
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
    if (source.width == width && source.height == height) {
        return source
    }
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val scale = width.toFloat() / source.width
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(0f, (height - source.height * scale) / 2)
        }
        canvas.drawBitmap(source, matrix, null)
        bitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error fitting bitmap: $e")
        source
    }
}

/**
 * Scale a bitmap using the fit method
 */
fun fillBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width == width && source.height == height) {
        return source
    }
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val sourceAspect = source.width.toFloat() / source.height.toFloat()
        val targetAspect = width.toFloat() / height.toFloat()
        val scale = if (sourceAspect > targetAspect) { height.toFloat() / source.height.toFloat() }
        else { width.toFloat() / source.width.toFloat() }
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                (width - source.width * scale) / 2,
                (height - source.height * scale) / 2
            )
        }
        canvas.drawBitmap(source, matrix, null)
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
    if (source.width == width && source.height == height) {
        return source
    }
    return try {
        val matrix = Matrix().apply {
            setScale(width.toFloat() / source.width, height.toFloat() / source.height)
        }
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            Canvas(this).drawBitmap(source, matrix, paint)
        }
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error stretching bitmap: $e")
        source
    }
}

/**
 * Darken the bitmap by a certain percentage - 0 is darkest, 100 is original
 */
fun darkenBitmap(source: Bitmap, percent: Int): Bitmap {
    val factor = (100 - percent.coerceIn(0, 100)) / 100f
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setScale(factor, factor, factor, 1f)
        })
    }

    return source.copy(Bitmap.Config.RGB_565, true).apply {
        Canvas(this).drawBitmap(source, 0f, 0f, paint)
    }
}

/**
 * Blur the bitmap by a certain percentage
 */
fun blurBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    return try {
        val radius = (percent * 0.2f).toInt()
        Toolkit.blur(source, radius)
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error blurring bitmap: $e")
        source
    }
}

/**
 * Apply a vignette effect to the bitmap
 */
fun vignetteBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    return try {
        val image = source.copy(Bitmap.Config.RGB_565, true) ?: return source
        val canvas = Canvas(image)
        val dim = if (source.width < source.height) source.height else source.width
        val rad = (dim * (150f - percent) / 150f).toInt()
        val centerX = source.width / 2f
        val centerY = source.height / 2f

        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.TRANSPARENT,
            Color.argb((0.5f * 255).toInt(), 0, 0, 0)
        )
        val pos = floatArrayOf(0f, 0.1f, 1f)

        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                centerX, centerY, rad.toFloat(),
                colors, pos, Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), vignettePaint)
        image
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error applying vignette: $e")
        source
    }
}

/**
 * Apply a grey filter to the bitmap based on a percentage
 */
fun grayBitmap(bitmap: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return bitmap
    val factor = percent / 100f
    val colorMatrix = ColorMatrix().apply { setSaturation(1 - factor) }
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }

    val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
    Canvas(grayBitmap).apply { drawBitmap(bitmap, 0f, 0f, paint) }
    return grayBitmap
}

/**
 * Calculate the brightness of a bitmap (0-100)
 */
fun calculateBrightness(bitmap: Bitmap, sampleSize: Int = 16): Int {
    val pixels = IntArray(bitmap.width * bitmap.height / sampleSize)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height / sampleSize)
    var total = 0f
    for (i in pixels.indices step sampleSize) {
        val color = pixels[i]
        total += 0.299f * Color.red(color) +
                0.587f * Color.green(color) +
                0.114f * Color.blue(color)
    }
    return (total / (pixels.size / sampleSize)).toInt()
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
                                    order = wallpapers.size,
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
 * Get the folder name from the folder URI
 */
suspend fun getFolderMetadata(folderUri: String, context: Context): Metadata = withContext(Dispatchers.IO) {
    return@withContext try {
        val uri = folderUri.toUri()
        try {
            val file = DocumentFileCompat.fromTreeUri(context, uri)
            Metadata(file?.name?.substringBeforeLast('.', file.name).toString(), file?.lastModified ?: 0)
        } catch (_: Exception) {
            val file = DocumentFile.fromTreeUri(context, uri)
            Metadata(file?.name?.substringBeforeLast('.', file.name.toString()) ?: "", file?.lastModified() ?: 0)
        }
    } catch (_: Exception) {
        Metadata("", 0)
    }
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
            Metadata(file?.name?.substringBeforeLast('.', file.name).toString(), file?.lastModified ?: 0)
        } catch (_: Exception) {
            val file = DocumentFile.fromSingleUri(context, uri)
            Metadata(file?.name?.substringBeforeLast('.', file.name.toString()) ?: "", file?.lastModified() ?: 0)
        }
    } catch (_: Exception) {
        Metadata("", 0)
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
 * Check if a URI is valid
 */
fun isValidUri(context: Context, uriString: String?): Boolean {
    val uri = uriString?.decompress("content://com.android.externalstorage.documents/")?.toUri() ?: return false
    return try {
        DocumentFileCompat.fromSingleUri(context, uri)?.exists() ?: false
    } catch (_: Exception) {
        DocumentFile.fromSingleUri(context, uri)?.exists() ?: false
    }
}

/**
 * Check if a URI is a directory
 */
fun isDirectory(context: Context, uriString: String?): Boolean {
    val uri = uriString?.toUri() ?: return false
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
    }
    catch (e: OutOfMemoryError) {
        Log.e("PaperizeWallpaperChanger", "Error ran out of memory", e)
        return processBitmap(
            width/2,
            height/2,
            source,
            darken,
            darkenPercent,
            scaling,
            blur,
            blurPercent,
            vignette,
            vignettePercent,
            grayscale,
            grayscalePercent
        )
    }
    catch (e: Exception) {
        Log.e("PaperizeWallpaperChanger", "Error processing bitmap", e)
        return null
    }
}