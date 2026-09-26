package com.anthonyla.paperize.domain.model

import android.net.Uri

data class Folder(
    val id: String,
    val albumId: String,
    val name: String,
    val uri: String,
    val coverUri: String?,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val wallpapers: List<Wallpaper> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = Uri.decode(name).let { decoded ->
            decoded.substringAfterLast('/', decoded.substringAfterLast(':', decoded))
        }

    companion object {
        fun empty(id: String = "", albumId: String = "") = Folder(
            id = id,
            albumId = albumId,
            name = "",
            uri = "",
            coverUri = null,
            dateModified = 0L
        )
    }
}
