package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.CursorWindow
import android.graphics.Color
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.themes.PaperizeTheme
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmItem
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmSchedulerImpl
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Field
import javax.inject.Inject
import androidx.core.net.toUri


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SET_WALLPAPER) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.SET_WALLPAPER), 0)

        }
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
            ensureExactAlarmPermission(context)
            val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
            val isFirstLaunch = runBlocking { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) } ?: true
            val scheduler = WallpaperAlarmSchedulerImpl(context)

            var hasScheduleRun by remember { mutableStateOf(false) }
            LaunchedEffect(settingsState.value) {
                if (!hasScheduleRun && settingsState.value.initialized) {
                    handleWallpaperScheduling(settingsState.value, scheduler)
                    hasScheduleRun = true
                }
            }

            PaperizeTheme(
                darkMode = settingsState.value.themeSettings.darkMode,
                amoledMode = settingsState.value.themeSettings.amoledTheme,
                dynamicTheming = settingsState.value.themeSettings.dynamicTheming
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    tonalElevation = 5.dp
                ) {
                    PaperizeApp(isFirstLaunch, scheduler)
                }
            }
        }
    }

    private fun ensureExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    private suspend fun handleWallpaperScheduling(
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

    private suspend fun scheduleWallpaperAlarm(
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
                startTime = scheduleSettings.startTime,
            ),
            origin = null,
            changeImmediate = true,
            cancelImmediate = true,
            firstLaunch = true,
            homeNextTime = settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME),
            lockNextTime = settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME)
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
