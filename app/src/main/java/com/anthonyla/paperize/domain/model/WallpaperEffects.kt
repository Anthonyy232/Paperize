package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.constants.Constants

/**
 * Domain model for Wallpaper Effects
 *
 * Represents the visual effects applied to wallpapers
 */
data class WallpaperEffects(
    val darkenPercentage: Int = Constants.DEFAULT_DARKEN_PERCENTAGE,
    val blurPercentage: Int = Constants.DEFAULT_BLUR_PERCENTAGE,
    val vignettePercentage: Int = Constants.DEFAULT_VIGNETTE_PERCENTAGE,
    val grayscale: Boolean = false
) {
    /**
     * Check if any effects are applied
     */
    val hasEffects: Boolean
        get() = darkenPercentage > 0 || blurPercentage > 0 || vignettePercentage > 0 || grayscale

    /**
     * Validate effect percentages
     */
    fun validate(): WallpaperEffects = copy(
        darkenPercentage = darkenPercentage.coerceIn(
            Constants.MIN_EFFECT_PERCENTAGE,
            Constants.MAX_EFFECT_PERCENTAGE
        ),
        blurPercentage = blurPercentage.coerceIn(
            Constants.MIN_EFFECT_PERCENTAGE,
            Constants.MAX_EFFECT_PERCENTAGE
        ),
        vignettePercentage = vignettePercentage.coerceIn(
            Constants.MIN_EFFECT_PERCENTAGE,
            Constants.MAX_EFFECT_PERCENTAGE
        )
    )

    companion object {
        fun none() = WallpaperEffects()
    }
}
