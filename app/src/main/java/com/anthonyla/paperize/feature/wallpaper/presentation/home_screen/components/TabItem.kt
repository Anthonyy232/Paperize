package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.anthonyla.paperize.R

/**
 * Data class representing a tab item
 */
data class TabItem(
    val title: String,
    val filledIcon: ImageVector,
    val unfilledIcon: ImageVector,
)

@Composable
fun getTabItems(): List<TabItem> {
    val context = LocalContext.current
    return listOf(
        TabItem(
            title = context.getString(R.string.wallpaper_screen),
            filledIcon = Icons.Filled.Image,
            unfilledIcon = Icons.Outlined.Image
        ),
        TabItem(
            title = context.getString(R.string.library_screen),
            filledIcon = Icons.Filled.AddToPhotos,
            unfilledIcon = Icons.Outlined.AddToPhotos
        )
    )
}