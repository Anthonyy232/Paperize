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
import androidx.core.graphics.createBitmap
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
    try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (width > 0 && height > 0) {
                return Size(width, height)
            }
        }
    } catch (e: Exception) {
        Log.w("WallpaperUtil", "Error reading EXIF dimensions for $this, falling back: $e")
    }

    try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                return Size(options.outWidth, options.outHeight)
            }
        }
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error getting image dimensions with BitmapFactory for $this: $e")
        return null
    }
    Log.w("WallpaperUtil", "Could not get image dimensions for $this")
    return null
}


/**
 * Calculate the inSampleSize for the image
 */
fun calculateInSampleSize(imageSize: Size, width: Int, height: Int): Int {
    if (imageSize.width == 0 || imageSize.height == 0) return 1
    if (width == 0 || height == 0) return 1

    if (imageSize.width > width || imageSize.height > height) {
        val heightRatio = (imageSize.height.toFloat() / height.toFloat()).fastRoundToInt()
        val widthRatio = (imageSize.width.toFloat() / width.toFloat()).fastRoundToInt()
        return (if (heightRatio < widthRatio) heightRatio else widthRatio).coerceAtLeast(1)
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
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
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
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics: WindowMetrics = windowManager.currentWindowMetrics
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
        Size(minOf(size.width, size.height), maxOf(size.width, size.height))
    } else {
        Size(maxOf(size.width, size.height), minOf(size.width, size.height))
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
    if (imageSize.width <= 0 || imageSize.height <= 0) {
        Log.e("WallpaperUtil", "Invalid image dimensions from URI: $imageSize")
        return null
    }
    val sampleSize = calculateInSampleSize(imageSize, width, height)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSampleSize(sampleSize)
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            Log.w("WallpaperUtil", "ImageDecoder failed, falling back to BitmapFactory: $e")
            context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inMutable = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    } else {
        context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }
}

private val SharedPaintFilterAntiAlias = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

/**
 * Scale a bitmap using the fit width method
 * The bitmap will fit into the given width while maintaining the same aspect ratio
 */
fun fitBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width == width && source.height == height) {
        return source
    }
    if (source.width <= 0 || source.height <= 0 || width <= 0 || height <= 0) {
        Log.w("WallpaperUtil", "Invalid dimensions for fitBitmap, returning source.")
        return source
    }

    return try {
        val bitmap = createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width.toFloat() / source.width
        val scaledSourceHeight = source.height * scale
        val yOffset = (height - scaledSourceHeight) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(0f, yOffset)
        }
        canvas.drawBitmap(source, matrix, SharedPaintFilterAntiAlias)
        bitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error fitting bitmap: $e")
        source
    }
}

/**
 * Scale a bitmap using the fill method
 */
fun fillBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width == width && source.height == height) {
        return source
    }
    return try {
        val bitmap = createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val sourceAspect = source.width.toFloat() / source.height.toFloat()
        val targetAspect = width.toFloat() / height.toFloat()

        val scale: Float
        val xOffset: Float
        val yOffset: Float

        if (sourceAspect >= targetAspect) {
            scale = height.toFloat() / source.height.toFloat()
            xOffset = (width - source.width * scale) / 2f
            yOffset = 0f
        } else {
            scale = width.toFloat() / source.width.toFloat()
            xOffset = 0f
            yOffset = (height - source.height * scale) / 2f
        }

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(xOffset, yOffset)
        }
        canvas.drawBitmap(source, matrix, SharedPaintFilterAntiAlias)
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
        val bitmap = createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val matrix = Matrix().apply {
            setScale(
                width.toFloat() / source.width,
                height.toFloat() / source.height
            )
        }
        canvas.drawBitmap(source, matrix, SharedPaintFilterAntiAlias)
        bitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error stretching bitmap: $e")
        source
    }
}

/**
 * Darken the bitmap by a certain percentage - 0 is darkest, 100 is original
 */
