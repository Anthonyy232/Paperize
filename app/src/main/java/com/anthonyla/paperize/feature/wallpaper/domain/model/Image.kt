package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Image(
    @PrimaryKey(autoGenerate = true) val imageId: Long = 0,
    val albumId: Long ?= 0,
    val imageName: String,
    val dateAdded: String
)