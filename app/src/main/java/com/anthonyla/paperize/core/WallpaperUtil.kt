package com.anthonyla.paperize.core
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
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
 * Retrieve a bitmap from a URI that is scaled down to the device's screen size
 */
fun retrieveBitmap(
    context: Context,
    wallpaper: Uri,
    device: DisplayMetrics
): Bitmap? {
    val imageSize = wallpaper.getImageDimensions(context) ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSampleSize(
                    calculateInSampleSize(
                        imageSize,
                        device.widthPixels,
                        device.heightPixels
                    )
                )
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize =
                        calculateInSampleSize(imageSize, device.widthPixels, device.heightPixels)
                    inMutable = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    } else {
        context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize =
                    calculateInSampleSize(imageSize, device.widthPixels, device.heightPixels)
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
        val factor = percent.toFloat().div(100f) * 10
        Toolkit.blur(source, factor.toInt())
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error darkening bitmap: $e")
        source
    }
}

/**
 * Retrieve wallpaper URIs from a folder directory URI
 */
fun getWallpaperFromFolder(folderUri: String, context: Context): List<String> {
    return try {
        val folderDocumentFile = DocumentFileCompat.fromTreeUri(context, folderUri.toUri())
        listFilesRecursive(folderDocumentFile, context)
    } catch (e: Exception) {
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
        } catch (e: Exception) {
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
            } catch (e: Exception) {
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
    } catch (e: Exception) {
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
    } catch (e: Exception) { false }
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
 * Darkens the bitmap by the given percentage and returns it
 * 0 - lightest, 100 - darkest
 */
fun processBitmap(
    device: DisplayMetrics,
    source: Bitmap,
    darken: Boolean,
    darkenPercent: Int,
    scaling: Boolean,
    scalingMode: ScalingConstants,
    blur: Boolean,
    blurPercent: Int
): Bitmap? {
    try {
        var processedBitmap = source

        // Apply wallpaper scaling effects
        if (scaling) {
            processedBitmap = when (scalingMode) {
                ScalingConstants.FILL -> fillBitmap(processedBitmap, device.widthPixels, device.heightPixels)
                ScalingConstants.FIT -> fitBitmap(processedBitmap, device.widthPixels, device.heightPixels)
                ScalingConstants.STRETCH -> stretchBitmap(processedBitmap, device.widthPixels, device.heightPixels)
            }
        }

        // Apply brightness effect
        if (darken && darkenPercent < 100) {
            processedBitmap = darkenBitmap(processedBitmap, darkenPercent)
        }

        // Apply blur effect
        if (blur && blurPercent > 0) {
            processedBitmap = blurBitmap(processedBitmap, blurPercent)
        }
        return processedBitmap
    } catch (e: Exception) {
        Log.e("PaperizeWallpaperChanger", "Error darkening bitmap", e)
        return null
    }
}