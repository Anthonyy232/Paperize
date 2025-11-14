package com.anthonyla.paperize

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.anthonyla.paperize.core.constants.Constants
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Paperize
 *
 * Annotated with @HiltAndroidApp to enable dependency injection
 */
@HiltAndroidApp
class PaperizeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channel (minSdk is 31, so always supported)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Wallpaper change notifications"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
