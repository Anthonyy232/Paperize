package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.themes.PaperizeTheme
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreenViewModel
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmItem
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmSchedulerImpl
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val albumsViewModel: AlbumsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val wallpaperScreenViewModel: WallpaperScreenViewModel by viewModels()
    private val context = this
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SET_WALLPAPER) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.SET_WALLPAPER), 0)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val splashScreen = installSplashScreen()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
                val fadeOut = ObjectAnimator.ofFloat(splashScreenViewProvider.view, View.ALPHA, 1f, 0f)
                fadeOut.interpolator = AccelerateInterpolator()
                fadeOut.duration = 300L
                fadeOut.doOnEnd { splashScreenViewProvider.remove() }
                fadeOut.start()
            }
        }
        splashScreen.setKeepOnScreenCondition { settingsViewModel.setKeepOnScreenCondition }

        setContent {
            val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
            val isFirstLaunch = runBlocking { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) } ?: true
            val scheduler = WallpaperAlarmSchedulerImpl(context)
            if (isFirstLaunch) {
                handleFirstLaunch(scheduler)
            }

            LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
                lifecycleScope.launch {
                    handleWallpaperScheduling(settingsState.value, scheduler)
                }
            }

            PaperizeTheme(
                darkMode = settingsState.value.themeSettings.darkMode,
                amoledMode = settingsState.value.themeSettings.amoledTheme,
                dynamicTheming = settingsState.value.themeSettings.dynamicTheming
            ) {
                Surface(tonalElevation = 5.dp) {
                    PaperizeApp(isFirstLaunch, scheduler)
                }
            }
        }
    }

    private fun handleFirstLaunch(scheduler: WallpaperAlarmSchedulerImpl) {
        scheduler.cancelWallpaperAlarm()
        wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset())
        settingsViewModel.onEvent(SettingsEvent.Reset)
        albumsViewModel.onEvent(AlbumsEvent.Reset)
        clearPersistedUriPermissions()
    }

    private fun clearPersistedUriPermissions() {
        val contentResolver = context.contentResolver
        val persistedUris = contentResolver.persistedUriPermissions
        for (permission in persistedUris) {
            contentResolver.releasePersistableUriPermission(
                permission.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun handleWallpaperScheduling(
        settings: SettingsState,
        scheduler: WallpaperAlarmSchedulerImpl
    ) {
        val wallpaperSettings = settings.wallpaperSettings
        val scheduleSettings = settings.scheduleSettings

        if (!wallpaperSettings.enableChanger) return
        if (wallpaperSettings.homeAlbumName.isNullOrEmpty() || wallpaperSettings.lockAlbumName.isNullOrEmpty()) {
            settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
            return
        }

        val shouldScheduleAlarm = if (scheduleSettings.scheduleSeparately) {
            !(isPendingIntentSet(Type.HOME.ordinal) && isPendingIntentSet(Type.LOCK.ordinal))
        } else {
            !isPendingIntentSet(Type.SINGLE.ordinal)
        }

        if (shouldScheduleAlarm) {
            scheduleWallpaperAlarm(settings, scheduler)
        }
    }

    private fun scheduleWallpaperAlarm(
        settings: SettingsState,
        scheduler: WallpaperAlarmSchedulerImpl
    ) {
        val scheduleSettings = settings.scheduleSettings
        val wallpaperSettings = settings.wallpaperSettings

        scheduler.scheduleWallpaperAlarm(
            WallpaperAlarmItem(
                homeInterval = scheduleSettings.homeInterval,
                lockInterval = scheduleSettings.lockInterval,
                setLock = wallpaperSettings.setLockWallpaper,
                setHome = wallpaperSettings.setHomeWallpaper,
                scheduleSeparately = scheduleSettings.scheduleSeparately,
                changeStartTime = scheduleSettings.changeStartTime,
                startTime = scheduleSettings.startTime
            ),
            origin = null,
            changeImmediate = true,
            cancelImmediate = true,
            firstLaunch = true
        )
        settingsViewModel.onEvent(SettingsEvent.RefreshNextSetTime)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                Intent().also { intent ->
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    context.startActivity(intent)
                }
            }
        }
    }

    private fun isPendingIntentSet(requestCode: Int): Boolean {
        val intent = Intent(context.applicationContext, WallpaperReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        return pendingIntent != null
    }
}
