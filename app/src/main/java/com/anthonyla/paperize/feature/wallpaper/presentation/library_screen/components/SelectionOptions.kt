package com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components

import androidx.compose.ui.graphics.vector.ImageVector

data class SelectionOptions(
    val id: String = "",
    val text: String = "",
    val subText: String? = "",
    val icon: ImageVector? = null
)