package com.anthonyla.paperize.core.util
import com.anthonyla.paperize.core.constants.Constants

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.HardwareRenderer
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.anthonyla.paperize.core.ScalingType
import com.anthonyla.paperize.core.WallpaperMediaType

/**
 * Wallpaper utility functions for bitmap processing and effects
 *
 * Improvements:
 * - Added EXIF orientation handling
 * - API-safe blur with fallback for Android < S
 * - Memory management with bitmap recycling
 * - Effect enable flags respected
 * - Quality optimization options
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
 * Get EXIF orientation from URI
 */
fun Uri.getExifOrientation(context: Context): Int {
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        } ?: ExifInterface.ORIENTATION_UNDEFINED
    } catch (e: Exception) {
        Log.w(TAG, "Error reading EXIF orientation: $e")
        ExifInterface.ORIENTATION_UNDEFINED
    }
}

/**
 * Create transformation matrix for EXIF orientation
 */
fun getExifTransformationMatrix(orientation: Int, width: Int, height: Int): Matrix {
    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
            matrix.setScale(-1f, 1f)
            matrix.postTranslate(width.toFloat(), 0f)
        }
        ExifInterface.ORIENTATION_ROTATE_180 -> {
            matrix.setRotate(180f)
            matrix.postTranslate(width.toFloat(), height.toFloat())
        }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setScale(1f, -1f)
            matrix.postTranslate(0f, height.toFloat())
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> {
            matrix.setRotate(90f)
            matrix.postTranslate(height.toFloat(), 0f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> {
            matrix.setRotate(-90f)
            matrix.postTranslate(0f, width.toFloat())
        }
    }

    return matrix
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
    fun getScreenSize(context: Context): Size {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics: WindowMetrics = windowManager.currentWindowMetrics
        return Size(metrics.bounds.width(), metrics.bounds.height())
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
 * Now with EXIF orientation support
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

    val bitmap = try {
        val source = ImageDecoder.createSource(context.contentResolver, wallpaperUri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            val (targetWidth, targetHeight) = when (scaling) {
                ScalingType.FILL -> {
                    // Calculate scale to cover target dimensions
                    val widthRatio = width.toFloat() / imageSize.width
                    val heightRatio = height.toFloat() / imageSize.height
                    val scale = maxOf(widthRatio, heightRatio)
                    Pair((imageSize.width * scale).fastRoundToInt(), (imageSize.height * scale).fastRoundToInt())
                }
                ScalingType.FIT -> {
                    // Calculate scale to fit inside target dimensions
                    val widthRatio = width.toFloat() / imageSize.width
                    val heightRatio = height.toFloat() / imageSize.height
                    val scale = minOf(widthRatio, heightRatio)
                    Pair((imageSize.width * scale).fastRoundToInt(), (imageSize.height * scale).fastRoundToInt())
                }
                ScalingType.STRETCH -> Pair(width, height)
                ScalingType.NONE -> Pair(imageSize.width, imageSize.height)
            }

            decoder.setTargetSize(targetWidth, targetHeight)
            decoder.isMutableRequired = true
            // Use high quality when possible
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: Exception) {
        Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory: $e")
        context.contentResolver.openInputStream(wallpaperUri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM during retrieveBitmap for $wallpaperUri: $e")
        null
    }

    // Handle EXIF orientation
    return bitmap?.let { applyExifOrientation(it, wallpaperUri, context) }
}

/**
 * Apply EXIF orientation to bitmap
 */
private fun applyExifOrientation(source: Bitmap, uri: Uri, context: Context): Bitmap {
    val orientation = uri.getExifOrientation(context)
    if (orientation == ExifInterface.ORIENTATION_UNDEFINED || orientation == ExifInterface.ORIENTATION_NORMAL) {
        return source
    }

    return try {
        val matrix = getExifTransformationMatrix(orientation, source.width, source.height)
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) {
            source.recycle() // Recycle old bitmap to free memory
        }
        rotated
    } catch (e: Exception) {
        Log.e(TAG, "Error applying EXIF orientation: $e")
        source
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM applying EXIF orientation: $e")
        source
    }
}

/**
 * Scale a bitmap using the fit method
 * The bitmap will fit into the given dimensions while maintaining aspect ratio
 */
/**
 * Scale a bitmap using the fit method
 * The bitmap will fit into the given dimensions while maintaining aspect ratio
 */
fun fitBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width <= 0 || source.height <= 0 || width <= 0 || height <= 0) {
        Log.w(TAG, "Invalid dimensions for fitBitmap: source=${source.width}x${source.height}, target=${width}x${height}")
        return source
    }
    if (source.width == width && source.height == height) {
        return source
    }

    return try {
        val bitmap = createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scaleX = width.toFloat() / source.width
        val scaleY = height.toFloat() / source.height
        // Use the smaller scale factor to ensure the entire image fits within the target dimensions
        val scale = minOf(scaleX, scaleY)
        
        val scaledSourceWidth = source.width * scale
        val scaledSourceHeight = source.height * scale
        
        val xOffset = (width - scaledSourceWidth) / 2f
        val yOffset = (height - scaledSourceHeight) / 2f
        
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(xOffset, yOffset)
        }
        canvas.drawBitmap(source, matrix, SharedPaintFilterAntiAlias)
        bitmap
    } catch (e: Exception) {
        Log.e(TAG, "Error fitting bitmap: $e")
        source
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM fitting bitmap: $e")
        source
    }
}

