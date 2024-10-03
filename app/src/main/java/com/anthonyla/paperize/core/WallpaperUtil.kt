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
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.google.android.renderscript.Toolkit
import com.lazygeniouz.dfc.file.DocumentFileCompat


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
 * Retrieve wallpaper URIs from a folder directory URI
 */
fun getWallpaperFromFolder(folderUri: String, context: Context): List<String> {
    return try {
        val folderDocumentFile = DocumentFileCompat.fromTreeUri(context, folderUri.toUri())
        listFilesRecursive(folderDocumentFile, context)
    } catch (_: Exception) {
        val folderDocumentFile = DocumentFile.fromTreeUri(context, folderUri.toUri())
        listFilesRecursive(folderDocumentFile, context)
    }
}

/**
 * Helper function to recursively list files in a directory for DocumentFileCompat
 */
fun listFilesRecursive(parent: DocumentFileCompat?, context: Context): List<String> {
    val files = mutableListOf<String>()
    parent?.listFiles()?.forEach { file ->
        if (file.isDirectory()) {
            files.addAll(listFilesRecursive(file, context))
        } else {
            val allowedExtensions = listOf("jpg", "jpeg", "png", "heif", "webp", "JPG", "JPEG", "PNG", "HEIF", "WEBP")
            if (file.extension in allowedExtensions) {
                files.add(file.uri.toString())
            }
        }
    }
    return files
}

/** Overloaded version of the function for DocumentFile */
fun listFilesRecursive(parent: DocumentFile?, context: Context): List<String> {
    val files = mutableListOf<String>()
    parent?.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            files.addAll(listFilesRecursive(file, context))
        } else {
            val allowedExtensions = listOf("jpg", "jpeg", "png", "heif", "webp", "JPG", "JPEG", "PNG", "HEIF", "WEBP")
            if ((file.name?.substringAfterLast(".") ?: "") in allowedExtensions) {
                files.add(file.uri.toString())
            }
        }
    }
    return files
}

/**
 * Helper function to find the first valid URI from a list of wallpapers
 * It will search through all wallpapers of an album first, and then all wallpapers of every folder of an album
 */
fun findFirstValidUri(context: Context, wallpapers: List<Wallpaper>, folders: List<Folder>): String? {
    wallpapers.forEach { wallpaper ->
        try {
            val file = DocumentFileCompat.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
            if (file?.exists() == true) {
                return wallpaper.wallpaperUri
            }
        } catch (_: Exception) {
            val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
            if (file?.exists() == true) {
                return wallpaper.wallpaperUri
            }
        }
    }
    folders.forEach { folder ->
        folder.wallpapers.forEach { wallpaper ->
            try {
                val file = DocumentFileCompat.fromSingleUri(context, wallpaper.toUri())
                if (file?.exists() == true) {
                    return wallpaper
                }
            } catch (_: Exception) {
                val file = DocumentFile.fromSingleUri(context, wallpaper.toUri())
                if (file?.exists() == true) {
                    return wallpaper
                }
            }
        }
    }
    return null
}

/**
 * Get the folder name from the folder URI
 */
fun getFolderNameFromUri(folderUri: String, context: Context): String? {
    return try {
        DocumentFileCompat.fromTreeUri(context, folderUri.toUri())?.name
    } catch (_: Exception) {
        DocumentFile.fromTreeUri(context, folderUri.toUri())?.name
    }
}

/**
 * Check if a URI is valid
 */
fun isValidUri(context: Context, uriString: String?): Boolean {
    val uri = uriString?.toUri()
    return try {
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            inputStream?.close()
        }
        true
    } catch (_: Exception) { false }
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