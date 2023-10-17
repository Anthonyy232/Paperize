package com.anthonyla.paperize.feature.wallpaper.domain.model


data class Folder(
    val initialAlbumName: String,
    val folderUri: String,
    val folderName: String?,
    val coverUri: String?,
    val wallpapers: List<String> = emptyList()
)