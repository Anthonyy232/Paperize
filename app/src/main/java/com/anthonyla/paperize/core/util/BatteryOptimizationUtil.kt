package com.anthonyla.paperize.core.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationUtil {
    private const val TAG = "BatteryOptimizationUtil"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting battery optimization exclusion", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening battery optimization settings", e2)
            }
        }
    }
}
