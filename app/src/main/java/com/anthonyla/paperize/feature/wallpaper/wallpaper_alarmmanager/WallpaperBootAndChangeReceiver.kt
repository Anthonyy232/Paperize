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
import androidx.core.content.edit

/**
 * Receiver for boot and time change and wallpaper change events to restart alarm manager
 */
@AndroidEntryPoint
class WallpaperBootAndChangeReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHORTCUT = "com.anthonyla.paperize.SHORTCUT"
        private const val PREFS_NAME = "wallpaper_receiver_prefs"
        private const val LAST_EXECUTION_TIME = "last_execution_time"
        private const val MIN_INTERVAL_MS = 3000
    }
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    override fun onReceive(context: Context, intent: Intent) = goAsync {
        try {
            if (intent.action == ACTION_SHORTCUT && !canExecute(context)) return@goAsync
            when(intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                ACTION_SHORTCUT -> {
                    saveExecutionTime(context)
                    val scheduler = WallpaperAlarmSchedulerImpl(context)
                    val toggleChanger = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false
                    val selectedAlbum = settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME) ?: settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME) ?: ""

                    if (selectedAlbum.isNotEmpty()) {
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
                        alarmItem.let{
                            scheduler.scheduleWallpaperAlarm(
                                wallpaperAlarmItem = it,
                                origin = null,
                                changeImmediate = true,
                                cancelImmediate = true,
                                setAlarm = toggleChanger,
                                homeNextTime = settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME),
                                lockNextTime = settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME),
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun canExecute(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastExecution = prefs.getLong(LAST_EXECUTION_TIME, 0)
        val currentTime = System.currentTimeMillis()

        return currentTime - lastExecution >= MIN_INTERVAL_MS
    }

    private fun saveExecutionTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putLong(LAST_EXECUTION_TIME, System.currentTimeMillis()) }
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