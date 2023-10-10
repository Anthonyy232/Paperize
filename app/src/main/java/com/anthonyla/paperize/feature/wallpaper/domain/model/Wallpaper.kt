package com.anthonyla.paperize.feature.wallpaper.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Wallpaper(
    @PrimaryKey(autoGenerate = false) val imageUri: String,
    val date: String
)