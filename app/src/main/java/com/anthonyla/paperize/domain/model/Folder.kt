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
        get() = decodedLastSegment(name)
}

/** Readable name of a document URI or encoded path: the part after the last '/', else after the last ':'. */
internal fun decodedLastSegment(encoded: String): String = Uri.decode(encoded).let { decoded ->
    decoded.substringAfterLast('/', decoded.substringAfterLast(':', decoded))
}
