package com.anthonyla.paperize.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

object PermissionUtil {
    
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             Environment.isExternalStorageManager()
        } else {
            // For older versions, we might check READ_EXTERNAL_STORAGE, but the app flow seems to rely on MANAGE for All Files Access
            // Stick to the existing logic which uses Environment.isExternalStorageManager()
            Environment.isExternalStorageManager() 
        }
       
    }
}
