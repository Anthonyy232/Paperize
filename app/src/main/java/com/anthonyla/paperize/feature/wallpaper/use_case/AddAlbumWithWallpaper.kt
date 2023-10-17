package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository

class AddAlbumWithWallpaper(private val repository: AlbumRepository) {
    suspend operator fun invoke(albumWithWallpaper: AlbumWithWallpaper) {
        repository.upsertAlbum(albumWithWallpaper.album)
        albumWithWallpaper.wallpapers.forEach { wallpaper ->
            repository.upsertWallpaper(wallpaper)
        }
    }
}