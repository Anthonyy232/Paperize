package com.anthonyla.paperize

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.anthonyla.paperize.feature.wallpaper.workmanager.WallpaperWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Hilt
 */
@HiltAndroidApp
class App: Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: WallpaperWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerFactory(workerFactory)
            .build()
}