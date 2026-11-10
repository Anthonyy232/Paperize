package com.anthonyla.paperize

import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.domain.model.Wallpaper

fun testAlbum(id: String = "", name: String = "") = Album(id = id, name = name, coverUri = null)

fun testAlbumSummary(id: String = "", name: String = "") = AlbumSummary(
    id = id, name = name, coverUri = null, wallpaperCount = 0, folderCount = 0, createdAt = 0L, modifiedAt = 0L
)

fun testFolder(id: String = "", albumId: String = "") =
    Folder(id = id, albumId = albumId, name = "", uri = "", coverUri = null, dateModified = 0L)

fun testWallpaper(id: String = "", albumId: String = "") =
    Wallpaper(id = id, albumId = albumId, uri = "", fileName = "", dateModified = 0L)
