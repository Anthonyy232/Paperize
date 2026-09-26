package com.anthonyla.paperize.domain.model

import android.net.Uri
import com.anthonyla.paperize.core.WallpaperMediaType
import com.anthonyla.paperize.core.WallpaperSourceType

data class Wallpaper(
    val id: String,
    val albumId: String,
    val folderId: String? = null,
    val uri: String,
    val fileName: String,
    val dateModified: Long,
    val displayOrder: Int = 0,
    val sourceType: WallpaperSourceType = WallpaperSourceType.DIRECT,
    val addedAt: Long = System.currentTimeMillis(),
    val mediaType: WallpaperMediaType = WallpaperMediaType.IMAGE
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
