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
            val scheduler = WallpaperAlarmSchedulerImpl(context)
            val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
            val isFirstLaunch = runBlocking { settingsDataStoreImpl.getBoolean(SettingsConstants.FIRST_LAUNCH) } ?: true
            if (isFirstLaunch) {
                scheduler.cancelWallpaperAlarm()
                wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset())
                settingsViewModel.onEvent(SettingsEvent.Reset)
                albumsViewModel.onEvent(AlbumsEvent.Reset)
                val contentResolver = context.contentResolver
                val persistedUris = contentResolver.persistedUriPermissions
                for (permission in persistedUris) {
                    contentResolver.releasePersistableUriPermission(permission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
                lifecycleScope.launch {
                    if (settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) == true) {
                        val homeAlbumName = settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME)
                        val lockAlbumName = settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME)
                        if (homeAlbumName.isNullOrEmpty() || lockAlbumName.isNullOrEmpty()) {
                            settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        }
                        else {
                            val shouldScheduleSeparately = settingsDataStoreImpl.getBoolean(SettingsConstants.SCHEDULE_SEPARATELY) ?: false
                            val shouldScheduleAlarm = if (shouldScheduleSeparately) {
                                !(isPendingIntentSet(Type.HOME.ordinal) && isPendingIntentSet(Type.LOCK.ordinal))
                            } else {
                                !isPendingIntentSet(Type.SINGLE.ordinal)
                            }
                            if (shouldScheduleAlarm) {
                                scheduler.scheduleWallpaperAlarm(
                                    WallpaperAlarmItem(
                                        homeInterval = settingsDataStoreImpl.getInt(SettingsConstants.HOME_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                                        lockInterval = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_WALLPAPER_CHANGE_INTERVAL) ?: SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT,
                                        setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false,
                                        setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false,
                                        scheduleSeparately = shouldScheduleSeparately,
                                        changeStartTime = settingsDataStoreImpl.getBoolean(SettingsConstants.CHANGE_START_TIME) ?: false,
                                        startTime = Pair(
                                            settingsDataStoreImpl.getInt(SettingsConstants.START_HOUR) ?: 0,
                                            settingsDataStoreImpl.getInt(SettingsConstants.START_MINUTE) ?: 0
                                        ),
                                    ),
                                    origin = null,
                                    changeImmediate = true,
                                    cancelImmediate = true,
                                    firstLaunch = true
                                )
                                settingsViewModel.onEvent(SettingsEvent.RefreshNextSetTime)
                                val selectedState = wallpaperScreenViewModel.state.value
                                val currentHomeAlbum = selectedState.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.homeAlbumName }
                                val currentLockAlbum = selectedState.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.lockAlbumName }
                                when {
                                    settingsState.value.scheduleSeparately && settingsState.value.setHomeWallpaper && settingsState.value.setLockWallpaper -> {
                                        if (currentHomeAlbum != null && currentLockAlbum != null) {
                                            settingsViewModel.onEvent(SettingsEvent.SetCurrentWallpaper(
                                                currentHomeWallpaper = currentHomeAlbum.album.homeWallpapersInQueue.firstOrNull() ?: currentHomeAlbum.wallpapers.firstOrNull()?.wallpaperUri,
                                                currentLockWallpaper = currentLockAlbum.album.lockWallpapersInQueue.firstOrNull() ?: currentLockAlbum.wallpapers.firstOrNull()?.wallpaperUri
                                            ))
                                        }
                                    }
                                    !settingsState.value.scheduleSeparately && settingsState.value.setHomeWallpaper && settingsState.value.setLockWallpaper -> {
                                        if (currentHomeAlbum != null && currentLockAlbum != null) {
                                            settingsViewModel.onEvent(SettingsEvent.SetCurrentWallpaper(
                                                currentHomeWallpaper = currentHomeAlbum.album.homeWallpapersInQueue.firstOrNull() ?: currentHomeAlbum.wallpapers.firstOrNull()?.wallpaperUri,
                                                currentLockWallpaper = currentHomeAlbum.album.homeWallpapersInQueue.firstOrNull() ?: currentHomeAlbum.wallpapers.firstOrNull()?.wallpaperUri
                                            ))
                                        }
                                    }
                                    settingsState.value.setHomeWallpaper -> {
                                        if (currentHomeAlbum != null) {
                                            settingsViewModel.onEvent(SettingsEvent.SetCurrentWallpaper(
                                                currentHomeWallpaper = currentHomeAlbum.album.homeWallpapersInQueue.firstOrNull() ?: currentHomeAlbum.wallpapers.firstOrNull()?.wallpaperUri,
                                                currentLockWallpaper = if (settingsState.value.scheduleSeparately) null else currentHomeAlbum.album.homeWallpapersInQueue.firstOrNull() ?: currentHomeAlbum.wallpapers.firstOrNull()?.wallpaperUri
                                            ))
                                        }
                                    }
                                    settingsState.value.setLockWallpaper -> {
                                        if (currentLockAlbum != null) {
                                            settingsViewModel.onEvent(SettingsEvent.SetCurrentWallpaper(
                                                currentHomeWallpaper = if (settingsState.value.scheduleSeparately) null else currentLockAlbum.album.lockWallpapersInQueue.firstOrNull() ?: currentLockAlbum.wallpapers.firstOrNull()?.wallpaperUri,
                                                currentLockWallpaper = currentLockAlbum.album.lockWallpapersInQueue.firstOrNull() ?: currentLockAlbum.wallpapers.firstOrNull()?.wallpaperUri
                                            ))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PaperizeTheme(settingsState.value.darkMode, settingsState.value.amoledTheme, settingsState.value.dynamicTheming) {
                Surface(tonalElevation = 5.dp) {
                    PaperizeApp(isFirstLaunch, scheduler)
                }
            }
        }
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
