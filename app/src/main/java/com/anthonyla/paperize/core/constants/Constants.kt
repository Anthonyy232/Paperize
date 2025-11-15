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
    const val NOTIFICATION_CHANNEL_NAME = "Paperize"
    const val NOTIFICATION_ID = 1

    // Services
    const val WALLPAPER_SERVICE_NAME = "WallpaperChangeService"
    const val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.ACTION_CHANGE_WALLPAPER"
    const val ACTION_UPDATE_WALLPAPER = "com.anthonyla.paperize.ACTION_UPDATE_WALLPAPER"
    const val ACTION_REFRESH_ALBUM = "com.anthonyla.paperize.ACTION_REFRESH_ALBUM"
    const val ACTION_CANCEL = "com.anthonyla.paperize.ACTION_CANCEL"

    // Alarm
    const val ALARM_REQUEST_CODE_HOME = 1001
    const val ALARM_REQUEST_CODE_LOCK = 1002
    const val ALARM_REQUEST_CODE_REFRESH = 1003

    // Intents
    const val EXTRA_SCREEN_TYPE = "screen_type"
    const val EXTRA_ALBUM_ID = "album_id"
    const val EXTRA_CHANGE_IMMEDIATE = "change_immediate"

    // Wallpaper
    const val DEFAULT_BLUR_PERCENTAGE = 0
    const val DEFAULT_DARKEN_PERCENTAGE = 0
    const val DEFAULT_VIGNETTE_PERCENTAGE = 0
    const val MAX_EFFECT_PERCENTAGE = 100
    const val MIN_EFFECT_PERCENTAGE = 0

    // Scheduling
    const val MIN_INTERVAL_MINUTES = 15
    const val DEFAULT_INTERVAL_MINUTES = 60
    const val REFRESH_INTERVAL_HOURS = 24

    // UI
    const val GRID_COLUMNS = 3
    const val ANIMATION_DURATION_MS = 300
    const val DEBOUNCE_DELAY_MS = 500L

    // Permissions
    const val REQUEST_CODE_PERMISSIONS = 100

    // File types
    val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "avif")
    val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/avif")

    // Error messages
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_FILE_NOT_FOUND = "File not found"
    const val ERROR_INVALID_URI = "Invalid URI"
    const val ERROR_WALLPAPER_SET_FAILED = "Failed to set wallpaper"
    const val ERROR_DATABASE_ERROR = "Database error occurred"

    // Privacy
    const val PRIVACY_POLICY_URL = "https://github.com/Anthonyy232/Paperize/blob/master/PRIVACY.md"
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
    const val SCHEDULE_START_TIME = "schedule_start_time"
    const val USE_START_TIME = "use_start_time"
    const val HOME_NEXT_CHANGE_TIME = "home_next_change_time"
    const val LOCK_NEXT_CHANGE_TIME = "lock_next_change_time"

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

    // Unified Effects (for effects screen)
    const val ENABLE_BLUR = "enable_blur"
    const val BLUR_PERCENTAGE = "blur_percentage"
    const val ENABLE_DARKEN = "enable_darken"
    const val DARKEN_PERCENTAGE = "darken_percentage"
    const val ENABLE_VIGNETTE = "enable_vignette"
    const val VIGNETTE_PERCENTAGE = "vignette_percentage"
    const val ENABLE_GRAYSCALE = "enable_grayscale"

    // Scaling
    const val HOME_SCALING_TYPE = "home_scaling_type"
    const val LOCK_SCALING_TYPE = "lock_scaling_type"

    // Current wallpapers
    const val CURRENT_HOME_WALLPAPER_ID = "current_home_wallpaper_id"
    const val CURRENT_LOCK_WALLPAPER_ID = "current_lock_wallpaper_id"

    // Behavior
    const val SKIP_LANDSCAPE = "skip_landscape"
    const val SKIP_NON_INTERACTIVE = "skip_non_interactive"

    // First launch
    const val FIRST_LAUNCH = "first_launch"
}
