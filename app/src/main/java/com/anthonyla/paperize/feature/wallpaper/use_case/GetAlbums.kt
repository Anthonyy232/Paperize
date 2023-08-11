package com.anthonyla.paperize.feature.wallpaper.use_case

import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithImages
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

class GetAlbums(private val repository: AlbumRepository) {
    operator fun invoke(): Flow<List<AlbumWithImages>> {
        return repository.getAlbums()
    }
}


/* for getImages
return repository.getAlbums().map { album ->
            when(albumsOrder.orderType) {
                is OrderType.Ascending -> {
                    when (albumsOrder) {
                        is AlbumsOrder.Date -> album.sortedBy { it.dateAdded.lowercase() }
                    }
                }
                is OrderType.Descending -> {
                    when (albumsOrder) {
                        is AlbumsOrder.Date -> album.sortedBy { it.dateAdded.uppercase() }
                    }
                }
            }
        }
 */