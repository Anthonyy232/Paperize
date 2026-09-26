package com.anthonyla.paperize.domain.model

import com.anthonyla.paperize.core.constants.Constants

data class WallpaperEffects(
    val enableBlur: Boolean = false,
    val blurPercentage: Int = Constants.DEFAULT_BLUR_PERCENTAGE,
    val enableDarken: Boolean = false,
    val darkenPercentage: Int = Constants.DEFAULT_DARKEN_PERCENTAGE,
    val enableVignette: Boolean = false,
    val vignettePercentage: Int = Constants.DEFAULT_VIGNETTE_PERCENTAGE,
    val enableGrayscale: Boolean = false,
    val grayscalePercentage: Int = Constants.DEFAULT_GRAYSCALE_PERCENTAGE,

    // Interactive effects (live wallpaper mode only)
    val enableDoubleTap: Boolean = false,
    val enableChangeOnScreenOff: Boolean = false,
    val enableParallax: Boolean = false,
    val parallaxIntensity: Int = Constants.DEFAULT_PARALLAX_INTENSITY
) {
    fun validate(): WallpaperEffects = copy(
        darkenPercentage = darkenPercentage.coerceIn(0, 100),
        blurPercentage = blurPercentage.coerceIn(0, 100),
        vignettePercentage = vignettePercentage.coerceIn(0, 100),
        grayscalePercentage = grayscalePercentage.coerceIn(0, 100),
        parallaxIntensity = parallaxIntensity.coerceIn(0, 100)
    )
}
