package com.anthonyla.paperize.feature.wallpaper.alarmmanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WallpaperScheduler @Inject constructor (
    private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val requestCode = "Paperize".hashCode()

    fun scheduleWallpaperChanger(timeInMinutes: Long, activateImmediately: Boolean = true) {
        val intervalInMillis = TimeUnit.MINUTES.toMillis(timeInMinutes)
        val intent = Intent(context, WallpaperReceiver::class.java).apply {
            putExtra("timeInMinutes", timeInMinutes)
        }

        if (activateImmediately) {
            context.sendBroadcast(intent)
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + intervalInMillis,
                    PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                Log.d("WallpaperReceiver", "Scheduled wallpaper changer in ${TimeUnit.MILLISECONDS.toMinutes(intervalInMillis)} minutes")
            } else {
                // Handle the case where the app cannot schedule exact alarms
                Log.e("WallpaperReceiver", "Cannot schedule exact alarms")
                cancelWallpaperChanger()
            }
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalInMillis,
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            Log.d("WallpaperReceiver", "Scheduled wallpaper changer in ${TimeUnit.MILLISECONDS.toMinutes(intervalInMillis)} minutes")
        }
    }

    fun cancelWallpaperChanger() {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, WallpaperReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.d("WallpaperReceiver", "Cancelled wallpaper changer")
    }
}