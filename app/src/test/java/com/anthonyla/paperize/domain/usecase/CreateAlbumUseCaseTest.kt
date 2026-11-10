package com.anthonyla.paperize.domain.usecase

import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.testAlbum
import com.anthonyla.paperize.testAlbumSummary
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CreateAlbumUseCaseTest {
    private val albums = mockk<AlbumRepository>()
    private val createAlbum = CreateAlbumUseCase(albums)

    @Test fun `a name that differs only by case is rejected without creating an album`() = runTest {
        every { albums.getAlbumSummaries() } returns flowOf(listOf(testAlbumSummary("nature", "Nature")))
        coEvery { albums.createAlbum("Trips", any()) } returns testAlbum("trips", "Trips")

        assertFalse(createAlbum("nature"))
        coVerify(exactly = 0) { albums.createAlbum(any(), any()) }

        assertTrue(createAlbum("Trips"))
        coVerify(exactly = 1) { albums.createAlbum("Trips", any()) }
    }
}
