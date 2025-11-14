package com.anthonyla.paperize.domain.model

/**
 * Domain model for App Settings (theme, preferences, etc.)
 */
data class AppSettings(
    val darkMode: Boolean? = null,  // null = system default, true = dark, false = light
    val amoledTheme: Boolean = false,
    val dynamicTheming: Boolean = true,
    val animate: Boolean = true,
    val firstLaunch: Boolean = true,
    val currentHomeWallpaperId: String? = null,
    val currentLockWallpaperId: String? = null
) {
    companion object {
        fun default() = AppSettings()
    }
}
