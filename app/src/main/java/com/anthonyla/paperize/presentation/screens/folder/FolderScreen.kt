package com.anthonyla.paperize.presentation.screens.folder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWallpaper: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewModel = hiltViewModel()
) {
    val folder by viewModel.folder.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: stringResource(R.string.folders)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        folder?.let { currentFolder ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${currentFolder.wallpaperCount} ${stringResource(R.string.wallpapers)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(Constants.GRID_COLUMNS),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentFolder.sortedWallpapers, key = { it.id }) { wallpaper ->
                        Card(
                            onClick = { onNavigateToWallpaper(wallpaper.id) },
                            modifier = Modifier.aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = wallpaper.uri,
                                contentDescription = wallpaper.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
