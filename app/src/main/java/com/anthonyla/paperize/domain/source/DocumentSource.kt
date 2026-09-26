package com.anthonyla.paperize.domain.source

data class SourceImage(val uri: String, val name: String, val modifiedAt: Long)
data class SourceFolder(val name: String, val images: List<SourceImage>)

interface DocumentSource {
    suspend fun retainReadPermission(uri: String)
    suspend fun readImage(uri: String): SourceImage
    suspend fun readFolder(uri: String, onProgress: (Int) -> Unit = {}): SourceFolder
    suspend fun isMissing(uri: String, isTree: Boolean = false): Boolean
}
