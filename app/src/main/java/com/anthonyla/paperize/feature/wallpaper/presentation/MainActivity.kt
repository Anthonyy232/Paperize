package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    private val settingsViewModel: SettingsViewModel by viewModels()

    // Launcher for SET_WALLPAPER permission
    private val requestSetWallpaperPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Handle the case where the user denies the permission.
            // For wallpaper apps, this permission is critical, so denying it
            // means the app won't function as intended.
            // Consider showing a dialog explaining why it's needed and
            // offering to open app settings.
        }
    }

    // Launcher for SCHEDULE_EXACT_ALARM permission (Android 12+)
    private val requestExactAlarmPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // No specific action needed on result here, as we just want the permission granted.
        // The check `am.canScheduleExactAlarms()` will reflect the new state when needed.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Request SET_WALLPAPER permission immediately if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER) != PackageManager.PERMISSION_GRANTED) {
            requestSetWallpaperPermission.launch(Manifest.permission.SET_WALLPAPER)
        }

        // Trigger a refresh of wallpapers when the app is opened.
        val refreshIntent = Intent(this, HomeWallpaperService::class.java).apply {
            action = HomeWallpaperService.Actions.REFRESH.toString()
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(refreshIntent)
        } else {
            startService(refreshIntent)
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
            // Request SCHEDULE_EXACT_ALARM permission when the Composable is active.
            // This will prompt the user to grant the permission if not already granted.
            LaunchedEffect(Unit) {
                requestExactAlarmPermissionIfNeeded(this@MainActivity)
            }

            val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
            val isFirstLaunch = runBlocking { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) } ?: true
            val scheduler = WallpaperAlarmSchedulerImpl(this)

            var hasScheduleRun by remember { mutableStateOf(false) }
            LaunchedEffect(settingsState.value) {
                // Only attempt to schedule once settings are initialized and the scheduling logic hasn't run yet.
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

    /**
     * Requests the SCHEDULE_EXACT_ALARM permission if needed on Android 12 (S) and above.
     * This function should be called from a LaunchedEffect or similar lifecycle-aware scope
     * within Compose, or directly in onCreate if you prefer.
     */
    private fun requestExactAlarmPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                requestExactAlarmPermission.launch(intent)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleWallpaperScheduling(
        settings: SettingsState,
        scheduler: WallpaperAlarmSchedulerImpl
    ) {
        val wallpaperSettings = settings.wallpaperSettings
        val scheduleSettings = settings.scheduleSettings

        // If changer is not enabled, or essential album names are missing, disable changer and return.
        if (!wallpaperSettings.enableChanger || wallpaperSettings.homeAlbumName.isNullOrEmpty() || wallpaperSettings.lockAlbumName.isNullOrEmpty()) {
            if (wallpaperSettings.enableChanger) { // Only dispatch event if it was enabled
                settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
            }
            return
        }

        // Check if alarms are already set to avoid redundant scheduling
        val shouldScheduleAlarm = if (scheduleSettings.scheduleSeparately) {
            !(isPendingIntentSet(Type.HOME.ordinal) && isPendingIntentSet(Type.LOCK.ordinal))
        } else {
            !isPendingIntentSet(Type.SINGLE.ordinal)
        }

        if (shouldScheduleAlarm) {
            val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canScheduleExactAlarms) {
                scheduleWallpaperAlarm(settings, scheduler)
            } else {
                // Optionally prompt user to grant permission
                requestExactAlarmPermissionIfNeeded(this)
            }
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private suspend fun scheduleWallpaperAlarm(
        settings: SettingsState,
        scheduler: WallpaperAlarmSchedulerImpl
    ) {
        val scheduleSettings = settings.scheduleSettings
        val wallpaperSettings = settings.wallpaperSettings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {

                return
            }
        }

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

    private fun isPendingIntentSet(requestCode: Int): Boolean {
        val intent = Intent(applicationContext, WallpaperReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        return pendingIntent != null
    }
}

