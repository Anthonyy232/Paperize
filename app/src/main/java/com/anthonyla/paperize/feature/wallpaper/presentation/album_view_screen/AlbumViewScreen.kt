package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent
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
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (Folder) -> Unit,
    onDeleteAlbum: () -> Unit,
    onSortClick: (List<Folder>, List<Wallpaper>)  -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyGridState()
    val albumViewState = albumScreenViewModel.state.collectAsStateWithLifecycle()
    val album = albumViewState.value.albums.find { it.album.initialAlbumName == albumViewState.value.initialAlbumName }

    BackHandler(
        enabled = albumViewState.value.selectionState.selectedCount > 0,
        onBack = {
            if (!albumViewState.value.isLoading) {
                albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
            }
        }
    )

    /** Image picker **/
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
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
    )

    /** Folder picker **/
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
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
    )

    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = album?.album?.displayedAlbumName ?: "" ,
                allSelected = albumViewState.value.selectionState.allSelected,
                selectedCount = albumViewState.value.selectionState.selectedCount,
                selectionMode = albumViewState.value.selectionState.selectedCount > 0,
                isLoad = albumViewState.value.isLoading,
                onSelectAllClick = {
                    if (!albumViewState.value.isLoading) {
                        if (!albumViewState.value.selectionState.allSelected) albumScreenViewModel.onEvent(
                            AlbumViewEvent.SelectAll
                        )
                        else albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    }
                },
                onBackClick = {
                    albumScreenViewModel.onEvent(AlbumViewEvent.Reset)
                    onBackClick()
                },
                onDeleteSelected = {
                    if (!albumViewState.value.isLoading) {
                        if (album != null) {
                            if (album.folders.size + album.wallpapers.size == albumViewState.value.selectionState.selectedCount) {
                                onBackClick()
                            }
                            albumScreenViewModel.onEvent(AlbumViewEvent.DeleteSelected)
                        }
                    }
                },
                onDeleteAlbum = {
                    if (!albumViewState.value.isLoading && album != null) {
                        albumScreenViewModel.onEvent(AlbumViewEvent.DeleteAlbum)
                        onDeleteAlbum()
                    }
                },
                onTitleChange = {
                    if (!albumViewState.value.isLoading && album != null) {
                        albumScreenViewModel.onEvent(AlbumViewEvent.ChangeAlbumName(it))
                    }
                },
                onSortClick = {
                    if (!albumViewState.value.isLoading && album != null) {
                        onSortClick(album.folders, album.wallpapers)
                    }
                }
            )
        },
        floatingActionButton = {
            AddAlbumAnimatedFab(
                isLoading = albumViewState.value.isLoading,
                animate = animate,
                onImageClick = {
                    albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    imagePickerLauncher.launch(arrayOf("image/*"))
                },
                onFolderClick = {
                    albumScreenViewModel.onEvent(AlbumViewEvent.DeselectAll)
                    folderPickerLauncher.launch(null)
                }
            )
        },
        bottomBar = {
            if (albumViewState.value.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        content = { it ->
            if (album != null) {
                LazyVerticalGridScrollbar(
                    state = lazyListState,
                    settings = ScrollbarSettings.Default.copy(
                        thumbUnselectedColor = MaterialTheme.colorScheme.primary,
                        thumbSelectedColor = MaterialTheme.colorScheme.primary,
                        thumbShape = RoundedCornerShape(16.dp),
                        scrollbarPadding = 1.dp,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                ) {
                    LazyVerticalGrid(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(4.dp, 4.dp),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        items(count = album.folders.size, key = { index -> album.folders[index].folderUri }) { index ->
                            FolderItem(
                                folder = album.folders[index],
                                itemSelected = albumViewState.value.selectionState.selectedFolders.contains(album.folders[index].folderUri),
                                selectionMode = albumViewState.value.selectionState.selectedCount > 0,
                                onItemSelection = {
                                    if (!albumViewState.value.isLoading) {
                                        albumScreenViewModel.onEvent(
                                            if (!albumViewState.value.selectionState.selectedFolders.contains(album.folders[index].folderUri))
                                                AlbumViewEvent.SelectFolder(album.folders[index].folderUri)
                                            else {
                                                AlbumViewEvent.DeselectFolder(album.folders[index].folderUri)
                                            }
                                        )
                                    }
                                },
                                onFolderViewClick = {
                                    if (!albumViewState.value.isLoading) {
                                        onShowFolderView(album.folders[index])
                                    }
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                            )
                        }
                        items(count = album.wallpapers.size, key = { index -> album.wallpapers[index].wallpaperUri }) { index ->
                            WallpaperItem(
                                wallpaperUri = album.wallpapers[index].wallpaperUri,
                                itemSelected = albumViewState.value.selectionState.selectedWallpapers.contains(album.wallpapers[index].wallpaperUri),
                                selectionMode = albumViewState.value.selectionState.selectedCount > 0,
                                onItemSelection = {
                                    if (!albumViewState.value.isLoading) {
                                        albumScreenViewModel.onEvent(
                                            if (!albumViewState.value.selectionState.selectedWallpapers.contains(
                                                    album.wallpapers[index].wallpaperUri
                                                )
                                            ) AlbumViewEvent.SelectWallpaper(album.wallpapers[index].wallpaperUri)
                                            else AlbumViewEvent.DeselectWallpaper(album.wallpapers[index].wallpaperUri)
                                        )
                                    }
                                },
                                onWallpaperViewClick = {
                                    if (!albumViewState.value.isLoading)
                                        onShowWallpaperView(album.wallpapers[index].wallpaperUri)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(150.dp, 350.dp)
                            )
                        }
                    }

                }
            }
        }
    )
}