fun darkenBitmap(source: Bitmap, brightnessToRetainPercent: Int): Bitmap {
    if (!source.isMutable) {
        Log.w("WallpaperUtil", "darkenBitmap received an immutable bitmap. Returning a copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        return darkenBitmap(mutableCopy, brightnessToRetainPercent)
    }

    val targetBrightnessFactor = brightnessToRetainPercent.coerceIn(0, 100) / 100f
    if (targetBrightnessFactor >= 1.0f) {
        return source
    }

    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setScale(targetBrightnessFactor, targetBrightnessFactor, targetBrightnessFactor, 1f)
        })
    }
    Canvas(source).drawBitmap(source, 0f, 0f, paint)
    return source
}

/**
 * Blur the bitmap by a certain percentage
 */
fun blurBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    return try {
        val radius = (percent.coerceIn(0, 100) * 0.25f).coerceIn(1f, 25f)
        Toolkit.blur(source, radius.toInt())
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error blurring bitmap (Renderscript): $e")
        source
    }
}

/**
 * Apply a vignette effect to the bitmap
 */
fun vignetteBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    if (!source.isMutable) {
        Log.w("WallpaperUtil", "vignetteBitmap received an immutable bitmap. Returning a copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        return vignetteBitmap(mutableCopy, percent)
    }

    try {
        val canvas = Canvas(source)
        val dim = if (source.width < source.height) source.height else source.width
        val rad = (dim * (1 - (percent.coerceIn(0, 100) / 150f))).coerceAtLeast(0.1f)
        val centerX = source.width / 2f
        val centerY = source.height / 2f

        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.argb((0.1f * 255).toInt(), 0, 0, 0),
            Color.argb((0.8f * 255).toInt(), 0, 0, 0)
        )
        val pos = floatArrayOf(0f, 0.7f, 1f)


        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX, centerY, rad,
                colors, pos, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), vignettePaint)
        return source
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error applying vignette: $e")
        return source
    }
}

/**
 * Apply a grey filter to the bitmap based on a percentage
 */
fun grayBitmap(bitmap: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return bitmap
    if (!bitmap.isMutable) {
        Log.w("WallpaperUtil", "grayBitmap received an immutable bitmap. Returning a copy.")
        val mutableCopy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        return grayBitmap(mutableCopy, percent)
    }

    val factor = percent.coerceIn(0, 100) / 100f
    if (factor <= 0f) return bitmap

    val colorMatrix = ColorMatrix().apply { setSaturation(1 - factor) }
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }

    Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, paint)
    return bitmap
}

/**
 * Get all wallpapers from a folder URI
 */
suspend fun getWallpaperFromFolder(folderUri: String, context: Context): List<Wallpaper> = withContext(Dispatchers.IO) {
    val allowedExtensions = setOf("jpg", "jpeg", "png", "heif", "heic", "webp")
    val contentResolver = context.contentResolver
    val wallpapers = mutableListOf<Wallpaper>()

    if (folderUri.isEmpty()) {
        return@withContext emptyList<Wallpaper>()
    }
    val rootUri = folderUri.toUri()
    val foldersToVisit = ArrayDeque<Uri>()
    foldersToVisit.add(rootUri)

    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    try {
        while (foldersToVisit.isNotEmpty()) {
            val currentFolderDocumentUri = foldersToVisit.removeFirst()
            val currentFolderTreeDocId = DocumentsContract.getTreeDocumentId(currentFolderDocumentUri)
                ?: DocumentsContract.getDocumentId(currentFolderDocumentUri)

            if (currentFolderTreeDocId == null) {
                Log.w("WallpaperUtil", "Could not get tree document ID for $currentFolderDocumentUri")
                continue
            }

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                rootUri,
                currentFolderTreeDocId
            )

            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val lastModifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex)
                    val mimeType = cursor.getString(mimeTypeIndex)
                    val dateModified = cursor.getLong(lastModifiedIndex)

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId)

                    if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                        val dirTreeUri = DocumentsContract.buildTreeDocumentUri(rootUri.authority, documentId)
                        foldersToVisit.add(dirTreeUri)
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
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error getting wallpapers from folder '$folderUri': ", e)
        return@withContext emptyList<Wallpaper>()
    }
    return@withContext wallpapers
}


