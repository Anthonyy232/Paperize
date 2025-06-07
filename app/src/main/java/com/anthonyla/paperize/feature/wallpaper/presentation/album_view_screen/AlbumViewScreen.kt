package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.AddAlbumAnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components.AlbumViewTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun AlbumViewScreen(
    albumScreenViewModel: AlbumScreenViewModel,
    animate: Boolean,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String, String) -> Unit,
    onShowFolderView: (Folder) -> Unit,
    onDeleteAlbum: () -> Unit,
    onSortClick: (List<Folder>, List<Wallpaper>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyGridState()

    val albumViewState by albumScreenViewModel.state.collectAsStateWithLifecycle()

    val album = remember(albumViewState.albums, albumViewState.initialAlbumName) {
        albumViewState.albums.find { it.album.initialAlbumName == albumViewState.initialAlbumName }
    }

    val colorScheme = MaterialTheme.colorScheme
    val scrollbarSettings = remember {
        ScrollbarSettings.Default.copy(
            thumbUnselectedColor = colorScheme.primary,
            thumbSelectedColor = colorScheme.primary,
            thumbShape = RoundedCornerShape(16.dp),
            scrollbarPadding = 1.dp,
        )
    }

    val commonItemModifier = remember {
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
    }

    BackHandler(
        enabled = albumViewState.selectionState.selectedCount > 0,
        onBack = remember(albumViewState.isLoading, albumScreenViewModel) {
            {
                if (!albumViewState.isLoading) {
                    albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                }
            }
        }
    )

    /** Image picker **/
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = remember(albumScreenViewModel, scope, context) {
            { uris: List<Uri> ->
                scope.launch(Dispatchers.IO) {
                    albumScreenViewModel.onEvent(AlbumViewEvent.SetLoading(true))
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    val uriList = uris.mapNotNull { uri ->
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                        if (persistedUriPermissions.any { it.uri == uri }) uri.toString() else null
                    }
                    if (uriList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            albumScreenViewModel.onEvent(AlbumViewEvent.AddWallpapers(uriList))
                        }
                    }
                    albumScreenViewModel.onEvent(AlbumViewEvent.SetLoading(false))
                }
            }
        }
    )

    /** Folder picker **/
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = remember(albumScreenViewModel, scope, context) {
            { uri: Uri? ->
                scope.launch(Dispatchers.IO) {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    if (uri != null) {
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                        if (persistedUriPermissions.any { it.uri == uri }) {
                            albumScreenViewModel.onEvent(AlbumViewEvent.AddFolder(uri.toString()))
                        }
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            val onSelectAllClick = remember(
                albumViewState.isLoading,
                albumViewState.selectionState.allSelected,
                albumScreenViewModel
            ) {
                {
                    if (!albumViewState.isLoading) {
                        if (!albumViewState.selectionState.allSelected) albumScreenViewModel.onEvent(
                            AlbumViewEvent.SelectAll
                        )
                        else albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    }
                }
            }

            val onBackClickTopBar = remember(albumScreenViewModel, onBackClick) {
                {
                    albumScreenViewModel.onEvent(AlbumViewEvent.Reset)
                    onBackClick()
                }
            }

            val onDeleteSelected = remember(
                albumViewState.isLoading,
                album,
                albumViewState.selectionState.selectedCount,
                albumScreenViewModel,
                onBackClick
            ) {
                {
                    if (!albumViewState.isLoading) {
                        if (album != null) {
                            if (album.folders.size + album.wallpapers.size == albumViewState.selectionState.selectedCount) {
                                onBackClick()
                            }
                            albumScreenViewModel.onEvent(AlbumViewEvent.DeleteSelected)
                        }
                    }
                }
            }

            val onDeleteAlbumTopBar = remember(
                albumViewState.isLoading,
                album,
                albumScreenViewModel,
                onDeleteAlbum
            ) {
                {
                    if (!albumViewState.isLoading && album != null) {
                        albumScreenViewModel.onEvent(AlbumViewEvent.DeleteAlbum)
                        onDeleteAlbum()
                    }
                }
            }

            val onTitleChangeTopBar: (String) -> Unit = remember(
                albumViewState.isLoading,
                album,
                albumScreenViewModel
            ) {
                { newTitle ->
                    if (!albumViewState.isLoading && album != null) {
                        albumScreenViewModel.onEvent(AlbumViewEvent.ChangeAlbumName(newTitle))
                    }
                }
            }

            val onSortClickTopBar = remember(
                albumViewState.isLoading,
                album,
                onSortClick
            ) {
                {
                    if (!albumViewState.isLoading && album != null) {
                        onSortClick(album.folders, album.wallpapers)
                    }
                }
            }

            AlbumViewTopBar(
                title = album?.album?.displayedAlbumName ?: "",
                allSelected = albumViewState.selectionState.allSelected,
                selectedCount = albumViewState.selectionState.selectedCount,
                selectionMode = albumViewState.selectionState.selectedCount > 0,
                isLoad = albumViewState.isLoading,
                onSelectAllClick = onSelectAllClick,
                onBackClick = onBackClickTopBar,
                onDeleteSelected = onDeleteSelected,
                onDeleteAlbum = onDeleteAlbumTopBar,
                onTitleChange = onTitleChangeTopBar,
                onSortClick = onSortClickTopBar
            )
        },
        floatingActionButton = {
            val onImageClickFab = remember(albumScreenViewModel, imagePickerLauncher) {
                {
                    albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    imagePickerLauncher.launch(arrayOf("image/*"))
                }
            }
            val onFolderClickFab = remember(albumScreenViewModel, folderPickerLauncher) {
                {
                    albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    folderPickerLauncher.launch(null)
                }
            }

            AddAlbumAnimatedFab(
                isLoading = albumViewState.isLoading,
                animate = animate,
                onImageClick = onImageClickFab,
                onFolderClick = onFolderClickFab
            )
        },
        bottomBar = {
            if (albumViewState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        content = { paddingValues ->
            if (album != null) {
                LazyVerticalGridScrollbar(
                    state = lazyListState,
                    settings = scrollbarSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    LazyVerticalGrid(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            items = album.folders,
                            key = { folder -> folder.folderUri }
                        ) { folder ->
                            val isSelected = albumViewState.selectionState.selectedFolders.contains(folder.folderUri)
                            val onFolderItemSelection = remember(
                                folder.folderUri,
                                albumViewState.isLoading,
                                albumViewState.selectionState.selectedFolders
                            ) {
                                {
                                    if (!albumViewState.isLoading) {
                                        albumScreenViewModel.onEvent(
                                            if (!isSelected) AlbumViewEvent.SelectFolder(folder.folderUri)
                                            else AlbumViewEvent.DeselectFolder(folder.folderUri)
                                        )
                                    }
                                }
                            }

                            val onFolderViewClickItem = remember(folder, albumViewState.isLoading, onShowFolderView) {
                                {
                                    if (!albumViewState.isLoading) {
                                        onShowFolderView(folder)
                                    }
                                }
                            }

                            FolderItem(
                                folder = folder,
                                itemSelected = isSelected,
                                selectionMode = albumViewState.selectionState.selectedCount > 0,
                                onItemSelection = onFolderItemSelection,
                                onFolderViewClick = onFolderViewClickItem,
                                modifier = commonItemModifier
                                    .then(
                                        if (animate) Modifier.animateItem(
                                            placementSpec = tween(
                                                durationMillis = 800,
                                                delayMillis = 0,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) else Modifier
                                    )
                            )
                        }

                        items(
                            items = album.wallpapers,
                            key = { wallpaper -> wallpaper.wallpaperUri }
                        ) { wallpaper ->
                            val isSelected = albumViewState.selectionState.selectedWallpapers.contains(wallpaper.wallpaperUri)
                            val onWallpaperItemSelection = remember(
                                wallpaper.wallpaperUri,
                                albumViewState.isLoading,
                                albumViewState.selectionState.selectedWallpapers
                            ) {
                                {
                                    if (!albumViewState.isLoading) {
                                        albumScreenViewModel.onEvent(
                                            if (!isSelected) AlbumViewEvent.SelectWallpaper(wallpaper.wallpaperUri)
                                            else AlbumViewEvent.DeselectWallpaper(wallpaper.wallpaperUri)
                                        )
                                    }
                                }
                            }

                            val onWallpaperViewClickItem = remember(wallpaper, albumViewState.isLoading, onShowWallpaperView) {
                                {
                                    if (!albumViewState.isLoading)
                                        onShowWallpaperView(wallpaper.wallpaperUri, wallpaper.fileName)
                                }
                            }

                            WallpaperItem(
                                wallpaperUri = wallpaper.wallpaperUri,
                                itemSelected = isSelected,
                                selectionMode = albumViewState.selectionState.selectedCount > 0,
                                onItemSelection = onWallpaperItemSelection,
                                onWallpaperViewClick = onWallpaperViewClickItem,
                                modifier = commonItemModifier
                                    .then(
                                        if (animate) Modifier.animateItem(
                                            placementSpec = tween(
                                                durationMillis = 800,
                                                delayMillis = 0,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }
    )
}