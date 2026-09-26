package com.anthonyla.paperize.core

enum class ScalingType {
    /**
     * Fill the screen, cropping if necessary
     */
    FILL,

    /**
     * Fit the entire image, adding letterboxing/pillarboxing if necessary
     */
    FIT,

    STRETCH,

    /**
     * Display at original size
     */
    NONE;

    companion object {
        fun fromString(value: String?): ScalingType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: FILL
        }
    }
}
