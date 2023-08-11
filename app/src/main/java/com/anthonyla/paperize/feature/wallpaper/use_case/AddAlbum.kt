package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository

class AddAlbum(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(album: Album) : Long {
        return repository.insertAlbum(album)
    }
}