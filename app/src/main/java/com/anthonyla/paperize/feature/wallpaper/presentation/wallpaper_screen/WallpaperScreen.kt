package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumState

@Composable
fun WallpaperScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            }
        }
    )
}
