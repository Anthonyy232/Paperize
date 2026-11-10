package com.anthonyla.paperize.core.constants

object Constants {
    const val DATABASE_NAME = "paperize_database"
    const val DATABASE_VERSION = 4

    const val PREFERENCES_NAME = "paperize_preferences"

    const val NOTIFICATION_CHANNEL_ID = "paperize_channel"
    const val NOTIFICATION_ID = 1

    const val WORK_NAME_HOME = "wallpaper_change_home"
    const val WORK_NAME_LOCK = "wallpaper_change_lock"
    const val WORK_NAME_BOTH = "wallpaper_change_both"
    const val WORK_NAME_LIVE = "wallpaper_change_live"
    const val WORK_NAME_REFRESH = "album_refresh"
    const val MAX_WORK_RETRY_ATTEMPTS = 3

    const val EXTRA_SCREEN_TYPE = "screen_type"

    const val DEFAULT_BLUR_PERCENTAGE = 0
    const val DEFAULT_DARKEN_PERCENTAGE = 0
    const val DEFAULT_VIGNETTE_PERCENTAGE = 0
    const val DEFAULT_PARALLAX_INTENSITY = 50
    const val DEFAULT_GRAYSCALE_PERCENTAGE = 0
    const val MAX_EFFECT_PERCENTAGE = 100
    const val SLIDER_EFFECT_STEPS = 99
    const val DIALOG_MESSAGE_MAX_LINES = 10
    const val MIN_EFFECT_PERCENTAGE = 0

    const val MIN_LIVE_INTERVAL_MINUTES = 1
    const val MIN_INTERVAL_MINUTES = 15
    const val MAX_INTERVAL_MINUTES = 43200  // 30 days in minutes
    const val DEFAULT_INTERVAL_MINUTES = 60

    const val ANIMATION_DURATION_LONG_MS = 800
    const val DEBOUNCE_DELAY_MS = 500L
    const val SETTINGS_DEBOUNCE_MS = 2000L
    const val WALLPAPER_ASPECT_RATIO = 9f / 16f
    const val GRID_THUMBNAIL_WIDTH = 300
    const val GRID_THUMBNAIL_HEIGHT = 500
    const val LIST_THUMBNAIL_SIZE = 150
    const val PREVIEW_THUMBNAIL_WIDTH = 600
    const val PREVIEW_THUMBNAIL_HEIGHT = 1200

    const val MAX_BLUR_RADIUS = 25.0f
    const val BRIGHTNESS_SAMPLE_SIZE = 10

    // ITU-R BT.709
    const val LUMINANCE_RED = 0.2126
    const val LUMINANCE_GREEN = 0.7152
    const val LUMINANCE_BLUE = 0.0722

    // Adaptive brightness thresholds follow WallYou.
    const val LIGHT_BRIGHTNESS_MIN = 0.8f
    const val DARK_BRIGHTNESS_MAX = 0.3f
    const val TARGET_BRIGHTNESS_DARK = 0.7f
    const val TARGET_BRIGHTNESS_LIGHT = 0.4f

    const val VIGNETTE_DIVISOR = 150f
    const val VIGNETTE_MIN_RADIUS = 1f  // guards against zero-radius RadialGradient crash
    const val VIGNETTE_INNER_ALPHA = 0.1f
    const val VIGNETTE_OUTER_ALPHA = 0.8f
    val VIGNETTE_GRADIENT_POSITIONS = floatArrayOf(0f, 0.7f, 1f)

    const val MAX_DAYS_INPUT_LENGTH = 3
    const val MAX_HOURS_MINUTES_INPUT_LENGTH = 2

    val SUPPORTED_IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "avif", "heic", "heif", "bmp", "gif", "tiff", "tif"
    )

    const val CROSSFADE_DURATION_MS = 750f
    const val BLUR_MIN_THRESHOLD = 0.01f

    const val MAX_WALLPAPER_LOAD_RETRIES = 10
}
