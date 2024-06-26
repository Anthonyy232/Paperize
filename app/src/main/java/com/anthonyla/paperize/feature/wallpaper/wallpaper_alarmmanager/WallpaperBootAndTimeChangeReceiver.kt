package com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager
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
 * Receiver for boot and time change events to restart alarm manager
 */
@AndroidEntryPoint
class WallpaperBootAndTimeChangeReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    override fun onReceive(context: Context, intent: Intent) = goAsync {
        try {
            if (Intent.ACTION_BOOT_COMPLETED == intent.action || Intent.ACTION_TIME_CHANGED == intent.action || Intent.ACTION_TIMEZONE_CHANGED == intent.action) {
                val scheduler = WallpaperScheduler(context)
                val toggleChanger = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false
                if (toggleChanger) {
                    val timeInMinutes1 = settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                    val timeInMinutes2 = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
                    val scheduleSeparately = settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false
                    val alarmItem = WallpaperAlarmItem(
                        timeInMinutes1 = timeInMinutes1,
                        timeInMinutes2 = timeInMinutes2,
                        scheduleSeparately = scheduleSeparately
                    )
                    alarmItem.let{scheduler.scheduleWallpaperAlarm(it, null, false, true)}
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