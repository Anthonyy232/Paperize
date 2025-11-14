package com.anthonyla.paperize.presentation.screens.album_view

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.screens.album_view.components.AlbumViewTopBar
import com.anthonyla.paperize.presentation.screens.album_view.components.FolderItem
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem

@Composable
fun AlbumViewScreen(
    albumId: String,
    onBackClick: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToWallpaperView: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyGridState()

    val album by viewModel.album.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()

    val colorScheme = MaterialTheme.colorScheme

    val commonItemModifier = remember {
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    // Handle permission failure
                }
            }
            viewModel.addWallpapers(uris.map { it.toString() })
        }
    }

    // Folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                viewModel.addFolder(it.toString())
            } catch (e: Exception) {
                // Handle permission failure
            }
        }
    }

    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = album?.name ?: "",
                onBackClick = onBackClick,
                onSortClick = { /* TODO: Implement sort */ },
                onDeleteAlbum = { viewModel.deleteAlbum(); onBackClick() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_wallpapers)
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Folders
            items(
                items = folders,
                key = { folder -> folder.id }
            ) { folder ->
                FolderItem(
                    folder = folder,
                    onClick = { onNavigateToFolder(folder.id) },
                    modifier = commonItemModifier
                        .then(
                            Modifier.animateItem(
                                placementSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        )
                )
            }

            // Wallpapers
            items(
                items = wallpapers,
                key = { wallpaper -> wallpaper.id }
            ) { wallpaper ->
                WallpaperItem(
                    wallpaperUri = wallpaper.uri,
                    onClick = { onNavigateToWallpaperView(wallpaper.uri, wallpaper.fileName) },
                    modifier = commonItemModifier
                        .then(
                            Modifier.animateItem(
                                placementSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 0,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        )
                )
            }
        }
    }
}
