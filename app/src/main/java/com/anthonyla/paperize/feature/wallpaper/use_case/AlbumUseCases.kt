package com.anthonyla.paperize.feature.wallpaper.use_case

data class AlbumUseCases(
    val getAlbums: GetAlbums,
    val addAlbum: AddAlbum,
    val addImage: AddImage,
    val deleteAlbum: DeleteAlbum,
    val deleteImage: DeleteImage
)