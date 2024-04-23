package com.anthonyla.paperize.feature.wallpaper.alarmmanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.TimeUnit

class WallpaperScheduler (
    private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleWallpaperChanger(timeInMinutes: Long) {
        val intervalInMillis = TimeUnit.MINUTES.toMillis(timeInMinutes)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            intervalInMillis,
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WallpaperReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.d("WallpaperReceiver", "Scheduled wallpaper changer every $timeInMinutes minutes")
    }

    fun cancelWallpaperChanger() {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WallpaperReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.d("WallpaperReceiver", "Cancelled wallpaper changer")
    }
}