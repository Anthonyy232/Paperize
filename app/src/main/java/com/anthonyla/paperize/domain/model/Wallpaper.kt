package com.anthonyla.paperize.domain.model

data class Wallpaper(
    val id: String,
    val albumId: String,
    val folderId: String? = null,
    val uri: String,
    val fileName: String,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
) {
    val displayFileName: String
        get() = decodedLastSegment(fileName)
}
