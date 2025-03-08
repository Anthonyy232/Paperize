package com.anthonyla.paperize

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.database.CursorWindow
import dagger.hilt.android.HiltAndroidApp
import java.lang.reflect.Field


/**
 * Application class for Hilt
 */
@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("wallpaper_service_channel", "Paperize", NotificationManager.IMPORTANCE_LOW)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}