package com.anthonyla.paperize.domain.model

import android.net.Uri

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
        get() = Uri.decode(fileName).let { decoded ->
            decoded.substringAfterLast('/', decoded.substringAfterLast(':', decoded))
        }

    companion object {
        fun empty(id: String = "", albumId: String = "") = Wallpaper(
            id = id,
            albumId = albumId,
            uri = "",
            fileName = "",
            dateModified = 0L
        )
    }
}