/**
 * Scale a bitmap using the fill method (crop to fill dimensions)
 */
fun fillBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width <= 0 || source.height <= 0 || width <= 0 || height <= 0) {
        Log.w(TAG, "Invalid dimensions for fillBitmap: source=${source.width}x${source.height}, target=${width}x${height}")
        return source
    }
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
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM filling bitmap: $e")
        source
    }
}

/**
 * Stretch the bitmap to fit the given dimensions
 */
fun stretchBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    if (source.width <= 0 || source.height <= 0 || width <= 0 || height <= 0) {
        Log.w(TAG, "Invalid dimensions for stretchBitmap: source=${source.width}x${source.height}, target=${width}x${height}")
        return source
    }
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
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM stretching bitmap: $e")
        source
    }
}

/**
 * Darken the bitmap by a certain percentage
 * @param darkenPercent 0-100 (0 is original brightness, 100 is completely dark/black)
 */
fun darkenBitmap(source: Bitmap, darkenPercent: Int): Bitmap {
    if (!source.isMutable) {
        Log.w(TAG, "darkenBitmap received an immutable bitmap. Creating a mutable copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        source.recycle()
        return darkenBitmap(mutableCopy, darkenPercent)
    }

    // Convert darkenPercent to brightness factor (invert)
    val targetBrightnessFactor = (100 - darkenPercent.coerceIn(0, 100)) / 100f
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
 * Blur the bitmap using GPU acceleration
 * @param percent 0-100
 */
fun blurBitmapHardware(source: Bitmap, percent: Int): Bitmap {
    val clampedPercent = percent.coerceIn(0, 100)
    if (clampedPercent == 0) {
        return source
    }

    val maxBlurRadius = Constants.MAX_BLUR_RADIUS
    val radius = (clampedPercent / 100.0f) * maxBlurRadius

    val imageReader = ImageReader.newInstance(
        source.width, source.height,
        PixelFormat.RGBA_8888, 1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )

    val renderNode = RenderNode("BlurEffect")
    val hardwareRenderer = HardwareRenderer()

    var resultBitmap: Bitmap? = null
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
        // Use mutable=true to allow subsequent effects to modify in-place
        resultBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("Failed to copy hardware bitmap to software bitmap")

        hardwareBitmap.recycle()
        hardwareBuffer.close()
        image.close()

    } catch (e: Exception) {
        Log.e(TAG, "Error blurring bitmap: $e")
        return source
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM blurring bitmap: $e")
        return source
    } finally {
        hardwareRenderer.destroy()
        renderNode.discardDisplayList()
        imageReader.close()
    }

    return resultBitmap ?: throw IllegalStateException("Blur operation failed - resultBitmap is null")
}

/**
 * Blur bitmap using GPU acceleration
 */
fun blurBitmap(source: Bitmap, percent: Int): Bitmap {
    return blurBitmapHardware(source, percent)
}

/**
 * Apply a vignette effect to the bitmap
 * @param percent 0-100
 */
fun vignetteBitmap(source: Bitmap, percent: Int): Bitmap {
    if (percent <= 0) return source
    if (!source.isMutable) {
        Log.w(TAG, "vignetteBitmap received an immutable bitmap. Creating a mutable copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        source.recycle()
        return vignetteBitmap(mutableCopy, percent)
    }

    return try {
        val canvas = Canvas(source)
        val dim = if (source.width < source.height) source.height else source.width
        val rad = (dim * (1 - (percent.coerceIn(0, 100) / Constants.VIGNETTE_DIVISOR))).coerceAtLeast(Constants.VIGNETTE_MIN_RADIUS)
        val centerX = source.width / 2f
        val centerY = source.height / 2f

        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.argb((Constants.VIGNETTE_INNER_ALPHA * 255).toInt(), 0, 0, 0),
            Color.argb((Constants.VIGNETTE_OUTER_ALPHA * 255).toInt(), 0, 0, 0)
        )
        val pos = Constants.VIGNETTE_GRADIENT_POSITIONS

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
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM applying vignette: $e")
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
        Log.w(TAG, "grayscaleBitmap received an immutable bitmap. Creating a mutable copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        source.recycle()
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
 * Calculate brightness estimate of bitmap (0.0 - 1.0)
 * Uses luminance calculation based on RGB values
 */
fun calculateBitmapBrightness(bitmap: Bitmap): Float {
    // Sample pixels to estimate brightness (sampling for performance)
    val sampleSize = Constants.BRIGHTNESS_SAMPLE_SIZE
    var totalLuminance = 0.0
    var pixelCount = 0

    for (x in 0 until bitmap.width step sampleSize) {
        for (y in 0 until bitmap.height step sampleSize) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel) / 255.0
            val g = Color.green(pixel) / 255.0
            val b = Color.blue(pixel) / 255.0

            // Calculate relative luminance using ITU-R BT.709 standard
            val luminance = Constants.LUMINANCE_RED * r + Constants.LUMINANCE_GREEN * g + Constants.LUMINANCE_BLUE * b
            totalLuminance += luminance
            pixelCount++
        }
    }

    return if (pixelCount > 0) {
        (totalLuminance / pixelCount).toFloat()
    } else {
        0.5f
    }
}

/**
 * Adjust bitmap brightness by a multiplier
 * @param brightnessFactor Brightness multiplier (e.g., 0.7 to darken, 1.5 to brighten)
 */
fun adjustBitmapBrightness(source: Bitmap, brightnessFactor: Float): Bitmap {
    if (brightnessFactor == 1.0f) return source
    if (!source.isMutable) {
        Log.w(TAG, "adjustBitmapBrightness received an immutable bitmap. Returning a copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        source.recycle() // Recycle source to prevent memory leak
        return adjustBitmapBrightness(mutableCopy, brightnessFactor)
    }

    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setScale(brightnessFactor, brightnessFactor, brightnessFactor, 1f)
        })
    }
    Canvas(source).drawBitmap(source, 0f, 0f, paint)
    return source
}

