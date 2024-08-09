package com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import javax.inject.Inject

/**
 * ViewModel for the folder view screen to hold the folder name and the list of wallpapers
 */
class FolderViewModel @Inject constructor (): ViewModel() {
    var folderName = mutableStateOf<String?>(null)
    var wallpapers = mutableStateOf<List<String>?>(null)
}