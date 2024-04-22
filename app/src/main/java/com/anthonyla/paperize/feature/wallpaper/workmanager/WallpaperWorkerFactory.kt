package com.anthonyla.paperize.feature.wallpaper.workmanager

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperWorkerFactory @Inject constructor(
    private val selectedAlbumRepository: SelectedAlbumRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker = WallpaperWorker(
        selectedAlbumRepository,
        appContext,
        workerParameters
    )
}