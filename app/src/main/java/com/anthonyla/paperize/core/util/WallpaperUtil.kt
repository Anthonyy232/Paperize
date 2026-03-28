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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
        // Integer division (floor) ensures inSampleSize is never larger than needed,
        // guaranteeing the decoded bitmap is at least as wide/tall as the target.
        // fastRoundToInt() can round UP, producing a bitmap smaller than the target.
        val heightRatio = imageSize.height / height
        val widthRatio = imageSize.width / width
        return minOf(heightRatio, widthRatio).coerceAtLeast(1)
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
 * Get the target render size for a wallpaper bitmap.
 *
 * For the HOME screen (and BOTH), the launcher may request a wider canvas than the physical
 * screen to support horizontal parallax scrolling across multiple home-screen pages (e.g.
 * Pixel Launcher with 3 pages requests ~3× screen width). If we render at physical-screen
 * width and pass a null visibleCropHint to WallpaperManager.setBitmap(), Android scales the
 * bitmap up to fill the launcher's desired width, which also enlarges the height and crops
 * the top/bottom — making "FIT" behave identically to "FILL" from the user's perspective.
 *
 * Fix: use WallpaperManager.getDesiredMinimumWidth/Height as the render target for HOME/BOTH
 * so the bitmap already fills the full parallax canvas. The launcher then has nothing to scale,
 * and the chosen scaling mode (FIT, FILL, STRETCH, NONE) is preserved correctly.
 *
 * LOCK screen wallpapers are not parallax-scrolled, so the physical screen size is correct.
 * LIVE wallpapers are rendered by GLRenderer directly; they do not go through this path.
 */
fun getWallpaperRenderSize(context: Context, screenType: com.anthonyla.paperize.core.ScreenType): Size {
    val screen = getDeviceScreenSize(context)
    return when (screenType) {
        com.anthonyla.paperize.core.ScreenType.HOME,
        com.anthonyla.paperize.core.ScreenType.BOTH -> {
            val wm = WallpaperManager.getInstance(context)
            val desiredW = wm.desiredMinimumWidth
            val desiredH = wm.desiredMinimumHeight
            // desiredMinimumWidth/Height return 0 when the launcher hasn't set a preference yet.
            // Fall back to physical screen size in that case.
            if (desiredW > 0 && desiredH > 0) Size(desiredW, desiredH) else screen
        }
        else -> screen
    }
}

/**
 * Retrieve a bitmap from a URI that is scaled down to the device's screen size.
 *
 * ImageDecoder is the primary path: it auto-applies EXIF orientation and exposes
 * post-EXIF dimensions in the callback via info.size, so we no longer need a
 * separate getImageDimensions() pre-read. The BitmapFactory fallback computes its
 * own inSampleSize inline to avoid an extra stream open on the happy path.
 */
