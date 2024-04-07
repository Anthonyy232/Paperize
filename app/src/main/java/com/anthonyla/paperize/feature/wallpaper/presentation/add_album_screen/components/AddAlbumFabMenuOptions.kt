package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components.SelectionOptions

/**
 * Options for the FAB menu in the Add Album screen
 */
data class AddAlbumFabMenuOptions (
    val imageOption: SelectionOptions = SelectionOptions("add_images", "Add Image", null, Icons.Filled.PhotoLibrary),
    val folderOption: SelectionOptions = SelectionOptions("add_folder", "Add Folder", null, Icons.Filled.Folder)
)