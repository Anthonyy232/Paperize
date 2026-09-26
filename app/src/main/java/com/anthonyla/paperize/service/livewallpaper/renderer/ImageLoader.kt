package com.anthonyla.paperize.service.livewallpaper.renderer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.util.calculateDecodeSize

sealed interface ImageLoader {
    /** Decode on a worker thread. The caller owns the returned bitmap. */
    fun load(targetWidth: Int, targetHeight: Int): Bitmap?
}

object EmptyImageLoader : ImageLoader {
    override fun load(targetWidth: Int, targetHeight: Int): Bitmap? = null
}

class ContentUriImageLoader(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val scalingType: ScalingType = ScalingType.FILL
) : ImageLoader {
    override fun load(targetWidth: Int, targetHeight: Int): Bitmap? = try {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // ImageDecoder reports dimensions after applying EXIF orientation.
            val (width, height) = calculateDecodeSize(
                info.size.width, info.size.height, targetWidth, targetHeight, scalingType
            )
            decoder.setTargetSize(width, height)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: Exception) {
        Log.e("ContentUriImageLoader", "Failed to load image from $uri", e)
        null
    } catch (e: OutOfMemoryError) {
        Log.e("ContentUriImageLoader", "OOM loading image from $uri", e)
        null
    }
}
