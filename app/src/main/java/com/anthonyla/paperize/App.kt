package com.anthonyla.paperize

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Hilt
 */
@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("wallpaper_service_channel", "Paperize", NotificationManager.IMPORTANCE_LOW)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}