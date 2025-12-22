package com.anthonyla.paperize.service.tile

import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings Tile for changing wallpaper
 */
@AndroidEntryPoint
class WallpaperTileService : TileService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "WallpaperTileService"
    }

    override fun onClick() {
        super.onClick()

        serviceScope.launch {
            try {
                val mode = settingsRepository.getWallpaperMode()

                if (mode == WallpaperMode.LIVE) {
                    // Send broadcast to trigger live wallpaper reload
                    val intent = Intent(Constants.ACTION_RELOAD_WALLPAPER)
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                } else {
                    // Start wallpaper change service for static wallpaper
                    // Use BOTH to ensure synchronized change if configured, or both if not
                    val intent = Intent(this@WallpaperTileService, WallpaperChangeService::class.java).apply {
                        action = Constants.ACTION_CHANGE_WALLPAPER
                        putExtra(Constants.EXTRA_SCREEN_TYPE, ScreenType.BOTH.name)
                    }
                    startForegroundService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling tile click", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
