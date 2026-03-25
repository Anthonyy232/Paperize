package com.anthonyla.paperize.service.livewallpaper.renderer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastRoundToInt
import com.anthonyla.paperize.core.ScalingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * @property scalingType How the image will be scaled for display
 */
class ContentUriImageLoader(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val scalingType: ScalingType = ScalingType.FILL
) : ImageLoader {

    companion object {
        private const val TAG = "ContentUriImageLoader"
    }

    override suspend fun load(targetWidth: Int, targetHeight: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // ImageDecoder auto-applies EXIF orientation; info.size returns post-EXIF (display)
                // dimensions. Computing decodeWidth/decodeHeight inside the lambda from info.size
                // ensures correct aspect-ratio math regardless of EXIF rotation metadata.
                val source = ImageDecoder.createSource(contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val srcWidth = info.size.width
                    val srcHeight = info.size.height

                    val (decodeWidth, decodeHeight) = when (scalingType) {
                        ScalingType.FILL -> {
                            val scale = maxOf(
                                targetWidth.toFloat() / srcWidth,
                                targetHeight.toFloat() / srcHeight
                            )
                            Pair((srcWidth * scale).fastRoundToInt(), (srcHeight * scale).fastRoundToInt())
                        }
                        ScalingType.FIT -> {
                            val scale = minOf(
                                targetWidth.toFloat() / srcWidth,
                                targetHeight.toFloat() / srcHeight
                            )
                            Pair((srcWidth * scale).fastRoundToInt(), (srcHeight * scale).fastRoundToInt())
                        }
                        ScalingType.STRETCH -> Pair(targetWidth, targetHeight)
                        ScalingType.NONE -> {
                            // Cap at 2× screen size to prevent OOM on very large source images
                            val maxW = targetWidth * 2
                            val maxH = targetHeight * 2
                            if (srcWidth > maxW || srcHeight > maxH) {
                                val scale = minOf(maxW.toFloat() / srcWidth, maxH.toFloat() / srcHeight)
                                Pair((srcWidth * scale).fastRoundToInt(), (srcHeight * scale).fastRoundToInt())
                            } else {
                                Pair(srcWidth, srcHeight)
                            }
                        }
                    }

                    Log.d(TAG, "Loading image: ${srcWidth}x${srcHeight} -> ${decodeWidth}x${decodeHeight} " +
                            "(scaling: $scalingType, target: ${targetWidth}x${targetHeight})")

                    decoder.setTargetSize(decodeWidth, decodeHeight)
                    decoder.isMutableRequired = false  // Immutable for GPU upload
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                
                // Validate loaded bitmap
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    Log.e(TAG, "Loaded bitmap has invalid dimensions: ${bitmap.width}x${bitmap.height}")
                    bitmap.recycle()
                    return@withContext null
                }
                
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from $uri", e)
                null
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OOM loading image from $uri", e)
                null
            }
        }
    }

}
