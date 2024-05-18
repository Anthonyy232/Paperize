package com.anthonyla.paperize.core
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
import androidx.exifinterface.media.ExifInterface

/**
 * Get the dimensions of the image from the uri
 */
fun Uri.getImageDimensions(context: Context): Size {
    val inputStream = context.contentResolver.openInputStream(this)!!
    val exif = ExifInterface(inputStream)
    var width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
    var height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
    if (width == 0 || height == 0) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(this), null, options)
        width = options.outWidth
        height = options.outHeight
    }
    return Size(width, height)
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
    val bitmap = Bitmap.createScaledBitmap(source, width, newHeight, true)
    val top = (height - newHeight) / 2f
    canvas.drawBitmap(bitmap, 0f, top, null)
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
        return newBitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error stretching bitmap: $e")
        return source
    }
}

/**
 * Darken the bitmap by a certain percentage - 0 is darkest, 100 is original
 */
fun darkenBitmap(source: Bitmap, percent: Int, width: Int, height: Int): Bitmap {
    try {
        val safePercentage = (100 - percent).coerceIn(0, 100)
        val factor = 1 - safePercentage / 100f
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(factor, factor, factor, 1f)
            })
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return bitmap
    } catch (e: Exception) {
        Log.e("WallpaperUtil", "Error darkening bitmap: $e")
        return source
    }
}