package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.Image
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository

class AddImage(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(image: Image) {
        repository.insertImage(image)
    }
}