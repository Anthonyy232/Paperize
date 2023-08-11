package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.Image
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository

class DeleteImage(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(image: Image) {
        repository.deleteImage(image)
    }
}