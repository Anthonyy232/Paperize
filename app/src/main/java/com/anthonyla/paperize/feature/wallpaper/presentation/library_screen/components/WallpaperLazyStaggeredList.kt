package com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components

import androidx.compose.runtime.Composable

@Composable
fun WallpaperLazyStaggeredList() {
    /*
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
     */
}