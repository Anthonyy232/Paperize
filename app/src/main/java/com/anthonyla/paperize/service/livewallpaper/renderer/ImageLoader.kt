package com.anthonyla.paperize.service.livewallpaper.renderer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Sealed interface for loading images from different sources.
 * Provides type-safe abstraction for image loading with automatic
 * sample size calculation and memory optimization.
 */
sealed interface ImageLoader {

    /**
     * Load the image as a Bitmap with the specified target dimensions.
     * The image will be downsampled to approximately match the target size.
     *
     * @param targetWidth Target width in pixels
     * @param targetHeight Target height in pixels
     * @return Loaded bitmap, or null if loading failed
     */
    suspend fun load(targetWidth: Int, targetHeight: Int): Bitmap?
}

/**
 * Empty image loader that always returns null.
 * Used as a null object pattern when no wallpaper is available.
 */
object EmptyImageLoader : ImageLoader {
    override suspend fun load(targetWidth: Int, targetHeight: Int): Bitmap? = null
}

/**
 * Image loader for Android content URIs.
 * Loads images from the MediaStore or other content providers.
 *
 * @property contentResolver Content resolver for URI access
 * @property uri Content URI to load
 */
class ContentUriImageLoader(
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : ImageLoader {

    companion object {
        private const val TAG = "ContentUriImageLoader"
    }

    override suspend fun load(targetWidth: Int, targetHeight: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Get image dimensions without loading pixel data
                val dimensions = getImageDimensions() ?: return@withContext null

                // Calculate optimal sample size
                val sampleSize = calculateSampleSize(
                    dimensions.first,
                    dimensions.second,
                    targetWidth,
                    targetHeight
                )

                Log.d(TAG, "Loading image: ${dimensions.first}x${dimensions.second} -> " +
                        "${dimensions.first / sampleSize}x${dimensions.second / sampleSize} " +
                        "(sample size: $sampleSize)")

                // Load bitmap with sampling
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(sampleSize)
                    decoder.isMutableRequired = false  // Immutable for GPU upload
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from $uri", e)
                null
            }
        }
    }

    /**
     * Get image dimensions without loading the full bitmap.
     */
    private fun getImageDimensions(): Pair<Int, Int>? {
        return try {
            val source = ImageDecoder.createSource(contentResolver, uri)
            var width = 0
            var height = 0

            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                width = info.size.width
                height = info.size.height
                decoder.setTargetSampleSize(Int.MAX_VALUE) // Don't load pixel data
            }

            if (width > 0 && height > 0) {
                Pair(width, height)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get image dimensions for $uri", e)
            null
        }
    }

    /**
     * Calculate the optimal sample size for downsampling.
     * Returns the largest power-of-2 sample size that still maintains
     * image dimensions larger than or equal to target dimensions.
     *
     * @param width Source image width
     * @param height Source image height
     * @param targetWidth Target width
     * @param targetHeight Target height
     * @return Sample size (power of 2)
     */
    private fun calculateSampleSize(
        width: Int,
        height: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        // Guard against invalid target dimensions
        if (targetWidth <= 0 || targetHeight <= 0) {
            Log.w(TAG, "Invalid target dimensions: ${targetWidth}x${targetHeight}, using original size")
            return 1
        }

        // Guard against invalid source dimensions
        if (width <=0 || height <= 0) {
            Log.w(TAG, "Invalid source dimensions: ${width}x${height}")
            return 1
        }

        var sampleSize = 1

        if (width > targetWidth || height > targetHeight) {
            val halfWidth = width / 2
            val halfHeight = height / 2

            // Calculate the largest sample size that is a power of 2 and keeps both
            // dimensions larger than the target dimensions
            while ((halfWidth / sampleSize) >= targetWidth &&
                   (halfHeight / sampleSize) >= targetHeight) {
                sampleSize *= 2
            }
        }

        // Ensure we don't downsample below target size
        return max(1, sampleSize / 2)
    }
}
