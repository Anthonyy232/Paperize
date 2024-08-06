package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.AddAlbumViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.album.AlbumsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewScreen
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
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AddEdit
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AlbumView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Home
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Licenses
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavConstants.INITIAL_OFFSET
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavScreens
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Notification
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Privacy
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Settings
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Startup
import com.anthonyla.paperize.feature.wallpaper.util.navigation.WallpaperView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.sharedXTransitionIn
import com.anthonyla.paperize.feature.wallpaper.util.navigation.sharedXTransitionOut
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmItem
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmSchedulerImpl
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun PaperizeApp(
    firstLaunch: Boolean,
    scheduler : WallpaperAlarmSchedulerImpl,
    topInsets: Dp,
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    wallpaperScreenViewModel: WallpaperScreenViewModel = hiltViewModel(),
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val selectedState = wallpaperScreenViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // React to albumState changes and change selectedAlbum's details to keep it from being stale
    LaunchedEffect(albumState) {
        withContext(Dispatchers.IO) {
            albumState.value.albumsWithWallpapers.asSequence().forEach { albumWithWallpapers ->
                if (!albumWithWallpapers.album.initialized && (albumWithWallpapers.wallpapers.isNotEmpty() || albumWithWallpapers.folders.isNotEmpty())) {
                    albumsViewModel.onEvent(AlbumsEvent.InitializeAlbum(albumWithWallpapers))
                }
                else if (albumWithWallpapers.wallpapers.isEmpty() && albumWithWallpapers.folders.isEmpty() && albumWithWallpapers.album.initialized) {
                    if (navController.currentDestination?.route == Home::class.simpleName) {
                        try {
                            navController.popBackStack<Home>(inclusive = false)
                        } catch (e: Exception) {
                            navController.navigate(Home)
                        }
                    }
                    albumsViewModel.onEvent(AlbumsEvent.DeleteAlbumWithWallpapers(albumWithWallpapers))
                }
            }
            /*selectedState.value.selectedAlbum?.let { selectedAlbum ->
                albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == selectedAlbum.album.initialAlbumName }
                    ?.let { foundAlbum ->
                        val albumNameHashCode = foundAlbum.album.initialAlbumName.hashCode()
                        val wallpapers: List<Wallpaper> =
                            foundAlbum.wallpapers + foundAlbum.folders.flatMap { folder ->
                                folder.wallpapers.map { wallpaper ->
                                    Wallpaper(
                                        initialAlbumName = foundAlbum.album.initialAlbumName,
                                        wallpaperUri = wallpaper,
                                        key = wallpaper.hashCode() + albumNameHashCode,
                                    )
                                }
                            }
                        val wallpapersUri = wallpapers.map { it.wallpaperUri }.toSet()
                        val newSelectedAlbum = SelectedAlbum(
                            album = foundAlbum.album.copy(
                                homeWallpapersInQueue = if (selectedAlbum.album.homeWallpapersInQueue.firstOrNull() in wallpapersUri) {
                                    val firstElement = selectedAlbum.album.homeWallpapersInQueue.first()
                                    val modifiedWallpapersUri = wallpapersUri.filter { it != firstElement }
                                    listOf(firstElement) + modifiedWallpapersUri.shuffled()
                                } else { wallpapersUri.shuffled() },
                                lockWallpapersInQueue = if (selectedAlbum.album.lockWallpapersInQueue.firstOrNull() in wallpapersUri) {
                                    val firstElement = selectedAlbum.album.lockWallpapersInQueue.first()
                                    val modifiedWallpapersUri = wallpapersUri.filter { it != firstElement }
                                    listOf(firstElement) + modifiedWallpapersUri.shuffled()
                                } else { wallpapersUri.shuffled() },
                                currentHomeWallpaper = if (selectedAlbum.album.currentHomeWallpaper in wallpapersUri) selectedAlbum.album.currentHomeWallpaper else selectedAlbum.album.homeWallpapersInQueue.firstOrNull { it in wallpapersUri },
                                currentLockWallpaper = if (selectedAlbum.album.currentLockWallpaper in wallpapersUri) selectedAlbum.album.currentLockWallpaper else selectedAlbum.album.lockWallpapersInQueue.firstOrNull { it in wallpapersUri }
                            ),
                            wallpapers = wallpapers
                        )
                        wallpaperScreenViewModel.onEvent(
                            WallpaperEvent.UpdateSelectedAlbum(
                                newSelectedAlbum,
                                null,
                                settingsState.value.scheduleSeparately
                            )
                        )
                    } ?: run {
                    wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset)
                    scheduler.cancelWallpaperAlarm()
                }
            }*/
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (firstLaunch) Startup else Home,
        modifier = Modifier.navigationBarsPadding()
    ) {
        // Navigate to the startup screen to show the privacy policy and notification screen
        composable<Startup> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) {
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
        composable<Notification> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) {
            NotificationScreen(
                onAgree = { settingsViewModel.onEvent(SettingsEvent.SetFirstLaunch) }
            )
        }
        // Navigate to the home screen to view all albums and wallpapers
        composable<Home> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) {
            HomeScreen(
                albums = albumState.value.albumsWithWallpapers,
                onSettingsClick = { navController.navigate(Settings) },
                navigateToAddWallpaperScreen = {
                    navController.navigate(AddEdit(it))
                },
                onAlbumViewClick = {
                    navController.navigate(AlbumView(it))
                },
                onScheduleWallpaperChanger = {
                    if (settingsState.value.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.homeInterval,
                                lockInterval = settingsState.value.lockInterval,
                                scheduleSeparately = settingsState.value.scheduleSeparately,
                                setHome = settingsState.value.setHomeWallpaper,
                                setLock = settingsState.value.setLockWallpaper
                            )
                            alarmItem.let{scheduler.scheduleWallpaperAlarm(it, null, true, true)}
                            scheduler.scheduleRefresh()
                        }
                    }
                },
                onHomeTimeChange = { timeInMinutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetHomeWallpaperInterval(timeInMinutes))
                    if (settingsState.value.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = timeInMinutes,
                                lockInterval = settingsState.value.lockInterval,
                                scheduleSeparately = settingsState.value.scheduleSeparately,
                                setHome = settingsState.value.setHomeWallpaper,
                                setLock = settingsState.value.setLockWallpaper
                            )
                            alarmItem.let{scheduler.updateWallpaperAlarm(it)}
                            scheduler.scheduleRefresh()
                        }
                    }

                },
                onLockTimeChange = { timeInMinutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetLockWallpaperInterval(timeInMinutes))
                    if (settingsState.value.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(3000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.homeInterval,
                                lockInterval = timeInMinutes,
                                scheduleSeparately = settingsState.value.scheduleSeparately,
                                setHome = settingsState.value.setHomeWallpaper,
                                setLock = settingsState.value.setLockWallpaper
                            )
                            alarmItem.let{scheduler.updateWallpaperAlarm(it)}
                            scheduler.scheduleRefresh()
                        }
                    }

                },
                onStop = { lock, home ->
                    if (selectedState.value.selectedAlbum != null) {
                        val notSameAlbum = settingsState.value.homeAlbumName != settingsState.value.lockAlbumName
                        when {
                            lock && home -> {
                                wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedState.value.selectedAlbum!!.find { it.album.initialAlbumName == settingsState.value.homeAlbumName}))
                            }
                            lock -> {
                                if (notSameAlbum) wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedState.value.selectedAlbum!!.find { it.album.initialAlbumName == settingsState.value.lockAlbumName}))
                            }
                            home -> {
                                if (notSameAlbum) wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset(selectedState.value.selectedAlbum!!.find { it.album.initialAlbumName == settingsState.value.homeAlbumName}))
                            }
                        }
                        settingsViewModel.onEvent(SettingsEvent.RemoveSelectedAlbum(lock, home))
                        scheduler.cancelWallpaperAlarm()
                    }
                },
                animate = settingsState.value.animate,
                homeInterval = settingsState.value.homeInterval,
                lockInterval = settingsState.value.lockInterval,
                lastSetTime = settingsState.value.lastSetTime,
                nextSetTime = settingsState.value.nextSetTime,
                homeSelectedAlbum = selectedState.value.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.homeAlbumName },
                lockSelectedAlbum = selectedState.value.selectedAlbum?.find { it.album.initialAlbumName == settingsState.value.lockAlbumName },
                enableChanger = settingsState.value.enableChanger,
                darkenPercentage = settingsState.value.darkenPercentage,
                darken = settingsState.value.darken,
                homeEnabled = settingsState.value.setHomeWallpaper,
                lockEnabled = settingsState.value.setLockWallpaper,
                scheduleSeparately = settingsState.value.scheduleSeparately,
                blur = settingsState.value.blur,
                blurPercentage = settingsState.value.blurPercentage,
                currentHomeWallpaper = settingsState.value.currentHomeWallpaper,
                currentLockWallpaper = settingsState.value.currentLockWallpaper,
                onToggleChanger = { enableWallpaperChanger ->
                    if (selectedState.value.selectedAlbum != null && !settingsState.value.homeAlbumName.isNullOrEmpty() && !settingsState.value.lockAlbumName.isNullOrEmpty()) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(enableWallpaperChanger))
                        if (enableWallpaperChanger) {
                            // update the wallpaper preview in wallpaper screen viewmodel
                            job?.cancel()
                            job = scope.launch {
                                delay(2000)
                                val alarmItem = WallpaperAlarmItem(
                                    homeInterval = settingsState.value.homeInterval,
                                    lockInterval = settingsState.value.lockInterval,
                                    scheduleSeparately = settingsState.value.scheduleSeparately,
                                    setHome = settingsState.value.setHomeWallpaper,
                                    setLock = settingsState.value.setLockWallpaper
                                )
                                alarmItem.let{scheduler.scheduleWallpaperAlarm(
                                    wallpaperAlarmItem = it,
                                    origin = null,
                                    changeImmediate = true,
                                    cancelImmediate = true)
                                }
                                scheduler.scheduleRefresh()
                            }
                        }
                        else { scheduler.cancelWallpaperAlarm() }
                    }
                },
                onSelectAlbum = {album, lock, home ->
                    val notSameAlbum = settingsState.value.homeAlbumName != settingsState.value.lockAlbumName
                    when {
                        lock && home -> {
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(album, settingsState.value.homeAlbumName))
                            settingsViewModel.onEvent(SettingsEvent.SetAlbumName(
                                homeAlbumName = album.album.initialAlbumName,
                                lockAlbumName = album.album.initialAlbumName,
                            ))
                        }
                        lock -> {
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(album, if (notSameAlbum) settingsState.value.lockAlbumName else null))
                            settingsViewModel.onEvent(SettingsEvent.SetAlbumName(
                                homeAlbumName = null,
                                lockAlbumName = album.album.initialAlbumName,
                            ))
                        }
                        home -> {
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.UpdateSelectedAlbum(album, if (notSameAlbum) settingsState.value.homeAlbumName else null))
                            settingsViewModel.onEvent(SettingsEvent.SetAlbumName(
                                homeAlbumName = album.album.initialAlbumName,
                                lockAlbumName = null,
                            ))
                        }
                    }
                    job?.cancel()
                    job = scope.launch {
                        val alarmItem = WallpaperAlarmItem(
                            homeInterval = settingsState.value.homeInterval,
                            lockInterval = settingsState.value.lockInterval,
                            scheduleSeparately = settingsState.value.scheduleSeparately,
                            setHome = settingsState.value.setHomeWallpaper,
                            setLock = settingsState.value.setLockWallpaper
                        )
                        alarmItem.let{scheduler.scheduleWallpaperAlarm(it, null, true, true)}
                        scheduler.scheduleRefresh()
                    }
                },
                onDarkenPercentage = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarkenPercentage(it))
                    if (settingsState.value.enableChanger && settingsState.value.darken) {
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                        }
                    }
                },
                onDarkCheck = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarken(it))
                    if (settingsState.value.enableChanger && (settingsState.value.darken && settingsState.value.darkenPercentage < 100)) {
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                        }
                    }
                },
                scaling = settingsState.value.wallpaperScaling,
                onScalingChange = {
                    if (settingsState.value.wallpaperScaling != it) {
                        settingsViewModel.onEvent(SettingsEvent.SetWallpaperScaling(it))
                        if (settingsState.value.enableChanger) {
                            job?.cancel()
                            job = scope.launch {
                                delay(2000)
                                scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                            }
                        }
                    }
                },
                onHomeCheckedChange = { setHome -> settingsViewModel.onEvent(SettingsEvent.SetHome(setHome))
                    if (selectedState.value.selectedAlbum != null && !setHome && !settingsState.value.setLockWallpaper) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        selectedState.value.selectedAlbum?.let {
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset())
                        }
                        scheduler.cancelWallpaperAlarm()
                    }
                    else if (selectedState.value.selectedAlbum != null && (setHome && !settingsState.value.setLockWallpaper) || (!setHome && settingsState.value.setLockWallpaper)) {
                        settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(false))
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.homeInterval,
                                lockInterval = settingsState.value.lockInterval,
                                scheduleSeparately = false,
                                setHome = setHome,
                                setLock = settingsState.value.setLockWallpaper
                            )
                            alarmItem.let{scheduler.scheduleWallpaperAlarm(it, null, true, true)}
                            scheduler.scheduleRefresh()
                        }
                    }
                    else if (selectedState.value.selectedAlbum != null && settingsState.value.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                        }
                    }
                },
                onLockCheckedChange = { setLock -> settingsViewModel.onEvent(SettingsEvent.SetLock(setLock))
                    if (selectedState.value.selectedAlbum!= null && !setLock && !settingsState.value.setHomeWallpaper) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        selectedState.value.selectedAlbum?.let {
                            wallpaperScreenViewModel.onEvent(WallpaperEvent.Reset())
                        }
                        scheduler.cancelWallpaperAlarm()
                    }
                    else if (selectedState.value.selectedAlbum!= null && (setLock && !settingsState.value.setHomeWallpaper) || (!setLock && settingsState.value.setHomeWallpaper)) {
                        settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(false))
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.homeInterval,
                                lockInterval = settingsState.value.lockInterval,
                                scheduleSeparately = false,
                                setHome = settingsState.value.setHomeWallpaper,
                                setLock = setLock
                            )
                            alarmItem.let{scheduler.scheduleWallpaperAlarm(it, null, true, true)}
                            scheduler.scheduleRefresh()
                        }
                    }
                    else if (selectedState.value.selectedAlbum != null && settingsState.value.enableChanger) {
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                        }
                    }
                },
                onScheduleSeparatelyChange = { changeSeparately ->
                    settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(changeSeparately))
                    if (selectedState.value.selectedAlbum != null && settingsState.value.enableChanger) {
                        //update wallpaper preview
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            val alarmItem = WallpaperAlarmItem(
                                homeInterval = settingsState.value.homeInterval,
                                lockInterval = settingsState.value.lockInterval,
                                scheduleSeparately = changeSeparately,
                                setHome = settingsState.value.setHomeWallpaper,
                                setLock = settingsState.value.setLockWallpaper
                            )
                            alarmItem.let{scheduler.scheduleWallpaperAlarm(it, null, true, true)}
                            scheduler.scheduleRefresh()
                        }
                    }
                },
                onBlurChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetBlur(it))
                    if (settingsState.value.enableChanger && (settingsState.value.blur && settingsState.value.blurPercentage > 0)) {
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                        }
                    }
                },
                onBlurPercentageChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetBlurPercentage(it))
                    if (settingsState.value.enableChanger && settingsState.value.blur) {
                        job?.cancel()
                        job = scope.launch {
                            delay(2000)
                            scheduler.updateWallpaper(settingsState.value.scheduleSeparately, settingsState.value.setHomeWallpaper, settingsState.value.setLockWallpaper)
                        }
                    }
                },
            )
        }
        // Navigate to the add album screen to create a new album and add wallpapers to it
        composable<AddEdit>(
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) { backStackEntry ->
            val addEdit: AddEdit = backStackEntry.toRoute()
            AddAlbumScreen(
                initialAlbumName = addEdit.wallpaper,
                onBackClick = { navController.navigateUp() },
                onConfirmation = { navController.navigateUp() },
                onShowWallpaperView = {
                    navController.navigate(WallpaperView(it))
                },
                onShowFolderView = { folderName, wallpapers ->
                    val encodedWallpapers = runBlocking { encodeUri(uri = Gson().toJson(wallpapers)) }
                    navController.navigate("${NavScreens.FolderView.route}/$folderName/$encodedWallpapers")
                },
                animate = settingsState.value.animate
            )
        }
        // Navigate to wallpaper view screen to view individual wallpapers in full screen
        composable<WallpaperView> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) { backStackEntry ->
            val wallpaperView: WallpaperView = backStackEntry.toRoute()
            WallpaperViewScreen(
                wallpaperUri = wallpaperView.wallpaper,
                onBackClick = { navController.navigateUp() },
                animate = settingsState.value.animate
            )
        }
        composable(
            route = NavScreens.FolderView.route.plus("/{folderName}/{wallpapers}"),
            arguments = listOf(
                navArgument("folderName") { type = NavType.StringType },
                navArgument("wallpapers") { type = NavType.StringType }
            ),
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName")
            val wallpapers = Gson().fromJson(
                backStackEntry.arguments?.getString("wallpapers"),
                Array<String>::class.java
            ).toList()
            if (wallpapers.isNotEmpty()) {
                FolderViewScreen(
                    folderName = folderName,
                    wallpapers = wallpapers,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = {
                        navController.navigate(WallpaperView(it))
                    },
                    animate = settingsState.value.animate
                )
            } else {
                navController.navigateUp()
            }
        }
        // Navigate to the album view screen to view folders and wallpapers in an album
        composable<AlbumView> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) { backStackEntry ->
            val albumView: AlbumView = backStackEntry.toRoute()
            val albumWithWallpaper = albumState.value.albumsWithWallpapers.find { it.album.initialAlbumName == albumView.initialAlbumName }
            if (albumWithWallpaper != null) {
                AlbumViewScreen(
                    album = albumWithWallpaper,
                    animate = settingsState.value.animate,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = {
                        navController.navigate(WallpaperView(it))
                    },
                    onShowFolderView = { folderName, wallpapers ->
                        val encodedWallpapers = runBlocking { encodeUri(uri = Gson().toJson(wallpapers)) }
                        navController.navigate("${NavScreens.FolderView.route}/$folderName/$encodedWallpapers")
                    },
                    onDeleteAlbum = {
                        navController.navigateUp()
                        albumsViewModel.onEvent(AlbumsEvent.DeleteAlbumWithWallpapers(albumWithWallpaper))
                    },
                    onTitleChange = { title, originalAlbumWithWallpaper ->
                        albumsViewModel.onEvent(AlbumsEvent.ChangeAlbumName(title, originalAlbumWithWallpaper))
                    },
                    onSelectionDeleted = {
                        albumsViewModel.onEvent(AlbumsEvent.RefreshAlbums)
                    }
                )
            } else { navController.navigateUp() }
        }
        // Navigate to the settings screen to change app settings
        composable<Settings> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) {
            SettingsScreen(
                settingsState = settingsViewModel.state,
                topInsets = topInsets,
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
        composable<Privacy> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) {
            PrivacyScreen(
                topInsets = topInsets,
                onBackClick = { navController.navigateUp() },
            )
        }
        // Navigate to the licenses screen to view the licenses of the libraries used
        composable<Licenses> (
            enterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { (it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            exitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popEnterTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionIn(initial = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            },
            popExitTransition = {
                if (settingsState.value.animate) {
                    sharedXTransitionOut(target = { -(it * INITIAL_OFFSET).toInt() })
                } else { null }
            }
        ) {
            LicensesScreen(
                topInsets = topInsets,
                onBackClick = { navController.navigateUp() },
            )
        }
    }
}

// Encode an URI so it can be passed with navigation
suspend fun encodeUri(uri: String): String =
    withContext(Dispatchers.IO) {
        URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
    }