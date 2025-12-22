package com.anthonyla.paperize.data.mapper

import com.anthonyla.paperize.core.WallpaperMediaType
import com.anthonyla.paperize.core.WallpaperSourceType
import com.anthonyla.paperize.data.database.entities.WallpaperEntity
import com.anthonyla.paperize.domain.model.Wallpaper
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WallpaperMapper
 */
class WallpaperMapperTest {

    // ============================================================
    // Test: WallpaperEntity.toDomainModel()
    // ============================================================

    @Test
    fun `entity toDomainModel maps all fields correctly`() {
        val entity = createWallpaperEntity()
        val domain = entity.toDomainModel()
        
        assertEquals(entity.id, domain.id)
        assertEquals(entity.albumId, domain.albumId)
        assertEquals(entity.folderId, domain.folderId)
        assertEquals(entity.uri, domain.uri)
        assertEquals(entity.fileName, domain.fileName)
        assertEquals(entity.dateModified, domain.dateModified)
        assertEquals(entity.displayOrder, domain.displayOrder)
        assertEquals(entity.sourceType, domain.sourceType)
        assertEquals(entity.addedAt, domain.addedAt)
        assertEquals(entity.mediaType, domain.mediaType)
    }

    @Test
    fun `entity with null folderId maps correctly`() {
        val entity = createWallpaperEntity(folderId = null)
        val domain = entity.toDomainModel()
        
        assertNull(domain.folderId)
    }

    @Test
    fun `entity with FOLDER sourceType maps correctly`() {
        val entity = createWallpaperEntity(
            folderId = "folder-123",
            sourceType = WallpaperSourceType.FOLDER
        )
        val domain = entity.toDomainModel()
        
        assertEquals("folder-123", domain.folderId)
        assertEquals(WallpaperSourceType.FOLDER, domain.sourceType)
    }

    // ============================================================
    // Test: Wallpaper.toEntity()
    // ============================================================

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val domain = createWallpaperDomain()
        val entity = domain.toEntity()
        
        assertEquals(domain.id, entity.id)
        assertEquals(domain.albumId, entity.albumId)
        assertEquals(domain.folderId, entity.folderId)
        assertEquals(domain.uri, entity.uri)
        assertEquals(domain.fileName, entity.fileName)
        assertEquals(domain.dateModified, entity.dateModified)
        assertEquals(domain.displayOrder, entity.displayOrder)
        assertEquals(domain.sourceType, entity.sourceType)
        assertEquals(domain.addedAt, entity.addedAt)
        assertEquals(domain.mediaType, entity.mediaType)
    }

    @Test
    fun `domain with null folderId toEntity maps correctly`() {
        val domain = createWallpaperDomain(folderId = null)
        val entity = domain.toEntity()
        
        assertNull(entity.folderId)
    }

    // ============================================================
    // Test: List extensions
    // ============================================================

    @Test
    fun `list of entities toDomainModels maps all items`() {
        val entities = listOf(
            createWallpaperEntity(id = "w1"),
            createWallpaperEntity(id = "w2"),
            createWallpaperEntity(id = "w3")
        )
        
        val domains = entities.toDomainModels()
        
        assertEquals(3, domains.size)
        assertEquals("w1", domains[0].id)
        assertEquals("w2", domains[1].id)
        assertEquals("w3", domains[2].id)
    }

    @Test
    fun `empty list toDomainModels returns empty list`() {
        val entities = emptyList<WallpaperEntity>()
        val domains = entities.toDomainModels()
        
        assertTrue(domains.isEmpty())
    }

    @Test
    fun `list of domains toEntities maps all items`() {
        val domains = listOf(
            createWallpaperDomain(id = "w1"),
            createWallpaperDomain(id = "w2")
        )
        
        val entities = domains.toEntities()
        
        assertEquals(2, entities.size)
        assertEquals("w1", entities[0].id)
        assertEquals("w2", entities[1].id)
    }

    // ============================================================
    // Test: Round-trip conversion
    // ============================================================

    @Test
    fun `entity to domain to entity preserves all data`() {
        val original = createWallpaperEntity()
        val converted = original.toDomainModel().toEntity()
        
        assertEquals(original.id, converted.id)
        assertEquals(original.albumId, converted.albumId)
        assertEquals(original.folderId, converted.folderId)
        assertEquals(original.uri, converted.uri)
        assertEquals(original.fileName, converted.fileName)
        assertEquals(original.dateModified, converted.dateModified)
        assertEquals(original.displayOrder, converted.displayOrder)
        assertEquals(original.sourceType, converted.sourceType)
        assertEquals(original.addedAt, converted.addedAt)
        assertEquals(original.mediaType, converted.mediaType)
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun createWallpaperEntity(
        id: String = "wallpaper-1",
        folderId: String? = null,
        sourceType: WallpaperSourceType = WallpaperSourceType.DIRECT
    ) = WallpaperEntity(
        id = id,
        albumId = "album-1",
        folderId = folderId,
        uri = "content://test/image.jpg",
        fileName = "image.jpg",
        dateModified = 1000L,
        displayOrder = 0,
        sourceType = sourceType,
        addedAt = 2000L,
        mediaType = WallpaperMediaType.IMAGE
    )

    private fun createWallpaperDomain(
        id: String = "wallpaper-1",
        folderId: String? = null
    ) = Wallpaper(
        id = id,
        albumId = "album-1",
        folderId = folderId,
        uri = "content://test/image.jpg",
        fileName = "image.jpg",
        dateModified = 1000L,
        displayOrder = 0,
        sourceType = WallpaperSourceType.DIRECT,
        addedAt = 2000L,
        mediaType = WallpaperMediaType.IMAGE
    )
}
