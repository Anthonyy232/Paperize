package com.anthonyla.paperize.core.util

import com.anthonyla.paperize.core.ScalingType
import kotlin.math.roundToInt

internal fun calculateDecodeSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
    scaling: ScalingType
): Pair<Int, Int> {
    val widthRatio = targetWidth.toFloat() / sourceWidth
    val heightRatio = targetHeight.toFloat() / sourceHeight
    val scale = when (scaling) {
        ScalingType.FILL -> maxOf(widthRatio, heightRatio)
        ScalingType.FIT -> minOf(widthRatio, heightRatio)
        // Keep native size unless decoding would exceed twice the screen size.
        ScalingType.NONE -> minOf(1f, widthRatio * 2, heightRatio * 2)
        ScalingType.STRETCH -> return targetWidth to targetHeight
    }
    return (sourceWidth * scale).roundToInt().coerceAtLeast(1) to
        (sourceHeight * scale).roundToInt().coerceAtLeast(1)
}
