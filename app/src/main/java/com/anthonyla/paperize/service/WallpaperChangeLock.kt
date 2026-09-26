package com.anthonyla.paperize.service

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes manual and scheduled wallpaper changes across both entry points. */
@Singleton
class WallpaperChangeLock @Inject constructor() {
    val mutex = Mutex()
}
