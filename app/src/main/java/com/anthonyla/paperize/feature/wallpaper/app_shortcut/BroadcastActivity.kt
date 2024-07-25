package com.anthonyla.paperize.feature.wallpaper.app_shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver

class BroadcastActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent("WallpaperBootAndChangeReceiver.intent.action.BROADCAST").setClass(this, WallpaperBootAndChangeReceiver::class.java)
        sendBroadcast(intent)
        finish()
    }
}