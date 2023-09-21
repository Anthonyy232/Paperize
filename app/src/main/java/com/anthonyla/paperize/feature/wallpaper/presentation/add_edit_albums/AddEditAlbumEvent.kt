package com.anthonyla.paperize.feature.wallpaper.presentation.add_edit_albums

import android.net.Uri

sealed class AddEditAlbumEvent{
    data class AddTitle(val value: String): AddEditAlbumEvent()
    data class AddImages(val images: List<Uri>)
    object SaveAlbum: AddEditAlbumEvent()
}