fun retrieveBitmap(
    context: Context,
    wallpaperUri: Uri,
    width: Int,
    height: Int,
    scaling: ScalingType = ScalingType.FIT
): Bitmap? {
    // ImageDecoder auto-applies EXIF orientation; info.size is the post-EXIF display size.
    // No separate getImageDimensions() call needed — saves 1-2 stream opens per wallpaper.
    var decodedWithImageDecoder = false
    val bitmap = try {
        val source = ImageDecoder.createSource(context.contentResolver, wallpaperUri)
        val result = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val srcWidth = info.size.width
            val srcHeight = info.size.height
            val (targetWidth, targetHeight) = when (scaling) {
                ScalingType.FILL -> {
                    val scale = maxOf(width.toFloat() / srcWidth, height.toFloat() / srcHeight)
                    Pair((srcWidth * scale).fastRoundToInt(), (srcHeight * scale).fastRoundToInt())
                }
                ScalingType.FIT -> {
                    val scale = minOf(width.toFloat() / srcWidth, height.toFloat() / srcHeight)
                    Pair((srcWidth * scale).fastRoundToInt(), (srcHeight * scale).fastRoundToInt())
                }
                ScalingType.STRETCH -> Pair(width, height)
                ScalingType.NONE -> {
                    val maxWidth = width * 2
                    val maxHeight = height * 2
                    if (srcWidth > maxWidth || srcHeight > maxHeight) {
                        val scale = minOf(maxWidth.toFloat() / srcWidth, maxHeight.toFloat() / srcHeight)
                        Pair((srcWidth * scale).fastRoundToInt(), (srcHeight * scale).fastRoundToInt())
                    } else {
                        Pair(srcWidth, srcHeight)
                    }
                }
            }
            decoder.setTargetSize(targetWidth, targetHeight)
            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        decodedWithImageDecoder = true
        result
    } catch (e: Exception) {
        Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory: $e")
        try {
            // Compute inSampleSize from a bounds-only pass, then decode in a second pass.
            // Two opens only on the rare fallback path — the primary ImageDecoder path is free.
            val sampleSize = context.contentResolver.openInputStream(wallpaperUri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, opts)
                if (scaling == ScalingType.NONE || opts.outWidth <= 0 || opts.outHeight <= 0) 1
                else calculateInSampleSize(Size(opts.outWidth, opts.outHeight), width, height)
            } ?: 1
            context.contentResolver.openInputStream(wallpaperUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inMutable = true
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                })
            }
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM during BitmapFactory fallback for $wallpaperUri: $oom")
            null
        }
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OOM during retrieveBitmap for $wallpaperUri: $e")
        null
    }

    // Only apply EXIF orientation for the BitmapFactory path — ImageDecoder already handles it
    return if (decodedWithImageDecoder) bitmap else bitmap?.let { applyExifOrientation(it, wallpaperUri, context) }
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
 * Darken the bitmap by a certain percentage
 * @param darkenPercent 0-100 (0 is original brightness, 100 is completely dark/black)
 */
fun darkenBitmap(source: Bitmap, darkenPercent: Int): Bitmap {
    if (!source.isMutable) {
        Log.w(TAG, "darkenBitmap received an immutable bitmap. Creating a mutable copy.")
        val mutableCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        // Do not recycle source here — caller owns the lifecycle and handles recycling
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
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
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

    var resultBitmap: Bitmap?
    try {
        hardwareRenderer.setSurface(imageReader.surface)
        hardwareRenderer.setContentRoot(renderNode)
        renderNode.setPosition(0, 0, source.width, source.height)

        val blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        renderNode.setRenderEffect(blurEffect)

        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(source, 0f, 0f, null)
        renderNode.endRecording()

        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = imageReader.acquireNextImage()
            ?: throw IllegalStateException("Failed to acquire blurred image")

        try {
            val hardwareBuffer = image.hardwareBuffer
                ?: throw IllegalStateException("Failed to acquire hardware buffer")

            try {
                val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                    ?: throw IllegalStateException("Failed to create bitmap from hardware buffer")

                // Convert hardware bitmap to regular bitmap for further processing
                // Use mutable=true to allow subsequent effects to modify in-place
                resultBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    ?: throw IllegalStateException("Failed to copy hardware bitmap to software bitmap")

                hardwareBitmap.recycle()
            } finally {
                hardwareBuffer.close()
            }
        } finally {
            image.close()
        }

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

    return resultBitmap
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
        // Do not recycle source here — caller owns the lifecycle and handles recycling
        return vignetteBitmap(mutableCopy, percent)
    }

    return try {
        val canvas = Canvas(source)
        // Use corner-to-center diagonal so the vignette darkens uniformly in all directions
        val halfW = source.width / 2f
        val halfH = source.height / 2f
        val diagonal = kotlin.math.sqrt(halfW * halfW + halfH * halfH)
        val rad = (diagonal * (1 - (percent.coerceIn(0, 100) / Constants.VIGNETTE_DIVISOR))).coerceAtLeast(Constants.VIGNETTE_MIN_RADIUS)
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
        // Do not recycle source here — caller owns the lifecycle and handles recycling
        return grayscaleBitmap(mutableCopy, percent)
    }

    val factor = percent.coerceIn(0, 100) / 100f
    if (factor <= 0f) return source

    val colorMatrix = ColorMatrix().apply { setSaturation(1 - factor) }
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

    Canvas(source).drawBitmap(source, 0f, 0f, paint)
    return source
}

/**
 * Calculate brightness estimate of bitmap (0.0 - 1.0)
 * Uses luminance calculation based on RGB values
 */
fun calculateBitmapBrightness(bitmap: Bitmap): Float {
    return BrightnessCalculator.calculateBitmapBrightness(bitmap)
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
        // Do not recycle source here — caller owns the lifecycle and handles recycling
        return adjustBitmapBrightness(mutableCopy, brightnessFactor)
    }

    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setScale(brightnessFactor, brightnessFactor, brightnessFactor, 1f)
        })
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    Canvas(source).drawBitmap(source, 0f, 0f, paint)
    return source
}


