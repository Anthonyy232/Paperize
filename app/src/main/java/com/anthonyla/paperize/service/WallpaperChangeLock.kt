package com.anthonyla.paperize.service

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared mutex for wallpaper change operations.
 *
 * Both [WallpaperChangeService] and [WallpaperChangeWorker] can trigger wallpaper
 * changes simultaneously (e.g. a manual change fires the service while a scheduled
 * WorkManager job fires the worker). This singleton ensures only one change runs
 * at a time across both entry points.
 */
@Singleton
class WallpaperChangeLock @Inject constructor() {
    val mutex = Mutex()
}
