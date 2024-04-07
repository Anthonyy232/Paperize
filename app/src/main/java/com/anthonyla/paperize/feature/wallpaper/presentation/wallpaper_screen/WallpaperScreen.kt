package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.AlbumBottomSheet
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentSelectedAlbum

@Composable
fun WallpaperScreen(
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel()
) {
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { it
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CurrentSelectedAlbum(
                    selectedAlbum = selectedState.value.selectedAlbum,
                    onOpenBottomSheet = {
                        if (albumState.value.albumsWithWallpapers.firstOrNull() != null) openBottomSheet = true
                    }
                )
                if (openBottomSheet) {
                    AlbumBottomSheet(
                        albums = albumState.value.albumsWithWallpapers,
                        currentSelectedAlbum = selectedState.value.selectedAlbum,
                        onDismiss = { openBottomSheet = false },
                        onSelect = { album ->
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(album))
                            openBottomSheet = false
                        }
                    )
                }
            }
        }
    )
}


/*
if (albumState.value.albumsWithWallpapers.firstOrNull() != null) {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    val inputStream = albumState.value.albumsWithWallpapers.firstOrNull()?.let {
                        it.album.coverUri?.let { it1 ->
                            context.contentResolver.openInputStream(
                                it1.toUri()
                            )
                        }
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    wallpaperManager.setBitmap(bitmap)
                }
 */