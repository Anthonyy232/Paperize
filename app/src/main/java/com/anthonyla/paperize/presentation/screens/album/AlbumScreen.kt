package com.anthonyla.paperize.presentation.screens.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.common.components.DeleteAlbumDialog
import com.anthonyla.paperize.presentation.common.components.EditAlbumNameDialog
import com.anthonyla.paperize.presentation.common.components.rememberFolderPicker
import com.anthonyla.paperize.presentation.common.components.rememberImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWallpaper: (String) -> Unit,
    onNavigateToFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val imagePicker = rememberImagePicker { uris ->
        viewModel.addWallpapers(uris)
        showFabMenu = false
    }

    val folderPicker = rememberFolderPicker { uri ->
        viewModel.addFolder(uri)
        showFabMenu = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(album?.name ?: stringResource(R.string.library_screen)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAlbum() }) {
                        Icon(Icons.Default.Refresh, "Refresh album")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_name)) },
                            onClick = {
                                showMenu = false
                                showEditNameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_album)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = imagePicker,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Image, stringResource(R.string.add_images))
                    }
                    SmallFloatingActionButton(
                        onClick = folderPicker,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Folder, stringResource(R.string.add_folder))
                    }
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu }
                ) {
                    Icon(
                        if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        }
    ) { paddingValues ->
        album?.let { currentAlbum ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Album info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${currentAlbum.totalWallpaperCount} ${stringResource(R.string.wallpapers)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (currentAlbum.isSelected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.currently_selected_album),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Display wallpapers and folders
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(Constants.GRID_COLUMNS),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Folders
                        items(currentAlbum.folders, key = { it.id }) { folder ->
                            FolderItem(
                                folder = folder,
                                onClick = { onNavigateToFolder(folder.id) }
                            )
                        }

                        // Direct wallpapers
                        items(currentAlbum.wallpapers, key = { it.id }) { wallpaper ->
                            WallpaperItem(
                                wallpaper = wallpaper,
                                onClick = { onNavigateToWallpaper(wallpaper.id) }
                            )
                        }
                    }
                }
            }

            if (showEditNameDialog) {
                EditAlbumNameDialog(
                    currentName = currentAlbum.name,
                    onDismiss = { showEditNameDialog = false },
                    onConfirm = { newName ->
                        viewModel.updateAlbumName(newName)
                        showEditNameDialog = false
                    }
                )
            }

            if (showDeleteDialog) {
                DeleteAlbumDialog(
                    albumName = currentAlbum.name,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.deleteAlbum { onNavigateBack() }
                        showDeleteDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: com.anthonyla.paperize.domain.model.Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            folder.coverUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = folder.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                        Text(
                            text = "${folder.wallpaperCount} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperItem(
    wallpaper: com.anthonyla.paperize.domain.model.Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        AsyncImage(
            model = wallpaper.uri,
            contentDescription = wallpaper.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
