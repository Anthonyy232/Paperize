package com.anthonyla.paperize.feature.wallpaper.presentation.library_screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components.AnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components.FabMenuOptions
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.WallpaperState
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.components.WallpaperItem
import okhttp3.internal.toImmutableList

@Composable
fun LibraryScreen(
    wallpaperState: WallpaperState,
    settingsState: SettingsState,
    onLaunchImagePhotoPicker: () -> Unit,
    onSelectionMode: (Boolean) -> Unit,
    onUpdateItemCount: (Int) -> Unit,
    onAllSelected: (Boolean) -> Unit,
    selectAll: Boolean,
    selectAllDone: () -> Unit,
    deleteImages: Boolean,
    onDeleteImagesClick: (List<String>) -> Unit
) {
    val lazyListState = rememberLazyStaggeredGridState()

    var selectedImageUris by rememberSaveable(Unit) { mutableStateOf(listOf<String>()) }
    var inSelectionMode by rememberSaveable { mutableStateOf(false) }

    /*
    If user selects all images available for deletion, add it to the list and clear the UI state
     */
    if (selectAll) {
        selectedImageUris = selectedImageUris.toMutableList().apply {
            clear()
            wallpaperState.wallpapers.forEach { add(it.imageUri) }
        }.toImmutableList()
        onUpdateItemCount(selectedImageUris.size)
        selectAllDone()
        onAllSelected(true)
    }

    /*
    If user deletes the selected item,
    pass the list back for deletion and clear the UI state
     */
    if (deleteImages) {
        onDeleteImagesClick(selectedImageUris)
        selectedImageUris = selectedImageUris.toMutableList().apply {
            clear()
        }.toImmutableList()
        onUpdateItemCount(0)
        inSelectionMode = false
        onSelectionMode(false)
        onAllSelected(selectedImageUris.size >= wallpaperState.wallpapers.size)
    }

    /*
     If user does a back press while in selection mode,
    turn off selection mode and clear the selected list
     */
    BackHandler(inSelectionMode) {
        selectedImageUris = selectedImageUris.toMutableList().apply {
            clear()
        }.toImmutableList()
        onUpdateItemCount(0)
        inSelectionMode = false
        onSelectionMode(false)
        onAllSelected(selectedImageUris.size >= wallpaperState.wallpapers.size)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            AnimatedVisibility(
                visible =
                !lazyListState.isScrollInProgress || lazyListState.firstVisibleItemScrollOffset < 0,
                enter = scaleIn(tween(400, 50, FastOutSlowInEasing)),
                exit = scaleOut(tween(400, 50, FastOutSlowInEasing)),
            ) {
                AnimatedFab(
                    menuOptions = FabMenuOptions(),
                    onImageClick = onLaunchImagePhotoPicker,
                    onFolderClick = {}
                )
            }
        },
        content = { it
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyVerticalStaggeredGrid(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    columns = StaggeredGridCells.Fixed(3),
                    contentPadding =  PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        items(wallpaperState.wallpapers, key = { it.imageUri })
                        { wallpaper ->
                            WallpaperItem(
                                selected = selectedImageUris.contains(wallpaper.imageUri),
                                selectionMode = inSelectionMode,
                                image = wallpaper.imageUri.toUri(),
                                onSelectionListClick = {
                                    inSelectionMode = true
                                    selectedImageUris = if (selectedImageUris.contains(wallpaper.imageUri)) {
                                        selectedImageUris.toMutableList().apply {
                                            remove(wallpaper.imageUri)
                                        }.toImmutableList()
                                    } else {
                                        selectedImageUris.toMutableList().apply {
                                            add(wallpaper.imageUri)
                                        }.toImmutableList()
                                    }
                                    onSelectionMode(true)
                                    onUpdateItemCount(selectedImageUris.size)
                                    onAllSelected(selectedImageUris.size >= wallpaperState.wallpapers.size)
                                },
                                onSelection = {
                                    selectedImageUris = if (selectedImageUris.contains(wallpaper.imageUri)) {
                                        selectedImageUris.toMutableList().apply {
                                            remove(wallpaper.imageUri)
                                        }.toImmutableList()
                                    } else {
                                        selectedImageUris.toMutableList().apply {
                                            add(wallpaper.imageUri)
                                        }.toImmutableList()
                                    }
                                    onUpdateItemCount(selectedImageUris.size)
                                    onAllSelected(selectedImageUris.size >= wallpaperState.wallpapers.size)
                                }
                            )
                        }
                    },
                )
            }
        }
    )
}