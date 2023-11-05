package com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.FolderItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.components.AlbumViewTopBar

@Composable
fun AlbumViewScreen(
    albumWithWallpaper: AlbumWithWallpaper,
    onBackClick: () -> Unit,
    onShowWallpaperView: (String) -> Unit,
    onShowFolderView: (String, String?, List<String>) -> Unit,
    onDeleteAlbum: () -> Unit,
    onTitleChange: (String, AlbumWithWallpaper) -> Unit

) {
    val lazyListState = rememberLazyStaggeredGridState()
    BackHandler { onBackClick() }
    Scaffold(
        topBar = {
            AlbumViewTopBar(
                title = albumWithWallpaper.album.displayedAlbumName,
                onBackClick = { onBackClick() },
                onDeleteAlbum = onDeleteAlbum,
                onTitleChange = { onTitleChange(it, albumWithWallpaper) }
            )
        },
        content = {
            LazyVerticalStaggeredGrid(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp, 4.dp),
                horizontalArrangement = Arrangement.Start,
                content = {
                    items (items = albumWithWallpaper.folders, key = { folder -> folder.folderUri.hashCode()}
                    ) { folder ->
                        FolderItem(
                            folder = folder,
                            itemSelected = false,
                            selectionMode = false,
                            onActivateSelectionMode = {},
                            onItemSelection = {},
                            onFolderViewClick = {
                                onShowFolderView(folder.folderUri, folder.folderName, folder.wallpapers)
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    items (items = albumWithWallpaper.wallpapers, key = { wallpaper -> wallpaper.wallpaperUri.hashCode()}
                    ) { wallpaper ->
                        WallpaperItem(
                            wallpaperUri = wallpaper.wallpaperUri,
                            itemSelected = false,
                            selectionMode = false,
                            onActivateSelectionMode = {},
                            onItemSelection = {},
                            onWallpaperViewClick = {
                                onShowWallpaperView(wallpaper.wallpaperUri)
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            )
        }
    )
}