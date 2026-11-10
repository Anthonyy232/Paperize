package com.anthonyla.paperize.core

enum class ScalingType {
    FILL,
    FIT,
    STRETCH,
    NONE;

    companion object {
        fun fromString(value: String?): ScalingType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: FILL
        }
    }
}
