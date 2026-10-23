package com.anthonyla.paperize.core.constants

object Constants {
    // Database
    const val DATABASE_NAME = "paperize_database"
    const val DATABASE_VERSION = 4

    // DataStore
    const val PREFERENCES_NAME = "paperize_preferences"

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "paperize_channel"
    const val NOTIFICATION_ID = 1

    // Services
    const val ACTION_RELOAD_WALLPAPER = "com.anthonyla.paperize.ACTION_RELOAD_WALLPAPER"

    // WorkManager
    const val WORK_NAME_HOME = "wallpaper_change_home"
    const val WORK_NAME_LOCK = "wallpaper_change_lock"
    const val WORK_NAME_BOTH = "wallpaper_change_both"
    const val WORK_NAME_LIVE = "wallpaper_change_live"
    const val WORK_NAME_REFRESH = "album_refresh"
    const val MAX_WORK_RETRY_ATTEMPTS = 3

    // Intents
    const val EXTRA_SCREEN_TYPE = "screen_type"
    const val EXTRA_INTERVAL_MINUTES = "interval_minutes"

    // Alarms (static rotation below WorkManager's 15-minute floor)
    const val ACTION_STATIC_ALARM = "com.anthonyla.paperize.ACTION_STATIC_ALARM"

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

    // Scheduling
    const val MIN_LIVE_INTERVAL_MINUTES = 1

    /** Static rotation is driven by AlarmManager, so it is not bound by the WorkManager floor. */
    const val MIN_STATIC_INTERVAL_MINUTES = 1

    /** WorkManager rejects periodic work shorter than this; shorter static intervals use alarms. */
    const val MIN_WORKMANAGER_INTERVAL_MINUTES = 15

    const val MAX_INTERVAL_MINUTES = 43200  // 30 days in minutes
    const val DEFAULT_INTERVAL_MINUTES = 60

    // UI
    const val ANIMATION_DURATION_LONG_MS = 800  // For item reordering animations
    const val DEBOUNCE_DELAY_MS = 500L
    const val SETTINGS_DEBOUNCE_MS = 2000L
    const val WALLPAPER_ASPECT_RATIO = 9f / 16f
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
    const val BRIGHTNESS_SAMPLE_SIZE = 10

    // Luminance coefficients (ITU-R BT.709 standard)
    const val LUMINANCE_RED = 0.2126
    const val LUMINANCE_GREEN = 0.7152
    const val LUMINANCE_BLUE = 0.0722

    // Adaptive brightness thresholds (based on WallYou implementation)
    const val LIGHT_BRIGHTNESS_MIN = 0.8f
    const val DARK_BRIGHTNESS_MAX = 0.3f
    const val TARGET_BRIGHTNESS_DARK = 0.7f
    const val TARGET_BRIGHTNESS_LIGHT = 0.4f

    // Vignette effect
    const val VIGNETTE_DIVISOR = 150f
    const val VIGNETTE_MIN_RADIUS = 1f  // guards against zero-radius RadialGradient crash
    const val VIGNETTE_INNER_ALPHA = 0.1f
    const val VIGNETTE_OUTER_ALPHA = 0.8f
    val VIGNETTE_GRADIENT_POSITIONS = floatArrayOf(0f, 0.7f, 1f)

    // Input validation
    const val MAX_DAYS_INPUT_LENGTH = 3
    const val MAX_HOURS_MINUTES_INPUT_LENGTH = 2

    // File types
    val SUPPORTED_IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "avif", "heic", "heif", "bmp", "gif", "tiff", "tif", "svg"
    )

    // Renderer
    const val CROSSFADE_DURATION_MS = 750f
    const val RELOAD_THROTTLE_MS = 250L
    const val BLUR_MIN_THRESHOLD = 0.01f
    const val PERCENTAGE_DIVISOR = 100f
    const val GL_ES_VERSION = 2

    // Wallpaper loading
    const val MAX_WALLPAPER_LOAD_RETRIES = 10
}

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

    // Live Wallpaper Mode Settings (effect keys are built in SettingsRepositoryImpl)
    const val LIVE_ALBUM_ID = "live_album_id"
    const val LIVE_INTERVAL_MINUTES = "live_interval_minutes"

    // Scaling
    const val HOME_SCALING_TYPE = "home_scaling_type"
    const val LOCK_SCALING_TYPE = "lock_scaling_type"
    const val LIVE_SCALING_TYPE = "live_scaling_type"
    const val HOME_SCROLLING_ENABLED = "home_scrolling_enabled"

    const val ADAPTIVE_BRIGHTNESS = "adaptive_brightness"
    const val FIRST_LAUNCH = "first_launch"
    const val WALLPAPER_MODE = "wallpaper_mode"
}
