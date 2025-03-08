package com.anthonyla.paperize.feature.wallpaper.app_shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver

class BroadcastActivity : Activity() {
    companion object {
        const val ACTION_CUSTOM_BROADCAST = "com.anthonyla.paperize.SHORTCUT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(ACTION_CUSTOM_BROADCAST).apply {
            setClass(this@BroadcastActivity, WallpaperBootAndChangeReceiver::class.java)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        sendBroadcast(intent)
        finish()
    }
}