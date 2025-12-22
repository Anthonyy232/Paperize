package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.AlbumEntity
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AlbumMapper
 */
class AlbumMapperTest {

    // ============================================================
    // Test: AlbumEntity.toDomainModel()
    // ============================================================

    @Test
    fun `entity toDomainModel maps all fields correctly`() {
        val entity = createAlbumEntity()
        val domain = entity.toDomainModel()
        
        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.coverUri, domain.coverUri)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.modifiedAt, domain.modifiedAt)
        assertTrue(domain.wallpapers.isEmpty())
        assertTrue(domain.folders.isEmpty())
    }

    @Test
    fun `entity toDomainModel with wallpapers includes them`() {
        val entity = createAlbumEntity()
        val wallpapers = listOf(
            createWallpaper(id = "w1"),
            createWallpaper(id = "w2")
        )
        
        val domain = entity.toDomainModel(wallpapers = wallpapers)
        
        assertEquals(2, domain.wallpapers.size)
        assertEquals("w1", domain.wallpapers[0].id)
        assertEquals("w2", domain.wallpapers[1].id)
    }

    @Test
    fun `entity toDomainModel with folders includes them`() {
        val entity = createAlbumEntity()
        val folders = listOf(
            createFolder(id = "f1"),
            createFolder(id = "f2")
        )
        
        val domain = entity.toDomainModel(folders = folders)
        
        assertEquals(2, domain.folders.size)
        assertEquals("f1", domain.folders[0].id)
        assertEquals("f2", domain.folders[1].id)
    }

    @Test
    fun `entity toDomainModel with wallpapers and folders includes both`() {
        val entity = createAlbumEntity()
        val wallpapers = listOf(createWallpaper(id = "w1"))
        val folders = listOf(createFolder(id = "f1"))
        
        val domain = entity.toDomainModel(wallpapers = wallpapers, folders = folders)
        
        assertEquals(1, domain.wallpapers.size)
        assertEquals(1, domain.folders.size)
    }

    @Test
    fun `entity with null coverUri maps correctly`() {
        val entity = createAlbumEntity(coverUri = null)
        val domain = entity.toDomainModel()
        
        assertNull(domain.coverUri)
    }

    // ============================================================
    // Test: Album.toEntity()
    // ============================================================

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val domain = createAlbumDomain()
        val entity = domain.toEntity()
        
        assertEquals(domain.id, entity.id)
        assertEquals(domain.name, entity.name)
        assertEquals(domain.coverUri, entity.coverUri)
        assertEquals(domain.createdAt, entity.createdAt)
        assertEquals(domain.modifiedAt, entity.modifiedAt)
    }

    @Test
    fun `domain toEntity does not include nested collections`() {
        val domain = createAlbumDomain().copy(
            wallpapers = listOf(createWallpaper(id = "w1")),
            folders = listOf(createFolder(id = "f1"))
        )
        val entity = domain.toEntity()
        
        // Entity should only have album fields, not nested collections
        assertEquals(domain.id, entity.id)
        assertEquals(domain.name, entity.name)
    }

    // ============================================================
    // Test: List extensions
    // ============================================================

    @Test
    fun `list of entities toDomainModels maps all items`() {
        val entities = listOf(
            createAlbumEntity(id = "a1", name = "Album 1"),
            createAlbumEntity(id = "a2", name = "Album 2"),
            createAlbumEntity(id = "a3", name = "Album 3")
        )
        
        val domains = entities.toDomainModels()
        
        assertEquals(3, domains.size)
        assertEquals("a1", domains[0].id)
        assertEquals("a2", domains[1].id)
        assertEquals("a3", domains[2].id)
    }

    @Test
    fun `empty list toDomainModels returns empty list`() {
        val entities = emptyList<AlbumEntity>()
        val domains = entities.toDomainModels()
        
        assertTrue(domains.isEmpty())
    }

    // ============================================================
    // Test: Round-trip conversion
    // ============================================================

    @Test
    fun `entity to domain to entity preserves all data`() {
        val original = createAlbumEntity()
        val converted = original.toDomainModel().toEntity()
        
        assertEquals(original.id, converted.id)
        assertEquals(original.name, converted.name)
        assertEquals(original.coverUri, converted.coverUri)
        assertEquals(original.createdAt, converted.createdAt)
        assertEquals(original.modifiedAt, converted.modifiedAt)
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun createAlbumEntity(
        id: String = "album-1",
        name: String = "Test Album",
        coverUri: String? = "content://cover/image.jpg"
    ) = AlbumEntity(
        id = id,
        name = name,
        coverUri = coverUri,
        createdAt = 1000L,
        modifiedAt = 2000L
    )

    private fun createAlbumDomain() = Album(
        id = "album-1",
        name = "Test Album",
        coverUri = "content://cover/image.jpg",
        createdAt = 1000L,
        modifiedAt = 2000L
    )

    private fun createWallpaper(id: String) = Wallpaper.empty(id = id, albumId = "album-1")
    
    private fun createFolder(id: String) = Folder.empty(id = id, albumId = "album-1")
}