/**
 * Get the folder name from the folder URI
 */
suspend fun getFolderMetadata(folderUri: String, context: Context): Metadata = withContext(Dispatchers.IO) {
    if (folderUri.isEmpty()) return@withContext Metadata("", 0)
    return@withContext try {
        val uri = folderUri.toUri()
        val file = DocumentFileCompat.fromTreeUri(context, uri)
        val name = file?.name?.substringBeforeLast('.', file.name) ?: ""
        val lastModified = file?.lastModified ?: 0
        Metadata(name, lastModified)
    } catch (e: Exception) {
        Log.w("WallpaperUtil", "Error getting folder metadata with DocumentFileCompat for '$folderUri': $e, trying DocumentFile")
        try {
            val uri = folderUri.toUri()
            val file = DocumentFile.fromTreeUri(context, uri)
            val name = file?.name?.substringBeforeLast('.', file.name.orEmpty()) ?: ""
            val lastModified = file?.lastModified() ?: 0
            Metadata(name, lastModified)
        } catch (e2: Exception) {
            Log.e("WallpaperUtil", "Error getting folder metadata with DocumentFile for '$folderUri': $e2")
            Metadata("", 0)
        }
    }
}

/**
 * Get filename and last modified date from an image URI
 * @param context Android context
 * @param uriString URI string of the image
 */
suspend fun getImageMetadata(context: Context, uriString: String): Metadata = withContext(Dispatchers.IO) {
    if (uriString.isEmpty()) return@withContext Metadata("", 0)
    return@withContext try {
        val uri = uriString.toUri()
        val file = DocumentFileCompat.fromSingleUri(context, uri)
        val name = file?.name?.substringBeforeLast('.', file.name) ?: ""
        val lastModified = file?.lastModified ?: 0
        Metadata(name, lastModified)
    } catch (e: Exception) {
        Log.w("WallpaperUtil", "Error getting image metadata with DocumentFileCompat for '$uriString': $e, trying DocumentFile")
        try {
            val uri = uriString.toUri()
            val file = DocumentFile.fromSingleUri(context, uri)
            val name = file?.name?.substringBeforeLast('.', file.name.orEmpty()) ?: ""
            val lastModified = file?.lastModified() ?: 0
            Metadata(name, lastModified)
        } catch (e2: Exception) {
            Log.e("WallpaperUtil", "Error getting image metadata with DocumentFile for '$uriString': $e2")
            Metadata("", 0)
        }
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
    try {
        for (folder in folders) {
            for (wallpaper in folder.wallpapers) {
                try {
                    if (isValidUri(context, wallpaper.wallpaperUri)) {
                        return@withContext wallpaper.wallpaperUri
                    }
                } catch (e: Exception) {
                    Log.w("WallpaperUtil", "Error checking URI validity for folder wallpaper: ${wallpaper.wallpaperUri}", e)
                }
            }
        }
        for (wallpaper in wallpapers) {
            try {
                if (isValidUri(context, wallpaper.wallpaperUri)) {
                    return@withContext wallpaper.wallpaperUri
                }
            } catch (e: Exception) {
                Log.w("WallpaperUtil", "Error checking URI validity for standalone wallpaper: ${wallpaper.wallpaperUri}", e)
            }
        }
        return@withContext null
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error finding valid URI", e)
        return@withContext null
    }
}

/**
 * Check if a URI is valid
 */
fun isValidUri(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrEmpty()) return false
    val decompressedUriString = try {
        uriString.decompress("content://com.android.externalstorage.documents/")
    } catch (e: Exception) {
        Log.w("WallpaperUtil", "Failed to decompress URI string: $uriString", e)
        uriString
    }
    val uri = decompressedUriString.toUri()

    return try {
        DocumentFileCompat.fromSingleUri(context, uri)?.exists() == true
    } catch (e: Exception) {
        Log.w("WallpaperUtil", "Error checking URI validity with DocumentFileCompat for '$uri': $e, trying DocumentFile")
        try {
            DocumentFile.fromSingleUri(context, uri)?.exists() == true
        } catch (e2: Exception) {
            Log.w("WallpaperUtil", "Error checking URI validity with DocumentFile for '$uri': $e2")
            false
        }
    }
}

