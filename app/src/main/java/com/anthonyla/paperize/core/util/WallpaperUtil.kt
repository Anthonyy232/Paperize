package com.anthonyla.paperize.core.util
import com.anthonyla.paperize.core.constants.Constants

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
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
import android.graphics.RectF
import android.graphics.Shader
import android.hardware.display.DisplayManager
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.anthonyla.paperize.core.ScalingType

private const val TAG = "WallpaperUtil"
private const val BUILT_IN_DISPLAY_CATEGORY =
    "android.hardware.display.category.BUILT_IN_DISPLAYS"

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

fun getDeviceScreenSize(context: Context): Size {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics: WindowMetrics = windowManager.currentWindowMetrics
    val currentWindow = metrics.bounds.width() to metrics.bounds.height()

    // Display.Mode dimensions are reported in the panel's natural orientation. Prefer them
    // over currentWindowMetrics so a scheduled change while a landscape game is foregrounded
    // cannot permanently rotate and over-crop the wallpaper bitmap.
    val displayManager = context.getSystemService(DisplayManager::class.java)
    val builtInDisplays = displayManager
        ?.getDisplays(BUILT_IN_DISPLAY_CATEGORY)
        .orEmpty()
    val candidateDisplays = if (builtInDisplays.isNotEmpty()) {
        // Android 17+ returns all built-in panels, including currently inactive panels.
        builtInDisplays.asSequence()
    } else {
        // Compatibility fallback for releases where the built-in category is unknown.
        // Exclude presentation and app-owned virtual displays.
        displayManager?.displays
            ?.asSequence()
            ?.filter { display ->
                display.flags and Display.FLAG_PRESENTATION == 0 &&
                    display.flags and Display.FLAG_PRIVATE == 0
            }
            .orEmpty()
    }
    val supportedModes = candidateDisplays
        .flatMap { display ->
            display.supportedModes.asSequence().map { mode ->
                mode.physicalWidth to mode.physicalHeight
            }
        }
        .toList()
    val selected = selectWallpaperDisplayDimensions(currentWindow, supportedModes)
    return Size(selected.first, selected.second)
}

internal fun selectWallpaperDisplayDimensions(
    currentWindow: Pair<Int, Int>,
    supportedModes: Iterable<Pair<Int, Int>>
): Pair<Int, Int> = selectLargestDisplayDimensions(supportedModes)
    ?: currentWindow.takeIf { (width, height) -> width > 0 && height > 0 }
    ?: (1 to 1)

internal fun selectLargestDisplayDimensions(
    candidates: Iterable<Pair<Int, Int>>
): Pair<Int, Int>? = candidates
    .filter { (width, height) -> width > 0 && height > 0 }
    .maxWithOrNull(
        compareBy<Pair<Int, Int>>(
            { (width, height) -> width.toLong() * height.toLong() },
            { (width, height) -> maxOf(width, height) },
            { (width, height) -> minOf(width, height) }
        )
    )

/**
 * Whether a static wallpaper should retain source overflow for launcher-managed scrolling.
 *
 * FIT/STRETCH/NONE use an exact launcher canvas so Android cannot rescale them into FILL.
 */
internal fun usesLauncherManagedScrolling(
    screenType: com.anthonyla.paperize.core.ScreenType,
    scaling: ScalingType,
    scrollingEnabled: Boolean
): Boolean =
    scrollingEnabled &&
        (screenType == com.anthonyla.paperize.core.ScreenType.HOME ||
        screenType == com.anthonyla.paperize.core.ScreenType.BOTH) &&
        scaling == ScalingType.FILL