/**
 * Adaptive brightness adjustment based on system dark/light mode
 * In dark mode: darkens bright images for better viewing
 * In light mode: brightens dark images for better visibility
 *
 * @param context Application context
 * @param source Source bitmap
 * @return Adjusted bitmap (or original if no adjustment needed)
 */
/**
 * Get adaptive brightness multiplier factor based on system dark/light mode
 * 
 * @param context Application context
 * @param brightness Current image brightness (0.0 to 1.0)
 * @return Multiplier factor to apply to colors
 */
fun getAdaptiveBrightnessMultiplier(context: Context, brightness: Float): Float {
    val isDarkMode = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

    val lightBrightnessMin = Constants.LIGHT_BRIGHTNESS_MIN
    val darkBrightnessMax = Constants.DARK_BRIGHTNESS_MAX
    val targetBrightnessDark = Constants.TARGET_BRIGHTNESS_DARK
    val targetBrightnessLight = Constants.TARGET_BRIGHTNESS_LIGHT

    // Avoid issues with very dark items
    if (brightness < 0.01f) return 1.0f

    return when {
        // In dark mode with very bright image: darken it
        isDarkMode && brightness > lightBrightnessMin -> targetBrightnessDark / brightness
        // In light mode with very dark image: brighten it
        !isDarkMode && brightness < darkBrightnessMax -> targetBrightnessLight / brightness
        // No adjustment needed
        else -> 1.0f
    }
}