/**
 * Get adaptive brightness multiplier factor based on system dark/light mode
 * 
 * @param context Application context
 * @param brightness Current image brightness (0.0 to 1.0)
 * @return Multiplier factor to apply to colors
 */
fun getAdaptiveBrightnessMultiplier(context: Context, brightness: Float): Float {
    val isDarkMode = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    
    return BrightnessCalculator.getAdaptiveMultiplier(isDarkMode, brightness)
}

fun adaptiveBrightnessAdjustment(context: Context, source: Bitmap): Bitmap {
    val isDarkMode = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val currentBrightness = BrightnessCalculator.calculateBitmapBrightness(source)
    val adjustmentFactor = BrightnessCalculator.getAdaptiveMultiplier(isDarkMode, currentBrightness)

    return if (adjustmentFactor != 1.0f) {
        Log.d(TAG, "Adaptive brightness adjustment factor: $adjustmentFactor (brightness: $currentBrightness, dark mode: $isDarkMode)")
        adjustBitmapBrightness(source, adjustmentFactor)
    } else {
        Log.d(TAG, "No adaptive brightness adjustment needed (brightness: $currentBrightness, dark mode: $isDarkMode)")
        source
    }
}

/**
 * Process bitmap with all effects - now respects enable flags
 * Properly manages bitmap lifecycle by recycling intermediate results
 *
 * Uses a GPU-accelerated pipeline via [RenderEffect] chaining when possible (API 31+).
 * All colour-filter effects (darken, grayscale) and blur are composed into a single
 * RenderNode draw call so there is only **one** GPU→CPU copy at the end.
 * Vignette is drawn on the same RenderNode canvas before read-back.
 * Falls back to the sequential CPU path on error.
 */
fun processBitmap(
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
    // Fast path – nothing to do
    val hasDarken = enableDarken && darkenPercent > 0
    val hasBlur = enableBlur && blurPercent > 0
    val hasVignette = enableVignette && vignettePercent > 0
    val hasGrayscale = enableGrayscale && grayscalePercent > 0
    if (!hasDarken && !hasBlur && !hasVignette && !hasGrayscale) return source

    // Try GPU-chained path
    try {
        return processBitmapGpu(
            source,
            hasDarken, darkenPercent,
            hasBlur, blurPercent,
            hasVignette, vignettePercent,
            hasGrayscale, grayscalePercent
        )
    } catch (e: Exception) {
        Log.w(TAG, "GPU effects pipeline failed, falling back to CPU: $e")
    } catch (e: OutOfMemoryError) {
        Log.w(TAG, "OOM in GPU effects pipeline, falling back to CPU: $e")
    }

    // CPU fallback
    return processBitmapCpu(
        source,
        hasDarken, darkenPercent,
        hasBlur, blurPercent,
        hasVignette, vignettePercent,
        hasGrayscale, grayscalePercent
    )
}

/**
 * GPU-accelerated effects pipeline.
 *
 * Chains darken, blur, and grayscale as [RenderEffect]s on a single [RenderNode],
 * draws the vignette overlay on the same canvas, and reads back the result once.
 */
