package com.anthonyla.paperize.core

/**
 * Screen types for wallpaper setting
 */
enum class ScreenType {
    HOME,
    LOCK,
    BOTH,
    LIVE;

    companion object {
        fun fromString(value: String?): ScreenType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: BOTH
        }
    }
}