fun adaptiveBrightnessAdjustment(context: Context, source: Bitmap): Bitmap {
    val isDarkMode = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val currentBrightness = calculateBitmapBrightness(source)

// Thresholds from WallYou
    val lightBrightnessMin = Constants.LIGHT_BRIGHTNESS_MIN
    val darkBrightnessMax = Constants.DARK_BRIGHTNESS_MAX
    val targetBrightnessDark = Constants.TARGET_BRIGHTNESS_DARK
    val targetBrightnessLight = Constants.TARGET_BRIGHTNESS_LIGHT

    // Avoid division by zero or very small numbers
    if (currentBrightness < 0.01f) {
        Log.d(TAG, "Image is too dark for adaptive brightness adjustment (brightness: $currentBrightness)")
        return source
    }

    return when {
        // In dark mode with very bright image: darken it
        isDarkMode && currentBrightness > lightBrightnessMin -> {
            val adjustmentFactor = targetBrightnessDark / currentBrightness
            Log.d(TAG, "Dark mode: Darkening bright image from $currentBrightness to $targetBrightnessDark")
            adjustBitmapBrightness(source, adjustmentFactor)
        }
        // In light mode with very dark image: brighten it
        !isDarkMode && currentBrightness < darkBrightnessMax -> {
            val adjustmentFactor = targetBrightnessLight / currentBrightness
            Log.d(TAG, "Light mode: Brightening dark image from $currentBrightness to $targetBrightnessLight")
            adjustBitmapBrightness(source, adjustmentFactor)
        }
        // No adjustment needed
        else -> {
            Log.d(TAG, "No adaptive brightness adjustment needed (brightness: $currentBrightness, dark mode: $isDarkMode)")
            source
        }
    }
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
 * Process bitmap with all effects - now respects enable flags
 * Properly manages bitmap lifecycle by recycling intermediate results
 */
fun processBitmap(
    context: Context,
    source: Bitmap,
    enableDarken: Boolean = false,
    darkenPercent: Int = 0,
    enableBlur: Boolean = false,
    blurPercent: Int = 0,
    enableVignette: Boolean = false,
    vignettePercent: Int = 0,
    enableGrayscale: Boolean = false,
    grayscalePercent: Int = 0
): Bitmap {
    var result = source

    // Apply effects in order, respecting enable flags
    // Recycle intermediate bitmaps to prevent memory leaks
    
    // Darken modifies in-place for mutable bitmaps, no need to track previous
    if (enableDarken && darkenPercent > 0) {
        result = darkenBitmap(result, darkenPercent)
    }

    // Blur always creates a new bitmap
    if (enableBlur && blurPercent > 0) {
        val previous = result
        result = blurBitmap(result, blurPercent)
        // Recycle the previous one if different from source
        if (result !== previous && previous !== source) {
            previous.recycle()
        }
    }


    if (enableVignette && vignettePercent > 0) {
        val previous = result
        result = vignetteBitmap(result, vignettePercent)
        // Vignette modifies in-place for mutable bitmaps, may create copy for immutable
        if (result !== previous && previous !== source) {
            previous.recycle()
        }
    }

    if (enableGrayscale && grayscalePercent > 0) {
        val previous = result
        result = grayscaleBitmap(result, grayscalePercent)
        // Grayscale modifies in-place for mutable bitmaps, may create copy for immutable
        if (result !== previous && previous !== source) {
            previous.recycle()
        }
    }

    return result
}

/**
 * Detect media type from URI
 *
 * Examines the file extension and MIME type to determine the media type.
 * Returns null if the media type cannot be determined or is unsupported.
 */
fun Uri.detectMediaType(context: Context): WallpaperMediaType? {
    try {
        // First try to get file extension from path
        val path = this.path
        if (path != null) {
            val extension = path.substringAfterLast('.', "")
            if (extension.isNotEmpty()) {
                val mediaType = WallpaperMediaType.fromExtension(extension)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Fallback: Try to get MIME type from content resolver
        val mimeType = context.contentResolver.getType(this)
        if (mimeType != null) {
            return when {
                mimeType.startsWith("image/") -> WallpaperMediaType.IMAGE
                else -> null
            }
        }

        // If all else fails, assume IMAGE for backward compatibility
        return WallpaperMediaType.IMAGE
    } catch (e: Exception) {
        Log.e(TAG, "Error detecting media type for URI: $this", e)
        return WallpaperMediaType.IMAGE  // Default to IMAGE on error
    }
}

/**
 * Validate that media type is supported in the current wallpaper mode
 */
fun WallpaperMediaType.isSupportedInMode(mode: com.anthonyla.paperize.core.WallpaperMode): Boolean {
    return when (mode) {
        com.anthonyla.paperize.core.WallpaperMode.STATIC -> this.supportedInStaticMode
        com.anthonyla.paperize.core.WallpaperMode.LIVE -> this.supportedInLiveMode
    }
}

/**
 * Check if Paperize live wallpaper is currently active/selected
 * Returns true if the Paperize live wallpaper service is the current wallpaper
 */
fun isPaperizeLiveWallpaperActive(context: Context): Boolean {
    return try {
        // If we are checking from within the service itself (e.g. preview mode), we are active
        if (context is com.anthonyla.paperize.service.livewallpaper.PaperizeLiveWallpaperService) {
            Log.d(TAG, "isPaperizeLiveWallpaperActive: called from service context, returning true")
            return true
        }

        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperInfo = wallpaperManager.wallpaperInfo

        // If wallpaperInfo is null, user has a static wallpaper (not a live wallpaper)
        if (wallpaperInfo == null) {
            Log.d(TAG, "isPaperizeLiveWallpaperActive: wallpaperInfo is null (static wallpaper), returning false")
            return false
        }

        // Check if the service component matches Paperize live wallpaper
        val expectedComponent = android.content.ComponentName(
            context.packageName,
            "com.anthonyla.paperize.service.livewallpaper.PaperizeLiveWallpaperService"
        )
        val isPaperize = wallpaperInfo.component == expectedComponent

        Log.d(TAG, "isPaperizeLiveWallpaperActive: current=${wallpaperInfo.component}, expected=$expectedComponent, match=$isPaperize")
        isPaperize
    } catch (e: Exception) {
        Log.e(TAG, "Error checking live wallpaper status", e)
        false
    }
}