private fun processBitmapGpu(
    source: Bitmap,
    hasDarken: Boolean, darkenPercent: Int,
    hasBlur: Boolean, blurPercent: Int,
    hasVignette: Boolean, vignettePercent: Int,
    hasGrayscale: Boolean, grayscalePercent: Int
): Bitmap {
    val w = source.width
    val h = source.height

    val imageReader = ImageReader.newInstance(
        w, h, PixelFormat.RGBA_8888, 1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )
    val renderNode = RenderNode("EffectsChain")
    val hardwareRenderer = HardwareRenderer()

    try {
        hardwareRenderer.setSurface(imageReader.surface)
        hardwareRenderer.setContentRoot(renderNode)
        renderNode.setPosition(0, 0, w, h)

        // --- Build chained RenderEffect (darken → blur → grayscale) ---
        var effect: RenderEffect? = null

        if (hasDarken) {
            val factor = (100 - darkenPercent.coerceIn(0, 100)) / 100f
            val cm = ColorMatrix().apply { setScale(factor, factor, factor, 1f) }
            val darkenEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
            effect = darkenEffect
        }

        if (hasBlur) {
            val radius = (blurPercent.coerceIn(0, 100) / 100f) * Constants.MAX_BLUR_RADIUS
            val blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            effect = if (effect != null) {
                RenderEffect.createChainEffect(blurEffect, effect)
            } else {
                blurEffect
            }
        }

        if (hasGrayscale) {
            val factor = grayscalePercent.coerceIn(0, 100) / 100f
            val cm = ColorMatrix().apply { setSaturation(1 - factor) }
            val gsEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
            effect = if (effect != null) {
                RenderEffect.createChainEffect(gsEffect, effect)
            } else {
                gsEffect
            }
        }

        if (effect != null) {
            renderNode.setRenderEffect(effect)
        }

        // --- Record canvas commands ---
        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(source, 0f, 0f, null)

        // Vignette is an overlay drawn on top of the source in the same pass
        if (hasVignette) {
            val halfW = w / 2f
            val halfH = h / 2f
            val diagonal = kotlin.math.sqrt(halfW * halfW + halfH * halfH)
            val rad = (diagonal * (1 - (vignettePercent.coerceIn(0, 100) / Constants.VIGNETTE_DIVISOR)))
                .coerceAtLeast(Constants.VIGNETTE_MIN_RADIUS)
            val colors = intArrayOf(
                Color.TRANSPARENT,
                Color.argb((Constants.VIGNETTE_INNER_ALPHA * 255).toInt(), 0, 0, 0),
                Color.argb((Constants.VIGNETTE_OUTER_ALPHA * 255).toInt(), 0, 0, 0)
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    halfW, halfH, rad,
                    colors, Constants.VIGNETTE_GRADIENT_POSITIONS, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }

        renderNode.endRecording()

        // --- Single GPU render + single read-back ---
        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = imageReader.acquireNextImage()
            ?: throw IllegalStateException("Failed to acquire image after GPU render")

        try {
            val hwBuffer = image.hardwareBuffer
                ?: throw IllegalStateException("Failed to acquire hardware buffer")
            try {
                val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, null)
                    ?: throw IllegalStateException("Failed to wrap hardware buffer")
                val result = hwBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    ?: throw IllegalStateException("Failed to copy hardware bitmap")
                hwBitmap.recycle()
                return result
            } finally {
                hwBuffer.close()
            }
        } finally {
            image.close()
        }
    } finally {
        hardwareRenderer.destroy()
        renderNode.discardDisplayList()
        imageReader.close()
    }
}

/**
 * CPU fallback: applies effects sequentially (original implementation).
 */
private fun processBitmapCpu(
    source: Bitmap,
    hasDarken: Boolean, darkenPercent: Int,
    hasBlur: Boolean, blurPercent: Int,
    hasVignette: Boolean, vignettePercent: Int,
    hasGrayscale: Boolean, grayscalePercent: Int
): Bitmap {
    var result = source

    if (hasDarken) {
        result = darkenBitmap(result, darkenPercent)
    }

    if (hasBlur) {
        val previous = result
        result = blurBitmap(result, blurPercent)
        if (result !== previous && previous !== source) {
            previous.recycle()
        }
    }

    if (hasVignette) {
        val previous = result
        result = vignetteBitmap(result, vignettePercent)
        if (result !== previous && previous !== source) {
            previous.recycle()
        }
    }

    if (hasGrayscale) {
        val previous = result
        result = grayscaleBitmap(result, grayscalePercent)
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
