package com.anthonyla.paperize.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService

/**
 * Broadcast receiver for wallpaper change alarms
 */
class WallpaperAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WallpaperAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received: ${intent.action}")

        val serviceIntent = Intent(context, WallpaperChangeService::class.java).apply {
            action = intent.action
            intent.extras?.let { putExtras(it) }
        }

        // minSdk is 31, so startForegroundService is always available
        context.startForegroundService(serviceIntent)
    }
}
