package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.data.database.entities.FolderEntity
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FolderMapper
 */
class FolderMapperTest {

    // ============================================================
    // Test: FolderEntity.toDomainModel()
    // ============================================================

    @Test
    fun `entity toDomainModel maps all fields correctly`() {
        val entity = createFolderEntity()
        val domain = entity.toDomainModel()
        
        assertEquals(entity.id, domain.id)
        assertEquals(entity.albumId, domain.albumId)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.uri, domain.uri)
        assertEquals(entity.coverUri, domain.coverUri)
        assertEquals(entity.dateModified, domain.dateModified)
        assertEquals(entity.displayOrder, domain.displayOrder)
        assertEquals(entity.addedAt, domain.addedAt)
        assertTrue(domain.wallpapers.isEmpty())
    }

    @Test
    fun `entity toDomainModel with wallpapers includes them`() {
        val entity = createFolderEntity()
        val wallpapers = listOf(
            createWallpaper(id = "w1"),
            createWallpaper(id = "w2")
        )
        
        val domain = entity.toDomainModel(wallpapers)
        
        assertEquals(2, domain.wallpapers.size)
        assertEquals("w1", domain.wallpapers[0].id)
        assertEquals("w2", domain.wallpapers[1].id)
    }

    @Test
    fun `entity with null coverUri maps correctly`() {
        val entity = createFolderEntity(coverUri = null)
        val domain = entity.toDomainModel()
        
        assertNull(domain.coverUri)
    }

    @Test
    fun `entity with coverUri maps correctly`() {
        val entity = createFolderEntity(coverUri = "content://cover/image.jpg")
        val domain = entity.toDomainModel()
        
        assertEquals("content://cover/image.jpg", domain.coverUri)
    }

    // ============================================================
    // Test: Folder.toEntity()
    // ============================================================

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val domain = createFolderDomain()
        val entity = domain.toEntity()
        
        assertEquals(domain.id, entity.id)
        assertEquals(domain.albumId, entity.albumId)
        assertEquals(domain.name, entity.name)
        assertEquals(domain.uri, entity.uri)
        assertEquals(domain.coverUri, entity.coverUri)
        assertEquals(domain.dateModified, entity.dateModified)
        assertEquals(domain.displayOrder, entity.displayOrder)
        assertEquals(domain.addedAt, entity.addedAt)
    }

    @Test
    fun `domain toEntity does not include wallpapers`() {
        val domain = createFolderDomain().copy(
            wallpapers = listOf(createWallpaper(id = "w1"))
        )
        val entity = domain.toEntity()
        
        // Entity should not have wallpapers - they're stored separately
        assertEquals(domain.id, entity.id)
    }

    // ============================================================
    // Test: List extensions
    // ============================================================

    @Test
    fun `list of entities toDomainModels maps all items`() {
        val entities = listOf(
            createFolderEntity(id = "f1"),
            createFolderEntity(id = "f2"),
            createFolderEntity(id = "f3")
        )
        
        val domains = entities.toDomainModels()
        
        assertEquals(3, domains.size)
        assertEquals("f1", domains[0].id)
        assertEquals("f2", domains[1].id)
        assertEquals("f3", domains[2].id)
    }

    @Test
    fun `empty list toDomainModels returns empty list`() {
        val entities = emptyList<FolderEntity>()
        val domains = entities.toDomainModels()
        
        assertTrue(domains.isEmpty())
    }

    // ============================================================
    // Test: Round-trip conversion
    // ============================================================

    @Test
    fun `entity to domain to entity preserves all data`() {
        val original = createFolderEntity()
        val converted = original.toDomainModel().toEntity()
        
        assertEquals(original.id, converted.id)
        assertEquals(original.albumId, converted.albumId)
        assertEquals(original.name, converted.name)
        assertEquals(original.uri, converted.uri)
        assertEquals(original.coverUri, converted.coverUri)
        assertEquals(original.dateModified, converted.dateModified)
        assertEquals(original.displayOrder, converted.displayOrder)
        assertEquals(original.addedAt, converted.addedAt)
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun createFolderEntity(
        id: String = "folder-1",
        coverUri: String? = "content://cover/default.jpg"
    ) = FolderEntity(
        id = id,
        albumId = "album-1",
        name = "Test Folder",
        uri = "content://folder/test",
        coverUri = coverUri,
        dateModified = 1000L,
        displayOrder = 0,
        addedAt = 2000L
    )

    private fun createFolderDomain() = Folder(
        id = "folder-1",
        albumId = "album-1",
        name = "Test Folder",
        uri = "content://folder/test",
        coverUri = "content://cover/default.jpg",
        dateModified = 1000L,
        displayOrder = 0,
        addedAt = 2000L
    )

    private fun createWallpaper(id: String) = Wallpaper.empty(id = id, albumId = "album-1")
}
