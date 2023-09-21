package com.anthonyla.paperize.feature.wallpaper.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.AlbumItem
import com.anthonyla.paperize.feature.wallpaper.use_case.AddImage

@Composable
fun LibraryScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    albumsViewModel: AlbumsViewModel = hiltViewModel()
) {
    /*
      val testAlbum1 = Album(albumName = "hi1", dateAdded = "today1")
                    viewModel.onEvent(AlbumsEvent.AddAlbum(testAlbum1))
                    val image1 = Image(imageName = "image1", dateAdded = "todayImage1")
                    viewModel.onEvent(AlbumsEvent.AddImage(image1))

                    val testAlbum2 = Album(albumName = "hi2", dateAdded = "today2")
                    viewModel.onEvent(AlbumsEvent.AddAlbum(testAlbum2))

                    val image2 = Image(imageName = "image2", dateAdded = "todayImage2")
                    viewModel.onEvent(AlbumsEvent.AddImage(image2))
     */

    val state = albumsViewModel.state.value
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Library")
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.albums) { album ->
                AlbumItem(
                    album = album,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                        },
                    onDeleteClick = {
                        albumsViewModel.onEvent(AlbumsEvent.DeleteAlbum(album))
                        //prompt some sort of message saying _ has been deleted
                    }
                )
            }
        }
    }
}

