package com.anthonyla.paperize.core.constants

/**
 * Application-wide constants
 */
object Constants {
    // Database
    const val DATABASE_NAME = "paperize_database"
    const val DATABASE_VERSION = 2  // v2: Added mediaType and crop fields to WallpaperEntity

    // DataStore
    const val PREFERENCES_NAME = "paperize_preferences"

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "paperize_channel"
    const val NOTIFICATION_ID = 1

    // Services
    const val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.ACTION_CHANGE_WALLPAPER"
    const val ACTION_RELOAD_WALLPAPER = "com.anthonyla.paperize.ACTION_RELOAD_WALLPAPER"

    // WorkManager
    const val WORK_NAME_HOME = "wallpaper_change_home"
    const val WORK_NAME_LOCK = "wallpaper_change_lock"
    const val WORK_NAME_BOTH = "wallpaper_change_both"
    const val WORK_NAME_LIVE = "wallpaper_change_live"
    const val WORK_NAME_REFRESH = "album_refresh"
    const val WORK_TAG_HOME = "wallpaper_change_home_tag"
    const val WORK_TAG_LOCK = "wallpaper_change_lock_tag"
    const val WORK_TAG_BOTH = "wallpaper_change_both_tag"
    const val WORK_TAG_LIVE = "wallpaper_change_live_tag"
    const val WORK_TAG_REFRESH = "album_refresh_tag"
    const val MAX_WORK_RETRY_ATTEMPTS = 3

    // Intents
    const val EXTRA_SCREEN_TYPE = "screen_type"

    // Wallpaper
    const val DEFAULT_BLUR_PERCENTAGE = 0
    const val DEFAULT_DARKEN_PERCENTAGE = 0
    const val DEFAULT_VIGNETTE_PERCENTAGE = 0
    const val DEFAULT_PARALLAX_INTENSITY = 50
    const val DEFAULT_GRAYSCALE_PERCENTAGE = 0
    const val MAX_EFFECT_PERCENTAGE = 100
    const val SLIDER_EFFECT_STEPS = 99
    const val DIALOG_MESSAGE_MAX_LINES = 10
    const val MIN_EFFECT_PERCENTAGE = 0
    const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L

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
    const val GRID_THUMBNAIL_WIDTH = 300
    const val GRID_THUMBNAIL_HEIGHT = 500
    const val LIST_THUMBNAIL_SIZE = 150
    const val PREVIEW_THUMBNAIL_WIDTH = 600
    const val PREVIEW_THUMBNAIL_HEIGHT = 1200

    // Time conversion
    const val MINUTES_PER_HOUR = 60
    const val MINUTES_PER_DAY = 1440

    // Image processing
    const val MAX_BLUR_RADIUS = 25.0f
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
    const val WALLPAPER_READ_INITIAL_DELAY_MS = 500L  // Initial retry delay (with linear backoff)

    // Input validation
    const val MAX_DAYS_INPUT_LENGTH = 3
    const val MAX_HOURS_MINUTES_INPUT_LENGTH = 2

    // File types
    val SUPPORTED_IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "avif",
        "heic", "heif",  // HEIC/HEIF - Apple's high efficiency format
        "bmp",           // Bitmap - legacy but still used
        "gif",           // GIF - mostly for static images (first frame used)
        "tiff", "tif",   // TIFF - high quality archival format
        "svg"            // SVG - vector graphics (rasterized for wallpaper)
    )

    // Renderer
    /** Crossfade animation duration in milliseconds - consistent across all refresh rates */
    const val CROSSFADE_DURATION_MS = 750f
    const val RELOAD_THROTTLE_MS = 250L
    const val BLUR_MIN_THRESHOLD = 0.01f
    const val PERCENTAGE_DIVISOR = 100f
    const val GL_ES_VERSION = 2
    const val SURFACE_POLL_INTERVAL_MS = 50L

    // Wallpaper loading
    const val MAX_WALLPAPER_LOAD_RETRIES = 10
    const val MAX_QUEUE_REBUILD_ATTEMPTS = 2
}

/**
 * Preference keys for DataStore
 */
object PreferenceKeys {
    // Theme
    const val DARK_MODE = "dark_mode"
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
    const val HOME_ENABLE_GRAYSCALE = "home_enable_grayscale"
    const val HOME_GRAYSCALE = "home_grayscale"

    // Effects - Lock
    const val LOCK_ENABLE_BLUR = "lock_enable_blur"
    const val LOCK_BLUR = "lock_blur"
    const val LOCK_ENABLE_DARKEN = "lock_enable_darken"
    const val LOCK_DARKEN = "lock_darken"
    const val LOCK_ENABLE_VIGNETTE = "lock_enable_vignette"
    const val LOCK_VIGNETTE = "lock_vignette"
    const val LOCK_ENABLE_GRAYSCALE = "lock_enable_grayscale"
    const val LOCK_GRAYSCALE = "lock_grayscale"

    // Interactive Effects - Home (live wallpaper mode only)
    const val HOME_ENABLE_DOUBLE_TAP = "home_enable_double_tap"
    const val HOME_ENABLE_PARALLAX = "home_enable_parallax"
    const val HOME_PARALLAX_INTENSITY = "home_parallax_intensity"

    // Interactive Effects - Lock (live wallpaper mode only)
    const val LOCK_ENABLE_DOUBLE_TAP = "lock_enable_double_tap"
    const val LOCK_ENABLE_PARALLAX = "lock_enable_parallax"
    const val LOCK_PARALLAX_INTENSITY = "lock_parallax_intensity"

    // Live Wallpaper Mode Settings
    const val LIVE_ALBUM_ID = "live_album_id"
    const val LIVE_INTERVAL_MINUTES = "live_interval_minutes"
    const val LIVE_ENABLE_BLUR = "live_enable_blur"
    const val LIVE_BLUR = "live_blur"
    const val LIVE_ENABLE_DARKEN = "live_enable_darken"
    const val LIVE_DARKEN = "live_darken"
    const val LIVE_ENABLE_VIGNETTE = "live_enable_vignette"
    const val LIVE_VIGNETTE = "live_vignette"
    const val LIVE_ENABLE_GRAYSCALE = "live_enable_grayscale"
    const val LIVE_GRAYSCALE = "live_grayscale"
    const val LIVE_ENABLE_DOUBLE_TAP = "live_enable_double_tap"
    const val LIVE_ENABLE_CHANGE_ON_SCREEN_ON = "live_enable_change_on_screen_on"
    const val LIVE_ENABLE_PARALLAX = "live_enable_parallax"
    const val LIVE_PARALLAX_INTENSITY = "live_parallax_intensity"

    // Scaling
    const val HOME_SCALING_TYPE = "home_scaling_type"
    const val LOCK_SCALING_TYPE = "lock_scaling_type"
    const val LIVE_SCALING_TYPE = "live_scaling_type"

    // Current wallpapers
    const val CURRENT_HOME_WALLPAPER_ID = "current_home_wallpaper_id"
    const val CURRENT_LOCK_WALLPAPER_ID = "current_lock_wallpaper_id"

    // Behavior
    const val ADAPTIVE_BRIGHTNESS = "adaptive_brightness"

    // First launch
    const val FIRST_LAUNCH = "first_launch"

    // Wallpaper mode
    const val WALLPAPER_MODE = "wallpaper_mode"
}
