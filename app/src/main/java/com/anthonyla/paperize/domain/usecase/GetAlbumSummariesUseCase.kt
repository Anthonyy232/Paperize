package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get all album summaries (lightweight metadata + counts)
 * Optimized for displaying lists of albums without loading full wallpaper data
 */
class GetAlbumSummariesUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    operator fun invoke(): Flow<List<AlbumSummary>> = albumRepository.getAlbumSummaries()
}