/**
 * Check if a URI is a directory
 */
fun isDirectory(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrEmpty()) return false
    val uri = uriString.toUri()
    return try {
        DocumentFile.fromSingleUri(context, uri)?.isDirectory == true
    } catch (e: Exception) {
        Log.w("WallpaperUtil", "Error checking if URI is a directory for '$uri': $e")
        try {
            DocumentFile.fromTreeUri(context, uri)?.isDirectory == true
        } catch (e2: Exception) {
            Log.w("WallpaperUtil", "Error checking tree URI directory status for '$uri': $e2")
            false
        }
    }
}

/**
 * Compress a string
 */
fun String.compress(prefixToRemove: String, charset: Charset = Charsets.UTF_8): String {
    val modifiedInput = if (this.startsWith(prefixToRemove)) {
        this.substring(prefixToRemove.length)
    } else { this }
    ByteArrayOutputStream().use { byteStream ->
        DeflaterOutputStream(byteStream).use { deflaterStream ->
            deflaterStream.write(modifiedInput.toByteArray(charset))
        }
        return Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
    }
}

/**
 * Decompress a string
 */
fun String.decompress(prefixToAdd: String, charset: Charset = Charsets.UTF_8): String {
    val compressedData = Base64.decode(this, Base64.NO_WRAP)
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

        processedBitmap = when (scaling) {
            ScalingConstants.FILL -> fillBitmap(processedBitmap, width, height)
            ScalingConstants.FIT -> fitBitmap(processedBitmap, width, height)
            ScalingConstants.STRETCH -> stretchBitmap(processedBitmap, width, height)
            ScalingConstants.NONE -> processedBitmap
        }

        if (!processedBitmap.isMutable && (darken || vignette || grayscale)) {
            Log.w("ProcessBitmap", "Bitmap became immutable before effects. Making a mutable copy.")
            processedBitmap = processedBitmap.copy(processedBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }

        if (darken && darkenPercent > 0) {
            processedBitmap = darkenBitmap(processedBitmap, darkenPercent)
        }

        if (blur && blurPercent > 0) {
            val blurred = blurBitmap(processedBitmap, blurPercent)
            processedBitmap = blurred
            if (!processedBitmap.isMutable && (vignette || grayscale)) {
                Log.w("ProcessBitmap", "Bitmap became immutable after blur. Making a mutable copy.")
                processedBitmap = processedBitmap.copy(processedBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }
        }

        if (vignette && vignettePercent > 0) {
            processedBitmap = vignetteBitmap(processedBitmap, vignettePercent)
        }

        if (grayscale && grayscalePercent > 0) {
            processedBitmap = grayBitmap(processedBitmap, grayscalePercent)
        }

        return processedBitmap
    }
    catch (e: OutOfMemoryError) {
        Log.e("PaperizeWallpaperChanger", "Error: Ran out of memory during bitmap processing. Attempting with smaller dimensions.", e)
        if (width / 2 > 0 && height / 2 > 0) {
            return processBitmap(
                width / 2,
                height / 2,
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
        } else {
            Log.e("PaperizeWallpaperChanger", "Cannot reduce dimensions further for OOM recovery.")
            return null
        }
    }
    catch (e: Exception) {
        Log.e("PaperizeWallpaperChanger", "Error processing bitmap", e)
        return null
    }
}