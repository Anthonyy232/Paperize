package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.data.settings.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Receiver for boot and time change and wallpaper change events to restart alarm manager
 */
@AndroidEntryPoint
class WallpaperBootAndChangeReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) = goAsync {
        try {
            val scheduler = WallpaperAlarmSchedulerImpl(context)
            val toggleChanger = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false
            if (toggleChanger) {
                val homeInterval = settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val lockInterval = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                val scheduleSeparately = settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false
                val setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false
                val setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false
                val alarmItem = WallpaperAlarmItem(
                    homeInterval = homeInterval,
                    lockInterval = lockInterval,
                    scheduleSeparately = scheduleSeparately,
                    setLock = setLock,
                    setHome = setHome
                )
                alarmItem.let{scheduler.scheduleWallpaperAlarm(
                    wallpaperAlarmItem = it,
                    origin = null,
                    changeImmediate = true,
                    cancelImmediate = true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* https://stackoverflow.com/questions/74111692/run-coroutine-functions-on-broadcast-receiver */
    private fun BroadcastReceiver.goAsync(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val pendingResult = goAsync()
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(context) {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }
}