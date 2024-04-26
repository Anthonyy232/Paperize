package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.AlbumBottomSheet
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components.CurrentSelectedAlbum
import kotlinx.coroutines.launch

@Composable
fun WallpaperScreen(
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel(),
    onScheduleWallpaperChanger: (Int) -> Unit
) {
    val context = LocalContext.current
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
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
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SET_WALLPAPER) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.SET_WALLPAPER), 0)
                            }
                            val wallpapers: List<Wallpaper> = album.wallpapers + album.folders.flatMap { folder ->
                                folder.wallpapers.map { wallpaper ->
                                    Wallpaper(
                                        initialAlbumName = album.album.initialAlbumName,
                                        wallpaperUri = wallpaper,
                                        key = wallpaper.hashCode() + album.album.initialAlbumName.hashCode(),
                                    )
                                }
                            }
                            val newSelectedAlbum = SelectedAlbum(
                                album = album.album.copy(
                                    wallpapersInQueue = wallpapers.map { it.wallpaperUri }.shuffled()
                                ),
                                wallpapers = wallpapers
                            )
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(newSelectedAlbum))
                            onScheduleWallpaperChanger(1)
                            openBottomSheet = false
                        }
                    )
                }
            }
        },
    )
}