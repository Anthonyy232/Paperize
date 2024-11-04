package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.anthonyla.paperize.data.SendContactIntent
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.LicensesScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.notifications_screen.NotificationScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.PrivacyScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.startup_screen.StartupScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.WallpaperScreenViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AddAlbum
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AlbumView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.FolderView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Home
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Licenses
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Notification
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Privacy
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Settings
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Startup
import com.anthonyla.paperize.feature.wallpaper.util.navigation.WallpaperView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.animatedScreen
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmItem
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmSchedulerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperizeApp(
    firstLaunch: Boolean,
    scheduler : WallpaperAlarmSchedulerImpl,
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel(),
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val folderViewModel: FolderViewModel = hiltViewModel()

    // React to albumState changes and change selectedAlbum's details to keep it from being stale
    LaunchedEffect(albumState.value) {
        withContext(Dispatchers.IO) {
            // Process albums that need initialization or deletion
            albumState.value.albumsWithWallpapers.asSequence()
                .filter { album ->
                    (!album.album.initialized && (album.wallpapers.isNotEmpty() || album.folders.isNotEmpty())) ||
                            (album.wallpapers.isEmpty() && album.folders.isEmpty() && album.album.initialized)
                }
                .forEach { album ->
                    when {
                        !album.album.initialized -> {
                            albumsViewModel.onEvent(AlbumsEvent.InitializeAlbum(album))
                        }
                        else -> {
                            if (navController.currentDestination?.route == Home::class.simpleName) {
                                try {
                                    navController.popBackStack<Home>(inclusive = false)
                                } catch (_: Exception) {
                                    navController.navigate(Home)
                                }
                            }
                            albumsViewModel.onEvent(AlbumsEvent.DeleteAlbumWithWallpapers(album))
                        }
                    }
                }
            // Update or reset selected albums
            selectedState.value.selectedAlbum?.forEach { selectedAlbum ->
                val matchingAlbum = albumState.value.albumsWithWallpapers
                    .find { it.album.initialAlbumName == selectedAlbum.album.initialAlbumName }
                matchingAlbum?.let { album ->
                    val needsUpdate = selectedAlbum.album.displayedAlbumName != album.album.displayedAlbumName ||
                            selectedAlbum.album.coverUri != album.album.coverUri ||
                            selectedAlbum.wallpapers.size != album.wallpapers.size + album.folders.sumOf { it.wallpapers.size }

                    if (needsUpdate) {
                        wallpaperScreenViewModel.onEvent(WallpaperEvent.AddSelectedAlbum(album, selectedAlbum.album.initialAlbumName))
                    }
                } ?: run {
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedAlbum))
                    settingsViewModel.onEvent(SettingsEvent.RemoveSelectedAlbumAsName(selectedAlbum.album.initialAlbumName))
                    scheduler.cancelWallpaperAlarm()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (firstLaunch) Startup else Home,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        // Navigate to the startup screen to show the privacy policy and notification screen
        animatedScreen<Startup>(animate = settingsState.value.themeSettings.animate) {
            StartupScreen(
                onAgree = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        settingsViewModel.onEvent(SettingsEvent.SetFirstLaunch)
                    }
                    else { navController.navigate(Notification) }
                }
            )
        }

        // Navigate to the notification screen to ask for notification permission
        animatedScreen<Notification>(animate = settingsState.value.themeSettings.animate) {
            NotificationScreen(
                onAgree = { settingsViewModel.onEvent(SettingsEvent.SetFirstLaunch) }
            )
        }

        // Navigate to the home screen to view all albums and wallpapers
        animatedScreen<Home>(animate = settingsState.value.themeSettings.animate) {
            HomeScreen(
                albums = albumState.value.albumsWithWallpapers,
                homeSelectedAlbum = selectedState.value.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.homeAlbumName },
                lockSelectedAlbum = selectedState.value.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.lockAlbumName },
                themeSettings = settingsState.value.themeSettings,
                wallpaperSettings = settingsState.value.wallpaperSettings,
                scheduleSettings = settingsState.value.scheduleSettings,
                effectSettings = settingsState.value.effectSettings,
                onSettingsClick = { navController.navigate(Settings) },
                onNavigateAddWallpaper = { navController.navigate(AddAlbum(it)) },
                onViewAlbum = { navController.navigate(AlbumView(it)) },
                onHomeTimeChange = { timeInMinutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetHomeWallpaperInterval(timeInMinutes))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = timeInMinutes,
                                lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                scheduleSeparately = settingsState.value.scheduleSettings.scheduleSeparately,
                                setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                                startTime = settingsState.value.scheduleSettings.startTime
                            )
                            alarmItem.let{scheduler.updateWallpaperAlarm(it)}
                            scheduler.scheduleRefresh()
                        }
                    }
                },
                onLockTimeChange = { timeInMinutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetLockWallpaperInterval(timeInMinutes))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                lockInterval = timeInMinutes,
                                scheduleSeparately = settingsState.value.scheduleSettings.scheduleSeparately,
                                setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                                startTime = settingsState.value.scheduleSettings.startTime
                            )
                            alarmItem.let{scheduler.updateWallpaperAlarm(it)}
                            scheduler.scheduleRefresh()
                        }
                    }

                },
                onStop = { lock, home ->
                    if (!selectedState.value.selectedAlbum.isNullOrEmpty()) {
                        val notSameAlbum = settingsState.value.wallpaperSettings.homeAlbumName != settingsState.value.wallpaperSettings.lockAlbumName
                        when {
                            lock && home -> {
                                wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedState.value.selectedAlbum!!.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.homeAlbumName}))
                            }
                            lock -> {
                                if (notSameAlbum) wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedState.value.selectedAlbum!!.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.lockAlbumName}))
                            }
                            home -> {
                                if (notSameAlbum) wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedState.value.selectedAlbum!!.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.homeAlbumName}))
                            }
                        }
                        settingsViewModel.onEvent(SettingsEvent.RemoveSelectedAlbumAsType(lock, home))
                        scheduler.cancelWallpaperAlarm()
                    }
                },
                onToggleChanger = { enableWallpaperChanger ->
                    if (!selectedState.value.selectedAlbum.isNullOrEmpty() && !settingsState.value.wallpaperSettings.homeAlbumName.isNullOrEmpty() && !settingsState.value.wallpaperSettings.lockAlbumName.isNullOrEmpty()) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(enableWallpaperChanger))
                        if (enableWallpaperChanger) {
                            job?.cancel()
                            job = scope.launch {
                                settingsViewModel.onEvent(SettingsEvent.RefreshNextSetTime)
                                val alarmItem = WallpaperAlarmItem(
                                    homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                    lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                    scheduleSeparately = settingsState.value.scheduleSettings.scheduleSeparately,
                                    setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                    setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                    changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                                    startTime = settingsState.value.scheduleSettings.startTime
                                )
                                alarmItem.let{scheduler.scheduleWallpaperAlarm(
                                    wallpaperAlarmItem = it,
                                    origin = null,
                                    changeImmediate = true,
                                    cancelImmediate = true,
                                    firstLaunch = true
                                ) }
                                scheduler.scheduleRefresh()
                            }
                        }
                        else { scheduler.cancelWallpaperAlarm() }
                    }
                },
                onSelectAlbum = { album, lock, home ->
                    val notSameAlbum = settingsState.value.wallpaperSettings.homeAlbumName != settingsState.value.wallpaperSettings.lockAlbumName
                    when {
                        lock && home -> {
                            settingsViewModel.onEvent(SettingsEvent.SetAlbumName(
                                homeAlbumName = album.album.initialAlbumName,
                                lockAlbumName = album.album.initialAlbumName,
                            ))
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.AddSelectedAlbum(
                                album = album,
                                deleteAlbumName = if (notSameAlbum) settingsState.value.wallpaperSettings.lockAlbumName else null)
                            )
                        }
                        lock -> {
                            settingsViewModel.onEvent(SettingsEvent.SetAlbumName(
                                homeAlbumName = null,
                                lockAlbumName = album.album.initialAlbumName,
                            ))
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.AddSelectedAlbum(
                                album = album,
                                deleteAlbumName = if (notSameAlbum) settingsState.value.wallpaperSettings.lockAlbumName else null)
                            )
                        }
                        home -> {
                            settingsViewModel.onEvent(SettingsEvent.SetAlbumName(
                                homeAlbumName = album.album.initialAlbumName,
                                lockAlbumName = null,
                            ))
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.AddSelectedAlbum(
                                album = album,
                                deleteAlbumName = if (notSameAlbum) settingsState.value.wallpaperSettings.homeAlbumName else null)
                            )
                        }
                    }
                    settingsViewModel.onEvent(SettingsEvent.RefreshNextSetTime)
                    scope.launch {
                        delay(1000)
                        val alarmItem = WallpaperAlarmItem(
                            homeInterval = settingsState.value.scheduleSettings.homeInterval,
                            lockInterval = settingsState.value.scheduleSettings.lockInterval,
                            scheduleSeparately = settingsState.value.scheduleSettings.scheduleSeparately,
                            setHome = home,
                            setLock = lock,
                            changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                            startTime = settingsState.value.scheduleSettings.startTime
                        )
                        alarmItem.let {
                            scheduler.scheduleWallpaperAlarm(
                                wallpaperAlarmItem = it,
                                origin = null,
                                changeImmediate = true,
                                cancelImmediate = true,
                                firstLaunch = true
                            )
                        }
                        scheduler.scheduleRefresh()
                    }
                },
                onDarkenPercentage = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetDarkenPercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.darken) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onDarkCheck = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarken(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onScalingChange = {
                    if (settingsState.value.wallpaperSettings.wallpaperScaling != it) {
                        settingsViewModel.onEvent(SettingsEvent.SetWallpaperScaling(it))
                        if (settingsState.value.wallpaperSettings.enableChanger) {
                            job?.cancel()
                            job = scope.launch {
                                scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                            }
                        }
                    }
                },
                onHomeCheckedChange = { setHome ->
                    settingsViewModel.onEvent(SettingsEvent.SetHome(setHome))
                    if (!selectedState.value.selectedAlbum.isNullOrEmpty() && !setHome && !settingsState.value.wallpaperSettings.setLockWallpaper) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        selectedState.value.selectedAlbum?.let { wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset()) }
                        scheduler.cancelWallpaperAlarm()
                    }
                    else if (!selectedState.value.selectedAlbum.isNullOrEmpty() && (setHome && !settingsState.value.wallpaperSettings.setLockWallpaper) || (!setHome && settingsState.value.wallpaperSettings.setLockWallpaper)) {
                        settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(false))
                        job?.cancel()
                        job = scope.launch {
                            if (!setHome && settingsState.value.wallpaperSettings.setLockWallpaper) {
                                val homeAlbum = selectedState.value.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.homeAlbumName }
                                if (homeAlbum != null) {
                                    wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(
                                        album = homeAlbum.copy(
                                            album = homeAlbum.album.copy(
                                                lockWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue,
                                                homeWallpapersInQueue = homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                                            )
                                        ),
                                    ))
                                }
                            }
                            delay(1000)
                            scheduler.updateWallpaper(false, setHome, settingsState.value.wallpaperSettings.setLockWallpaper)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                scheduleSeparately = false,
                                setHome = setHome,
                                setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                                startTime = settingsState.value.scheduleSettings.startTime
                            )
                            alarmItem.let{scheduler.scheduleWallpaperAlarm(
                                wallpaperAlarmItem = it,
                                origin = null,
                                changeImmediate = false,
                                cancelImmediate = true,
                                firstLaunch = true
                            ) }
                            scheduler.scheduleRefresh()
                        }
                    }
                    else if (!selectedState.value.selectedAlbum.isNullOrEmpty() && settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, setHome, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onLockCheckedChange = { setLock ->
                    settingsViewModel.onEvent(SettingsEvent.SetLock(setLock))
                    if (selectedState.value.selectedAlbum!= null && !setLock && !settingsState.value.wallpaperSettings.setHomeWallpaper) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        selectedState.value.selectedAlbum?.let {
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset())
                        }
                        scheduler.cancelWallpaperAlarm()
                    }
                    else if (selectedState.value.selectedAlbum!= null && (setLock && !settingsState.value.wallpaperSettings.setHomeWallpaper) || (!setLock && settingsState.value.wallpaperSettings.setHomeWallpaper)) {
                        settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(false))
                        job?.cancel()
                        job = scope.launch {
                            delay(1000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, setLock)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                scheduleSeparately = false,
                                setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                setLock = setLock,
                                changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                                startTime = settingsState.value.scheduleSettings.startTime
                            )
                            alarmItem.let{ scheduler.scheduleWallpaperAlarm(
                                wallpaperAlarmItem = it,
                                origin = null,
                                changeImmediate = false,
                                cancelImmediate = true,
                                firstLaunch = true
                            ) }
                            scheduler.scheduleRefresh()
                        }
                    }
                    else if (!selectedState.value.selectedAlbum.isNullOrEmpty() && settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(1000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onScheduleSeparatelyChange = { changeSeparately ->
                    settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(changeSeparately))
                    if (!selectedState.value.selectedAlbum.isNullOrEmpty() && settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(1000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                scheduleSeparately = changeSeparately,
                                setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                changeStartTime = settingsState.value.scheduleSettings.changeStartTime,
                                startTime = settingsState.value.scheduleSettings.startTime
                            )
                            alarmItem.let{scheduler.scheduleWallpaperAlarm(
                                wallpaperAlarmItem = it,
                                origin = null,
                                changeImmediate = true,
                                cancelImmediate = true,
                                firstLaunch = true
                            ) }
                            scheduler.scheduleRefresh()
                        }
                    }
                },
                onBlurChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetBlur(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onBlurPercentageChange = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetBlurPercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.blur) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onVignetteChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetVignette(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onVignettePercentageChange = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetVignettePercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.vignette) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onGrayscaleChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetGrayscale(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onGrayscalePercentageChange = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetGrayscalePercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.grayscale) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSettings.scheduleSeparately, settingsState.value.wallpaperSettings.setHomeWallpaper, settingsState.value.wallpaperSettings.setLockWallpaper)
                        }
                    }
                },
                onStartTimeChange = { time ->
                    settingsViewModel.onEvent(SettingsEvent.SetStartTime(time.hour, time.minute))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                scheduleSeparately = settingsState.value.scheduleSettings.scheduleSeparately,
                                setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                changeStartTime = true,
                                startTime = Pair(time.hour, time.minute)
                            )
                            alarmItem.let{scheduler.updateWallpaperAlarm(it, true)}
                            scheduler.scheduleRefresh()
                        }
                    }
                },
                onChangeStartTimeToggle = { changeStartTime ->
                    settingsViewModel.onEvent(SettingsEvent.SetChangeStartTime(changeStartTime))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.scheduleSettings.homeInterval,
                                lockInterval = settingsState.value.scheduleSettings.lockInterval,
                                scheduleSeparately = settingsState.value.scheduleSettings.scheduleSeparately,
                                setHome = settingsState.value.wallpaperSettings.setHomeWallpaper,
                                setLock = settingsState.value.wallpaperSettings.setLockWallpaper,
                                changeStartTime = changeStartTime,
                                startTime = settingsState.value.scheduleSettings.startTime
                            )
                            alarmItem.let{scheduler.updateWallpaperAlarm(it, true)}
                            scheduler.scheduleRefresh()
                        }
                    }
                },
            )
        }

        // Navigate to the add album screen to create a new album and add wallpapers to it
        animatedScreen<AddAlbum>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val addAlbum: AddAlbum = backStackEntry.toRoute()
            AddAlbumScreen(
                initialAlbumName = addAlbum.wallpaper,
                onBackClick = { navController.navigateUp() },
                onConfirmation = { navController.navigateUp() },
                onShowWallpaperView = {
                    navController.navigate(WallpaperView(it))
                },
                onShowFolderView = { folderName, wallpapers ->
                    folderViewModel.folderName.value = folderName
                    folderViewModel.wallpapers.value = wallpapers
                    navController.navigate(FolderView)
                },
                animate = settingsState.value.themeSettings.animate
            )
        }

        // Navigate to wallpaper view screen to view individual wallpapers in full screen
        animatedScreen<WallpaperView>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val wallpaperView: WallpaperView = backStackEntry.toRoute()
            WallpaperViewScreen(
                wallpaperUri = wallpaperView.wallpaper,
                onBackClick = { navController.navigateUp() },
                animate = settingsState.value.themeSettings.animate
            )
        }

        // Navigate to the folder view screen to view wallpapers in a folder
        animatedScreen<FolderView>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val folderName = folderViewModel.folderName.value
            val wallpapers = folderViewModel.wallpapers.value
            if (wallpapers != null) {
                if (wallpapers.isNotEmpty()) {
                    FolderViewScreen(
                        folderName = folderName,
                        wallpapers = wallpapers,
                        onBackClick = { navController.navigateUp() },
                        onShowWallpaperView = {
                            navController.navigate(WallpaperView(it))
                        },
                        animate = settingsState.value.themeSettings.animate
                    )
                } else {
                    navController.navigateUp()
                }
            }
        }

        // Navigate to the album view screen to view folders and wallpapers in an album
        animatedScreen<AlbumView>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val albumView: AlbumView = backStackEntry.toRoute()
            val albumWithWallpaper = albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == albumView.initialAlbumName }
            if (albumWithWallpaper != null) {
                AlbumViewScreen(
                    album = albumWithWallpaper,
                    animate = settingsState.value.themeSettings.animate,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = {
                        navController.navigate(WallpaperView(it))
                    },
                    onShowFolderView = { folderName, wallpapers ->
                        folderViewModel.folderName.value = folderName
                        folderViewModel.wallpapers.value = wallpapers
                        navController.navigate(FolderView)
                    },
                    onDeleteAlbum = {
                        navController.navigateUp()
                        albumsViewModel.onEvent(AlbumsEvent.DeleteAlbumWithWallpapers(albumWithWallpaper))
                    },
                    onAlbumNameChange = { name, originalAlbumWithWallpaper ->
                        albumsViewModel.onEvent(AlbumsEvent.ChangeAlbumName(name, originalAlbumWithWallpaper))
                    },
                    onSelectionDeleted = {
                        albumsViewModel.onEvent(AlbumsEvent.RefreshAlbums)
                    }
                )
            } else { navController.navigateUp() }
        }

        // Navigate to the settings screen to change app settings
        animatedScreen<Settings>(animate = settingsState.value.themeSettings.animate) {
            SettingsScreen(
                themeSettings = settingsState.value.themeSettings,
                onBackClick = { navController.navigateUp() },
                onDarkModeClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarkMode(it))
                },
                onAmoledClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetAmoledTheme(it))
                },
                onDynamicThemingClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDynamicTheming(it))
                },
                onAnimateClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetAnimate(it))
                },
                onPrivacyClick = {
                    navController.navigate(Privacy)
                },
                onLicenseClick = {
                    navController.navigate(Licenses)
                },
                onContactClick = {
                    SendContactIntent(context)
                },
                onResetClick = {
                    settingsViewModel.onEvent(SettingsEvent.Reset)
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset())
                    albumsViewModel.onEvent(AlbumsEvent.Reset)
                    addAlbumViewModel.onEvent(AddAlbumEvent.Reset)
                    scheduler.cancelWallpaperAlarm()
                    val contentResolver = context.contentResolver
                    val persistedUris = contentResolver.persistedUriPermissions
                    for (permission in persistedUris) {
                        contentResolver.releasePersistableUriPermission(permission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                }
            )
        }

        // Navigate to the privacy screen to view the privacy policy
        animatedScreen<Privacy>(animate = settingsState.value.themeSettings.animate) {
            PrivacyScreen(
                onBackClick = { navController.navigateUp() },
            )
        }

        // Navigate to the licenses screen to view the licenses of the libraries used
        animatedScreen<Licenses>(animate = settingsState.value.themeSettings.animate) {
            LicensesScreen(
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}