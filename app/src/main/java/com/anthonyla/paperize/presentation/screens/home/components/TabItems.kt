package com.anthonyla.paperize.presentation.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

data class TabItem(
    val title: String,
    val unfilledIcon: ImageVector,
    val filledIcon: ImageVector
)

fun getTabItems(
    wallpaperTitle: String,
    libraryTitle: String
): List<TabItem> = listOf(
    TabItem(
        title = wallpaperTitle,
        unfilledIcon = Icons.Outlined.Image,
        filledIcon = Icons.Filled.Image
    ),
    TabItem(
        title = libraryTitle,
        unfilledIcon = Icons.Outlined.PhotoLibrary,
        filledIcon = Icons.Filled.PhotoLibrary
    )
)
