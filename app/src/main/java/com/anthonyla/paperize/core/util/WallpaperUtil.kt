package com.anthonyla.paperize.core.util

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
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.hardware.HardwareRenderer
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.anthonyla.paperize.core.ScalingType

/**
 * Wallpaper utility functions for bitmap processing and effects
 */

private const val TAG = "WallpaperUtil"
private val SharedPaintFilterAntiAlias = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

/**
 * Get the dimensions of the image from the URI
 */
fun Uri.getImageDimensions(context: Context): Size? {
    // Try EXIF first
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
        Log.w(TAG, "Error reading EXIF dimensions for $this, falling back: $e")
    }

    // Fallback to BitmapFactory
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
        Log.e(TAG, "Error getting image dimensions with BitmapFactory for $this: $e")
        return null
    }

    Log.w(TAG, "Could not get image dimensions for $this")
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
    return 1
}

/**
 * Get device screen size without orientation
 */
object ScreenMetricsCompat {
    private val api: Api = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ApiLevel30()
    } else {
        Api()
    }

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
    val size = ScreenMetricsCompat.getScreenSize(context)
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
    wallpaperUri: Uri,
    width: Int,
    height: Int,
    scaling: ScalingType = ScalingType.FIT
): Bitmap? {
    val imageSize = wallpaperUri.getImageDimensions(context) ?: return null
    if (imageSize.width <= 0 || imageSize.height <= 0) {
        Log.e(TAG, "Invalid image dimensions from URI: $imageSize")
        return null
    }

    val sampleSize = if (scaling == ScalingType.NONE) {
        1
    } else {
        calculateInSampleSize(imageSize, width, height)
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, wallpaperUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSampleSize(sampleSize)
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory: $e")
            context.contentResolver.openInputStream(wallpaperUri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inMutable = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    } else {
        context.contentResolver.openInputStream(wallpaperUri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }
}

/**
 * Scale a bitmap using the fit method
 * The bitmap will fit into the given dimensions while maintaining aspect ratio
 */
fun fitBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width == width && source.height == height) {
        return source
    }
    if (source.width <= 0 || source.height <= 0 || width <= 0 || height <= 0) {
        Log.w(TAG, "Invalid dimensions for fitBitmap, returning source.")
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
        Log.e(TAG, "Error fitting bitmap: $e")
        source
    }
}

/**
 * Scale a bitmap using the fill method (crop to fill dimensions)
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
        Log.e(TAG, "Error filling bitmap: $e")
        source
    }
}

/**
 * Stretch the bitmap to fit the given dimensions
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
        Log.e(TAG, "Error stretching bitmap: $e")
        source
    }
}

/**
 * Darken the bitmap by a certain percentage
 * @param brightnessToRetainPercent 0-100 (0 is darkest, 100 is original)
 */
fun darkenBitmap(source: Bitmap, brightnessToRetainPercent: Int): Bitmap {
    if (!source.isMutable) {
        Log.w(TAG, "darkenBitmap received an immutable bitmap. Returning a copy.")
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
 * Blur the bitmap by a certain percentage using GPU acceleration
 * @param percent 0-100
 */
@RequiresApi(Build.VERSION_CODES.S)
fun blurBitmap(source: Bitmap, percent: Int): Bitmap {
    val clampedPercent = percent.coerceIn(0, 100)
    if (clampedPercent == 0) {
        return source
    }

    val maxBlurRadius = 25.0f
    val radius = (clampedPercent / 100.0f) * maxBlurRadius

    val imageReader = ImageReader.newInstance(
        source.width, source.height,
        PixelFormat.RGBA_8888, 1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )

    val renderNode = RenderNode("BlurEffect")
    val hardwareRenderer = HardwareRenderer()

    var resultBitmap: Bitmap?
    try {
        hardwareRenderer.setSurface(imageReader.surface)
        hardwareRenderer.setContentRoot(renderNode)
        renderNode.setPosition(0, 0, source.width, source.height)

        val blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
        renderNode.setRenderEffect(blurEffect)

        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(source, 0f, 0f, null)
        renderNode.endRecording()

        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = imageReader.acquireNextImage()
            ?: throw IllegalStateException("Failed to acquire blurred image")

        val hardwareBuffer = image.hardwareBuffer
            ?: throw IllegalStateException("Failed to acquire hardware buffer")

        val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
            ?: throw IllegalStateException("Failed to create bitmap from hardware buffer")

        // Convert hardware bitmap to regular bitmap for further processing
        resultBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)

        hardwareBuffer.close()
        image.close()

    } finally {
        hardwareRenderer.destroy()
        renderNode.discardDisplayList()
        imageReader.close()
    }

    return resultBitmap
}

/**
 * Apply a vignette effect to the bitmap
 * @param percent 0-100
 */
fun vignetteBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    if (!source.isMutable) {
        Log.w(TAG, "vignetteBitmap received an immutable bitmap. Returning a copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        return vignetteBitmap(mutableCopy, percent)
    }

    return try {
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
        source
    } catch (e: Exception) {
        Log.e(TAG, "Error applying vignette: $e")
        source
    }
}

/**
 * Apply a grayscale filter to the bitmap
 * @param percent 0-100 (0 is original, 100 is full grayscale)
 */
fun grayscaleBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    if (!source.isMutable) {
        Log.w(TAG, "grayscaleBitmap received an immutable bitmap. Returning a copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        return grayscaleBitmap(mutableCopy, percent)
    }

    val factor = percent.coerceIn(0, 100) / 100f
    if (factor <= 0f) return source

    val colorMatrix = ColorMatrix().apply { setSaturation(1 - factor) }
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }

    Canvas(source).drawBitmap(source, 0f, 0f, paint)
    return source
}

/**
 * Apply scaling to bitmap based on ScalingType
 */
fun scaleBitmap(source: Bitmap, width: Int, height: Int, scaling: ScalingType): Bitmap {
    return when (scaling) {
        ScalingType.FILL -> fillBitmap(source, width, height)
        ScalingType.FIT -> fitBitmap(source, width, height)
        ScalingType.STRETCH -> stretchBitmap(source, width, height)
        ScalingType.NONE -> source
    }
}

/**
 * Process bitmap with all effects
 */
fun processBitmap(
    source: Bitmap,
    darkenPercent: Int = 0,
    blurPercent: Int = 0,
    vignettePercent: Int = 0,
    grayscalePercent: Int = 0
): Bitmap {
    var result = source

    // Apply effects in order
    if (darkenPercent > 0) {
        result = darkenBitmap(result, 100 - darkenPercent)
    }

    if (blurPercent > 0) {
        result = blurBitmap(result, blurPercent)
    }

    if (vignettePercent > 0) {
        result = vignetteBitmap(result, vignettePercent)
    }

    if (grayscalePercent > 0) {
        result = grayscaleBitmap(result, grayscalePercent)
    }

    return result
}
