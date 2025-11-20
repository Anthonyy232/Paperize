package com.anthonyla.paperize.core.constants

/**
 * Application-wide constants
 */
object Constants {
    // Database
    const val DATABASE_NAME = "paperize_database"
    const val DATABASE_VERSION = 1

    // DataStore
    const val PREFERENCES_NAME = "paperize_preferences"

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "paperize_channel"
    const val NOTIFICATION_ID = 1

    // Services
    const val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.ACTION_CHANGE_WALLPAPER"

    // WorkManager
    const val WORK_NAME_HOME = "wallpaper_change_home"
    const val WORK_NAME_LOCK = "wallpaper_change_lock"
    const val WORK_NAME_BOTH = "wallpaper_change_both"
    const val WORK_NAME_REFRESH = "album_refresh"
    const val WORK_TAG_HOME = "wallpaper_change_home_tag"
    const val WORK_TAG_LOCK = "wallpaper_change_lock_tag"
    const val WORK_TAG_BOTH = "wallpaper_change_both_tag"
    const val WORK_TAG_REFRESH = "album_refresh_tag"
    const val WORK_TAG_IMMEDIATE = "immediate_change"
    const val MAX_WORK_RETRY_ATTEMPTS = 3

    // Intents
    const val EXTRA_SCREEN_TYPE = "screen_type"

    // Wallpaper
    const val DEFAULT_BLUR_PERCENTAGE = 0
    const val DEFAULT_DARKEN_PERCENTAGE = 0
    const val DEFAULT_VIGNETTE_PERCENTAGE = 0
    const val MAX_EFFECT_PERCENTAGE = 100
    const val MIN_EFFECT_PERCENTAGE = 0

    // Scheduling
    const val MIN_INTERVAL_MINUTES = 15
    const val MAX_INTERVAL_MINUTES = 43200  // 30 days in minutes
    const val DEFAULT_INTERVAL_MINUTES = 60

    // UI
    const val ANIMATION_DURATION_LONG_MS = 800  // For item reordering animations
    const val DEBOUNCE_DELAY_MS = 500L
    const val PERMISSION_SCREEN_TRANSITION_DELAY_MS = 300L  // Brief delay for permission screen transitions
    const val SETTINGS_DEBOUNCE_MS = 2000L  // Debounce for settings changes before persisting
    const val WALLPAPER_CHANGE_DEBOUNCE_MS = 2000L
    const val WALLPAPER_ASPECT_RATIO = 9f / 16f  // Standard phone aspect ratio

    // Time conversion
    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 1440

    // Image processing
    const val MAX_BLUR_RADIUS = 25.0f
    const val RGB_MAX_VALUE = 255.0
    const val BRIGHTNESS_SAMPLE_SIZE = 10  // Pixel sample size for brightness calculation
    const val DEFAULT_BRIGHTNESS = 0.5f  // Default brightness fallback

    // Luminance coefficients (ITU-R BT.709 standard)
    const val LUMINANCE_RED = 0.2126
    const val LUMINANCE_GREEN = 0.7152
    const val LUMINANCE_BLUE = 0.0722

    // Adaptive brightness thresholds (based on WallYou implementation)
    const val LIGHT_BRIGHTNESS_MIN = 0.8f  // Threshold for bright images in dark mode
    const val DARK_BRIGHTNESS_MAX = 0.3f  // Threshold for dark images in light mode
    const val TARGET_BRIGHTNESS_DARK = 0.7f  // Target brightness in dark mode
    const val TARGET_BRIGHTNESS_LIGHT = 0.4f  // Target brightness in light mode

    // Vignette effect
    const val VIGNETTE_DIVISOR = 150f  // Radius calculation divisor
    const val VIGNETTE_MIN_RADIUS = 0.1f  // Minimum vignette radius
    const val VIGNETTE_INNER_ALPHA = 0.1f  // Inner alpha for vignette gradient
    const val VIGNETTE_OUTER_ALPHA = 0.8f  // Outer alpha for vignette gradient
    val VIGNETTE_GRADIENT_POSITIONS = floatArrayOf(0f, 0.7f, 1f)  // Vignette gradient positions

    // Wallpaper loading retry
    const val WALLPAPER_READ_MAX_ATTEMPTS = 4  // Maximum retry attempts for wallpaper loading
    const val WALLPAPER_READ_INITIAL_DELAY_MS = 500L  // Initial retry delay (with linear backoff)

    // Render nodes
    const val RENDER_NODE_BLUR = "BlurEffect"  // Identifier for blur render node

    // Input validation
    const val MAX_DAYS_INPUT_LENGTH = 3
    const val MAX_HOURS_MINUTES_INPUT_LENGTH = 2

    // File types
    val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "avif")
}

/**
 * Preference keys for DataStore
 */
object PreferenceKeys {
    // Theme
    const val DARK_MODE = "dark_mode"
    const val AMOLED_THEME = "amoled_theme"
    const val DYNAMIC_THEMING = "dynamic_theming"
    const val ANIMATE = "animate"

    // Scheduling
    const val ENABLE_CHANGER = "enable_changer"
    const val SEPARATE_SCHEDULES = "separate_schedules"
    const val SHUFFLE_ENABLED = "shuffle_enabled"
    const val HOME_ENABLED = "home_enabled"
    const val LOCK_ENABLED = "lock_enabled"
    const val HOME_ALBUM_ID = "home_album_id"
    const val LOCK_ALBUM_ID = "lock_album_id"
    const val HOME_INTERVAL_MINUTES = "home_interval_minutes"
    const val LOCK_INTERVAL_MINUTES = "lock_interval_minutes"

    // Effects - Home
    const val HOME_ENABLE_BLUR = "home_enable_blur"
    const val HOME_BLUR = "home_blur"
    const val HOME_ENABLE_DARKEN = "home_enable_darken"
    const val HOME_DARKEN = "home_darken"
    const val HOME_ENABLE_VIGNETTE = "home_enable_vignette"
    const val HOME_VIGNETTE = "home_vignette"
    const val HOME_GRAYSCALE = "home_grayscale"

    // Effects - Lock
    const val LOCK_ENABLE_BLUR = "lock_enable_blur"
    const val LOCK_BLUR = "lock_blur"
    const val LOCK_ENABLE_DARKEN = "lock_enable_darken"
    const val LOCK_DARKEN = "lock_darken"
    const val LOCK_ENABLE_VIGNETTE = "lock_enable_vignette"
    const val LOCK_VIGNETTE = "lock_vignette"
    const val LOCK_GRAYSCALE = "lock_grayscale"

    // Scaling
    const val HOME_SCALING_TYPE = "home_scaling_type"
    const val LOCK_SCALING_TYPE = "lock_scaling_type"

    // Current wallpapers
    const val CURRENT_HOME_WALLPAPER_ID = "current_home_wallpaper_id"
    const val CURRENT_LOCK_WALLPAPER_ID = "current_lock_wallpaper_id"

    // Behavior
    const val ADAPTIVE_BRIGHTNESS = "adaptive_brightness"

    // First launch
    const val FIRST_LAUNCH = "first_launch"
}
