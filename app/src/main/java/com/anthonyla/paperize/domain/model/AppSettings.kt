package com.anthonyla.paperize.domain.model

data class AppSettings(
    val darkMode: Boolean? = null,  // null = system default, true = dark, false = light
    val dynamicTheming: Boolean = false,
    val animate: Boolean = true,
    val firstLaunch: Boolean = true
) {
}
