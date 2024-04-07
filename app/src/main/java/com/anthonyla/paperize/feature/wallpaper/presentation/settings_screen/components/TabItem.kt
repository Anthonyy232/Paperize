package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing a tab item
 */
data class TabItem(
    val title: String,
    val filledIcon: ImageVector,
    val unfilledIcon: ImageVector,
)

/**
 * List of tab items
 */
val tabItems = listOf(
    TabItem(
        title = "Wallpaper",
        filledIcon = Icons.Filled.Image,
        unfilledIcon = Icons.Outlined.Image
    ),
    TabItem(
        title = "Library",
        filledIcon = Icons.Filled.AddToPhotos,
        unfilledIcon = Icons.Outlined.AddToPhotos
    )
)