package com.anthonyla.paperize.core
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
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

enum class Type { HOME, LOCK, BOTH }

/**
 * Get the dimensions of the image from the uri
 */
fun Uri.getImageDimensions(context: Context): Size? {
    try {
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
            return Size(width, height)
        }
        return null
    } catch (e: Exception) { return null }
}

/**
 * Calculate the inSampleSize for the image
 */
fun calculateInSampleSize(imageSize: Size, width: Int, height: Int): Int {
    return if (imageSize.width <= width && imageSize.height <= height) { 1 }
    else { (imageSize.width/ width.toFloat()).fastRoundToInt() }
}

/**
 * Scale a bitmap using the fit width method
 * The bitmap will fit into the given width while maintaining the same aspect ratio
 */
fun fitBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    val aspectRatio = source.height.toFloat() / source.width.toFloat()
    val newHeight = (width * aspectRatio).toInt()
    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(newBitmap)
    val scaledBitmap = Bitmap.createScaledBitmap(source, width, newHeight, true)
    val top = (height - newHeight) / 2f
    canvas.drawBitmap(scaledBitmap, 0f, top, null)
    scaledBitmap.recycle()
    return newBitmap
}

/**
 * Scale a bitmap using the fill height method
 */
fun fillBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    try {
        val aspectRatio = source.width.toFloat() / source.height.toFloat()
        var newWidth = width
        var newHeight = (newWidth / aspectRatio).toInt()
        if (newHeight < height) {
            newHeight = height
            newWidth = (newHeight * aspectRatio).toInt()
        }
        val scaledBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
        val x = (width - newWidth) / 2f
        val y = (height - newHeight) / 2f
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(scaledBitmap, x, y, null)
        scaledBitmap.recycle()
        return bitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error filling bitmap: $e")
        return source
    }
}

/**
 * Stretch the bitmap to fit the given width and height
 */
fun stretchBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    try {
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val bitmap = Bitmap.createScaledBitmap(source, width, height, true)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return newBitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error stretching bitmap: $e")
        return source
    }
}

/**
 * Darken the bitmap by a certain percentage - 0 is darkest, 100 is original
 */
fun darkenBitmap(source: Bitmap, percent: Int): Bitmap {
    try {
        val safePercentage = (100 - percent).coerceIn(0, 100)
        val factor = 1 - safePercentage / 100f
        val canvas = Canvas(source)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(factor, factor, factor, 1f)
            })
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return source
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error darkening bitmap: $e")
        return source
    }
}

fun blurBitmap(source: Bitmap, percent: Int): Bitmap {
    try {
        val factor = percent.toFloat().div(100f) * 10
        return Toolkit.blur(source, factor.toInt())
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error darkening bitmap: $e")
        return source
    }
}

/**
 * Retrieve wallpaper URIs from a folder directory URI
 */
fun getWallpaperFromFolder(folderUri: String, context: Context): List<String> {
    try {
        val folderDocumentFile = DocumentFileCompat.fromTreeUri(context, folderUri.toUri())
        return listFilesRecursive(folderDocumentFile, context)
    } catch (e: Exception) {
        val folderDocumentFile = DocumentFile.fromTreeUri(context, folderUri.toUri())
        return listFilesRecursive(folderDocumentFile, context)
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
        val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
        if (file?.exists() == true) {
            return wallpaper.wallpaperUri
        }
    }
    folders.forEach { folder ->
        folder.wallpapers.forEach { wallpaper ->
            val file = DocumentFile.fromSingleUri(context, wallpaper.toUri())
            if (file?.exists() == true) {
                return wallpaper
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
 * Check if device uses a live wallpaper
 */
fun isLiveWallpaperSet(context: Context): Boolean {
    val wallpaperManager = WallpaperManager.getInstance(context)
    return wallpaperManager.wallpaperInfo != null
}