fun retrieveBitmap(
    context: Context,
    wallpaperUri: Uri,
    width: Int,
    height: Int,
    scaling: ScalingType = ScalingType.FIT,
    preserveSourceOverflow: Boolean = false
): Bitmap? {
    // ImageDecoder reports dimensions after applying EXIF orientation.
    var decodedWithImageDecoder = false
    val bitmap = try {
        val source = ImageDecoder.createSource(context.contentResolver, wallpaperUri)
        val result = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (targetW, targetH) = calculateDecodeSize(
                info.size.width, info.size.height, width, height, scaling
            )
            decoder.setTargetSize(targetW, targetH)
            // Crop during decode to avoid allocating fill-scale overflow.
            if (scaling == ScalingType.FILL && !preserveSourceOverflow &&
                (targetW > width || targetH > height)
            ) {
                val cropX = ((targetW - width) / 2).coerceAtLeast(0)
                val cropY = ((targetH - height) / 2).coerceAtLeast(0)
                decoder.setCrop(android.graphics.Rect(cropX, cropY, cropX + width, cropY + height))
            }

            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        decodedWithImageDecoder = true
        result
    } catch (e: Exception) {
        Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory: $e")
        try {
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
    val oriented = if (decodedWithImageDecoder) bitmap else bitmap?.let { applyExifOrientation(it, wallpaperUri, context) }

    // An exact canvas prevents the launcher from undoing the selected scaling mode.
    return oriented?.let {
        if (preserveSourceOverflow && scaling == ScalingType.FILL) {
            scaleToFillPreservingOverflow(it, width, height)
        } else {
            finalizeToCanvas(it, width, height, scaling)
        }
    }
}

/**
 * Scale [source] just enough to cover one screen while retaining any overflow.
 *
 * The returned bitmap may be wider than [width] or taller than [height]. That overflow is what a
 * launcher uses to scroll a static wallpaper without moving past the source image's real edge.
 */
private fun scaleToFillPreservingOverflow(
    source: Bitmap,
    width: Int,
    height: Int
): Bitmap {
    val scale = maxOf(
        width.toFloat() / source.width,
        height.toFloat() / source.height
    )
    val targetW = (source.width * scale).fastRoundToInt()
    val targetH = (source.height * scale).fastRoundToInt()
    if (targetW == source.width && targetH == source.height) return source

    val scaled = source.scale(targetW, targetH)
    if (scaled !== source) source.recycle()
    return scaled
}

/** Render either decoder's output onto the requested canvas; consumes [source]. */
internal fun finalizeToCanvas(source: Bitmap, canvasW: Int, canvasH: Int, scaling: ScalingType): Bitmap {
    if (source.width == canvasW && source.height == canvasH) return source
    val widthRatio = canvasW.toFloat() / source.width
    val heightRatio = canvasH.toFloat() / source.height
    val scale = when (scaling) {
        ScalingType.FILL -> maxOf(widthRatio, heightRatio)
        ScalingType.FIT -> minOf(widthRatio, heightRatio)
        ScalingType.NONE, ScalingType.STRETCH -> 1f
    }
    val width = if (scaling == ScalingType.STRETCH) canvasW.toFloat() else source.width * scale
    val height = if (scaling == ScalingType.STRETCH) canvasH.toFloat() else source.height * scale
    val left = (canvasW - width) / 2f
    val top = (canvasH - height) / 2f
    val result = createBitmap(canvasW, canvasH)
    Canvas(result).apply {
        drawColor(Color.BLACK)
        drawBitmap(source, null, RectF(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG))
    }
    source.recycle()
    return result
}

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

private fun drawVignette(canvas: Canvas, width: Int, height: Int, percent: Int) {
    val centerX = width / 2f
    val centerY = height / 2f
    val radius = (kotlin.math.hypot(centerX, centerY) *
        (1 - percent.coerceIn(0, 100) / Constants.VIGNETTE_DIVISOR))
        .coerceAtLeast(Constants.VIGNETTE_MIN_RADIUS)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((Constants.VIGNETTE_INNER_ALPHA * 255).toInt(), 0, 0, 0),
                Color.argb((Constants.VIGNETTE_OUTER_ALPHA * 255).toInt(), 0, 0, 0)
            ),
            Constants.VIGNETTE_GRADIENT_POSITIONS, Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
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

fun getAdaptiveBrightnessMultiplier(context: Context, brightness: Float): Float {
    val isDarkMode = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return BrightnessCalculator.getAdaptiveMultiplier(isDarkMode, brightness)
}

fun adaptiveBrightnessAdjustment(context: Context, source: Bitmap): Bitmap {
    val brightness = BrightnessCalculator.calculateBitmapBrightness(source)
    return adjustBitmapBrightness(source, getAdaptiveBrightnessMultiplier(context, brightness))
}

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
    val hasDarken = enableDarken && darkenPercent > 0
    val hasBlur = enableBlur && blurPercent > 0
    val hasVignette = enableVignette && vignettePercent > 0
    val hasGrayscale = enableGrayscale && grayscalePercent > 0
    if (!hasDarken && !hasBlur && !hasVignette && !hasGrayscale) return source

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

    return processBitmapCpu(
        source,
        hasDarken, darkenPercent,
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

        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(source, 0f, 0f, null)

        if (hasVignette) {
            drawVignette(canvas, w, h, vignettePercent)
        }

        renderNode.endRecording()

        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        return checkNotNull(imageReader.acquireNextImage()) { "Failed to acquire rendered image" }.use { image ->
            checkNotNull(image.hardwareBuffer) { "Failed to acquire hardware buffer" }.use { buffer ->
                val bitmap = checkNotNull(Bitmap.wrapHardwareBuffer(buffer, null)) { "Failed to wrap hardware buffer" }
                try {
                    checkNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, true)) { "Failed to copy hardware bitmap" }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    } finally {
        hardwareRenderer.destroy()
        renderNode.discardDisplayList()
        imageReader.close()
    }
}

/**
 * Fallback when the GPU pipeline fails. Darken, grayscale and vignette are plain canvas
 * operations; blur requires [RenderEffect], so it is skipped here.
 */
private fun processBitmapCpu(
    source: Bitmap,
    hasDarken: Boolean, darkenPercent: Int,
    hasVignette: Boolean, vignettePercent: Int,
    hasGrayscale: Boolean, grayscalePercent: Int
): Bitmap {
    // The caller owns and recycles [source] if a copy is returned.
    val result = if (source.isMutable) source else source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    if (hasDarken || hasGrayscale) {
        val colorMatrix = ColorMatrix()
        if (hasGrayscale) colorMatrix.setSaturation(1 - grayscalePercent.coerceIn(0, 100) / 100f)
        if (hasDarken) {
            val factor = (100 - darkenPercent.coerceIn(0, 100)) / 100f
            colorMatrix.postConcat(ColorMatrix().apply { setScale(factor, factor, factor, 1f) })
        }
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        canvas.drawBitmap(result, 0f, 0f, paint)
    }
    if (hasVignette) drawVignette(canvas, result.width, result.height, vignettePercent)
    return result
}
