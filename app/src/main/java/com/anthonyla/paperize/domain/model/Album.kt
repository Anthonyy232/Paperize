package com.anthonyla.paperize.domain.model

data class Album(
    val id: String,
    val name: String,
    val coverUri: String?,
    val wallpapers: List<Wallpaper> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun empty(id: String = "", name: String = "") = Album(
            id = id,
            name = name,
            coverUri = null
        )
    }
}
