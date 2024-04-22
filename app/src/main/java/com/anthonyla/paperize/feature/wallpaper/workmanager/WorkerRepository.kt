package com.anthonyla.paperize.feature.wallpaper.workmanager

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Repository class to handle WallpaperWorker scheduling
 */
class WorkerRepository @Inject constructor(context: Context) {
    private val workManager = WorkManager.getInstance(context)
    fun scheduleWallpaperChanger(time: Long) {
        val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
            15, TimeUnit.MINUTES
        )
        workManager.enqueueUniquePeriodicWork(
            "WallpaperWorker",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest.build()
        )
    }

    fun cancelWorker() {
        workManager.cancelAllWork()
    }
}