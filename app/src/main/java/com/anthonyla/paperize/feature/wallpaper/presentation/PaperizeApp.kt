package com.anthonyla.paperize.feature.wallpaper.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumScreenViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.album_view_screen.AlbumViewEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.folder_view_screen.FolderViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.notifications_screen.NotificationScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.PrivacyScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.SortEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.SortViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.SortViewScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.startup_screen.StartupScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_view_screen.WallpaperViewScreen
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AddAlbum
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AlbumView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.FolderView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Home
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Notification
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Privacy
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Settings
import com.anthonyla.paperize.feature.wallpaper.util.navigation.SortView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.Startup
import com.anthonyla.paperize.feature.wallpaper.util.navigation.WallpaperView
import com.anthonyla.paperize.feature.wallpaper.util.navigation.animatedScreen
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmItem
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAlarmSchedulerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperizeApp(
    firstLaunch: Boolean,
    scheduler : WallpaperAlarmSchedulerImpl,
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    albumScreenViewModel: AlbumScreenViewModel = hiltViewModel(),
    addAlbumViewModel: AddAlbumViewModel = hiltViewModel(),
    folderViewModel: FolderViewModel = hiltViewModel(),
    sortViewModel: SortViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
    val albumState = albumsViewModel.state.collectAsStateWithLifecycle()
    val sortState = sortViewModel.state.collectAsStateWithLifecycle()
    val folderState = folderViewModel.state.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = if (firstLaunch) Startup else Home,
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
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
                homeSelectedAlbum = albumState.value.selectedAlbum.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.homeAlbumName },
                lockSelectedAlbum = albumState.value.selectedAlbum.find { it.album.initialAlbumName == settingsState.value.wallpaperSettings.lockAlbumName },
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
                        job = scope.updateWallpaperAlarm(
                            job = job,
                            settingsState = settingsState.value.copy(
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    homeInterval = timeInMinutes
                                )
                            ),
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onLockTimeChange = { timeInMinutes ->
                    settingsViewModel.onEvent(SettingsEvent.SetLockWallpaperInterval(timeInMinutes))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaperAlarm(
                            job = job,
                            settingsState = settingsState.value.copy(
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    lockInterval = timeInMinutes
                                )
                            ),
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onDeselect = { lock, home ->
                    if (albumState.value.selectedAlbum.isNotEmpty()) {
                        val notSameAlbum = settingsState.value.wallpaperSettings.homeAlbumName != settingsState.value.wallpaperSettings.lockAlbumName
                        when {
                            lock && home -> {
                                settingsState.value.wallpaperSettings.lockAlbumName?.let {
                                    albumsViewModel.onEvent(AlbumsEvent.RemoveSelectedAlbum(it))
                                }
                            }
                            lock -> {
                                if (notSameAlbum) {
                                    settingsState.value.wallpaperSettings.lockAlbumName?.let {
                                        albumsViewModel.onEvent(AlbumsEvent.RemoveSelectedAlbum(it))
                                    }
                                }
                            }
                            home -> {
                                if (notSameAlbum) {
                                    settingsState.value.wallpaperSettings.homeAlbumName?.let {
                                        albumsViewModel.onEvent(AlbumsEvent.RemoveSelectedAlbum(it))
                                    }
                                }
                            }
                        }
                        settingsViewModel.onEvent(SettingsEvent.RemoveSelectedAlbumAsType(lock, home))
                        scheduler.cancelWallpaperAlarm()
                    }
                },
                onToggleChanger = { enableWallpaperChanger ->
                    if (albumState.value.selectedAlbum.isNotEmpty() && !settingsState.value.wallpaperSettings.homeAlbumName.isNullOrEmpty() && !settingsState.value.wallpaperSettings.lockAlbumName.isNullOrEmpty()) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(enableWallpaperChanger))
                        if (enableWallpaperChanger) {
                            job = scope.scheduleWallpaperUpdate(
                                job = job,
                                settingsState = settingsState.value.copy(
                                    wallpaperSettings = settingsState.value.wallpaperSettings.copy(
                                        enableChanger = true
                                    )
                                ),
                                settingsViewModel = settingsViewModel,
                                scheduler = scheduler,
                                context = context,
                                refreshNextTime = true
                            )
                        }
                        else { scheduler.cancelWallpaperAlarm() }
                    }
                },
                onSelectAlbum = { album, lock, home ->
                    val currentSettings = settingsState.value
                    val notSameAlbum = currentSettings.wallpaperSettings.lockAlbumName != currentSettings.wallpaperSettings.homeAlbumName
                    val newLockAlbum = if (lock) album.album.initialAlbumName else null
                    val newHomeAlbum = if (home) album.album.initialAlbumName else null
                    settingsViewModel.onEvent(SettingsEvent.SetAlbum(
                        homeAlbumName = newHomeAlbum,
                        lockAlbumName = newLockAlbum
                    ))
                    albumsViewModel.onEvent(
                        AlbumsEvent.AddSelectedAlbum(
                            album = album,
                            deselectAlbumName = when {
                                notSameAlbum && lock -> currentSettings.wallpaperSettings.lockAlbumName
                                notSameAlbum && home -> currentSettings.wallpaperSettings.homeAlbumName
                                else -> null
                            },
                            shuffle = currentSettings.scheduleSettings.shuffle
                        )
                    )
                    val shouldSchedule =
                        (home && !currentSettings.wallpaperSettings.lockAlbumName.isNullOrEmpty()) ||
                            (lock && !currentSettings.wallpaperSettings.homeAlbumName.isNullOrEmpty()) ||
                                (lock && home)

                    if (shouldSchedule) {
                        job = scope.scheduleWallpaperUpdate(
                            job = job,
                            settingsState = settingsState.value.copy(
                                wallpaperSettings = currentSettings.wallpaperSettings.copy(
                                    homeAlbumName = newHomeAlbum ?: currentSettings.wallpaperSettings.homeAlbumName,
                                    lockAlbumName = newLockAlbum ?: currentSettings.wallpaperSettings.lockAlbumName
                                )
                            ),
                            settingsViewModel = settingsViewModel,
                            scheduler = scheduler,
                            context = context,
                            refreshNextTime = true,
                            changeImmediate = true,
                            cancelImmediate = true,
                            firstLaunch = true,
                            delay = 500L
                        )
                    }
                },
                onDarkenPercentage = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetDarkenPercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.darken) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    lockDarkenPercentage = lock,
                                    homeDarkenPercentage = home
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onDarkCheck = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarken(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    darken = it
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onScalingChange = {
                    if (settingsState.value.wallpaperSettings.wallpaperScaling != it) {
                        settingsViewModel.onEvent(SettingsEvent.SetWallpaperScaling(it))
                        if (settingsState.value.wallpaperSettings.enableChanger) {
                            job = scope.updateWallpaper(
                                job = job,
                                settingsState = settingsState.value.copy(
                                    wallpaperSettings = settingsState.value.wallpaperSettings.copy(
                                        wallpaperScaling = it
                                    )
                                ),
                                scheduler = scheduler
                            )
                        }
                    }
                },
                onHomeCheckedChange = { setHome ->
                    settingsViewModel.onEvent(SettingsEvent.SetHome(setHome))
                    if (albumState.value.selectedAlbum.isNotEmpty() && !setHome && !settingsState.value.wallpaperSettings.setLockWallpaper) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        albumState.value.selectedAlbum.let { albumsViewModel.onEvent(AlbumsEvent.DeselectSelected) }
                        scheduler.cancelWallpaperAlarm()
                    }
                    else if (albumState.value.selectedAlbum.isNotEmpty() && ((setHome && !settingsState.value.wallpaperSettings.setLockWallpaper) || (!setHome && settingsState.value.wallpaperSettings.setLockWallpaper))) {
                        settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(false))
                        job = scope.scheduleWallpaperUpdate(
                            job = job,
                            settingsState = settingsState.value.copy(
                                wallpaperSettings = settingsState.value.wallpaperSettings.copy(
                                    setHomeWallpaper = setHome
                                ),
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    scheduleSeparately = false
                                )
                            ),
                            settingsViewModel = settingsViewModel,
                            scheduler = scheduler,
                            context = context
                        )
                    }
                    else if (albumState.value.selectedAlbum.isNotEmpty() && settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.scheduleWallpaperUpdate(
                            job = job,
                            settingsState = settingsState.value.copy(
                                wallpaperSettings = settingsState.value.wallpaperSettings.copy(
                                    setHomeWallpaper = setHome
                                )
                            ),
                            settingsViewModel = settingsViewModel,
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onLockCheckedChange = { setLock ->
                    settingsViewModel.onEvent(SettingsEvent.SetLock(setLock))
                    if (!setLock && !settingsState.value.wallpaperSettings.setHomeWallpaper) {
                        settingsViewModel.onEvent(SettingsEvent.SetChangerToggle(false))
                        albumState.value.selectedAlbum.let {
                            albumsViewModel.onEvent(AlbumsEvent.DeselectSelected)
                        }
                        scheduler.cancelWallpaperAlarm()
                    }
                    else if (albumState.value.selectedAlbum.isNotEmpty() && ((setLock && !settingsState.value.wallpaperSettings.setHomeWallpaper) || (!setLock && settingsState.value.wallpaperSettings.setHomeWallpaper))) {
                        settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(false))
                        job = scope.scheduleWallpaperUpdate(
                            job = job,
                            settingsState = settingsState.value.copy(
                                wallpaperSettings = settingsState.value.wallpaperSettings.copy(
                                    setLockWallpaper = setLock
                                ),
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    scheduleSeparately = false
                                )
                            ),
                            settingsViewModel = settingsViewModel,
                            scheduler = scheduler,
                            context = context
                        )
                    }
                    else if (albumState.value.selectedAlbum.isNotEmpty() && settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.scheduleWallpaperUpdate(
                            job = job,
                            settingsState = settingsState.value.copy(
                                wallpaperSettings = settingsState.value.wallpaperSettings.copy(
                                    setLockWallpaper = setLock
                                )
                            ),
                            settingsViewModel = settingsViewModel,
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onScheduleSeparatelyChange = { changeSeparately ->
                    settingsViewModel.onEvent(SettingsEvent.SetScheduleSeparately(changeSeparately))
                    if (albumState.value.selectedAlbum.isNotEmpty() && settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.scheduleWallpaperUpdate(
                            job = job,
                            settingsState = settingsState.value.copy(
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    scheduleSeparately = changeSeparately
                                )
                            ),
                            settingsViewModel = settingsViewModel,
                            scheduler = scheduler,
                            context = context,
                            changeImmediate = true,
                            cancelImmediate = true,
                            firstLaunch = true
                        )
                    }
                },
                onBlurChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetBlur(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    blur = it
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onBlurPercentageChange = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetBlurPercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.blur) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    lockBlurPercentage = lock,
                                    homeBlurPercentage = home
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onVignetteChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetVignette(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    vignette = it
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onVignettePercentageChange = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetVignettePercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.vignette) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    lockVignettePercentage = lock,
                                    homeVignettePercentage = home
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onGrayscaleChange = {
                    settingsViewModel.onEvent(SettingsEvent.SetGrayscale(it))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    grayscale = it
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onGrayscalePercentageChange = { home, lock ->
                    settingsViewModel.onEvent(SettingsEvent.SetGrayscalePercentage(home, lock))
                    if (settingsState.value.wallpaperSettings.enableChanger && settingsState.value.effectSettings.grayscale) {
                        job = scope.updateWallpaper(
                            job = job,
                            settingsState = settingsState.value.copy(
                                effectSettings = settingsState.value.effectSettings.copy(
                                    lockGrayscalePercentage = lock,
                                    homeGrayscalePercentage = home
                                )
                            ),
                            scheduler = scheduler
                        )
                    }
                },
                onStartTimeChange = { time ->
                    settingsViewModel.onEvent(SettingsEvent.SetStartTime(time.hour, time.minute))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaperAlarm(
                            job = job,
                            settingsState = settingsState.value.copy(
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    startTime = Pair(time.hour, time.minute))
                            ),
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onChangeStartTimeToggle = { changeStartTime ->
                    settingsViewModel.onEvent(SettingsEvent.SetChangeStartTime(changeStartTime))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaperAlarm(
                            job = job,
                            settingsState = settingsState.value.copy(
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    changeStartTime = changeStartTime
                                )
                            ),
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onShuffleCheck = { shuffle ->
                    settingsViewModel.onEvent(SettingsEvent.SetShuffle(shuffle))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        job = scope.updateWallpaperAlarm(
                            job = job,
                            settingsState = settingsState.value.copy(
                                scheduleSettings = settingsState.value.scheduleSettings.copy(
                                    shuffle = shuffle
                                )
                            ),
                            scheduler = scheduler,
                            context = context
                        )
                    }
                },
                onRefreshChange = { refresh ->
                    settingsViewModel.onEvent(SettingsEvent.SetRefresh(refresh))
                    if (settingsState.value.wallpaperSettings.enableChanger) {
                        WallpaperAlarmSchedulerImpl.scheduleRefresh(context, refresh)
                    }
                },
                onSkipLandscapeChange = { skipLandscape ->
                    settingsViewModel.onEvent(SettingsEvent.SetSkipLandscape(skipLandscape))
                },
                onSkipNonInteractiveChange = {
                    skipNonInteractive ->
                    settingsViewModel.onEvent(SettingsEvent.SetSkipNonInteractive(skipNonInteractive))
                }
            )
        }

        // Navigate to the add album screen to create a new album and add wallpapers to it
        animatedScreen<AddAlbum>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val addAlbum: AddAlbum = backStackEntry.toRoute()
            AddAlbumScreen(
                addAlbumViewModel = addAlbumViewModel,
                initialAlbumName = addAlbum.initialAlbumName,
                onBackClick = {
                    navController.navigateUp()
                },
                onConfirmation = {
                    navController.navigateUp()
                },
                onShowWallpaperView = { uri, name ->
                    navController.navigate(WallpaperView(uri, name))
                },
                onShowFolderView = { folder ->
                    if (folder.wallpapers.isNotEmpty()) {
                        folderViewModel.onEvent(FolderEvent.LoadFolderView(folder))
                        navController.navigate(FolderView)
                    }
                },
                onSortClick = { folders, wallpapers ->
                    if (folders.isNotEmpty() || wallpapers.isNotEmpty()) {
                        sortViewModel.onEvent(SortEvent.LoadSortView(folders, wallpapers))
                        navController.navigate(SortView)
                    }
                },
                animate = settingsState.value.themeSettings.animate
            )
        }

        // Navigate to wallpaper view screen to view individual wallpapers in full screen
        animatedScreen<WallpaperView>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val wallpaperView: WallpaperView = backStackEntry.toRoute()
            WallpaperViewScreen(
                wallpaperUri = wallpaperView.wallpaperUri,
                wallpaperName = wallpaperView.wallpaperName,
                onBackClick = { navController.navigateUp() },
                animate = settingsState.value.themeSettings.animate
            )
        }

        // Navigate to the folder view screen to view wallpapers in a folder
        animatedScreen<FolderView>(animate = settingsState.value.themeSettings.animate) {
            if (folderState.value.folder == null || folderState.value.folder!!.wallpapers.isEmpty()) {
                navController.navigateUp()
            }
            else {
                FolderViewScreen(
                    folder = folderState.value.folder!!,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = { uri, name ->
                        navController.navigate(WallpaperView(uri, name))
                    },
                    animate = settingsState.value.themeSettings.animate
                )
            }
        }

        // Navigate to sort view screen to sort wallpapers and folders
        animatedScreen<SortView>(animate = settingsState.value.themeSettings.animate) {
            if (sortState.value.folders.isEmpty() && sortState.value.wallpapers.isEmpty()) {
                navController.navigateUp()
            }
            else {
                SortViewScreen(
                    sortViewModel = sortViewModel,
                    onSaveClick = {
                        val previousScreen = navController.previousBackStackEntry?.destination?.route
                        if (previousScreen?.contains(AddAlbum::class.simpleName.toString()) ?: false) {
                            addAlbumViewModel.onEvent(AddAlbumEvent.LoadFoldersAndWallpapers(
                                folders = sortState.value.folders,
                                wallpapers = sortState.value.wallpapers
                            ))
                            navController.navigateUp()
                        }
                        else if (previousScreen?.contains(AlbumView::class.simpleName.toString()) ?: false) {
                            albumScreenViewModel.onEvent(AlbumViewEvent.LoadFoldersAndWallpapers(
                                folders = sortState.value.folders,
                                wallpapers = sortState.value.wallpapers
                            ))
                            navController.navigateUp()
                        }
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        // Navigate to the album view screen to view folders and wallpapers in an album
        animatedScreen<AlbumView>(animate = settingsState.value.themeSettings.animate) { backStackEntry ->
            val albumView: AlbumView = backStackEntry.toRoute()
            if (albumView.initialAlbumName.isNotEmpty()) {
                albumScreenViewModel.onEvent(AlbumViewEvent.LoadAlbum(albumView.initialAlbumName))
                AlbumViewScreen(
                    albumScreenViewModel = albumScreenViewModel ,
                    animate = settingsState.value.themeSettings.animate,
                    onBackClick = { navController.navigateUp() },
                    onShowWallpaperView = { uri, name ->
                        navController.navigate(WallpaperView(uri, name))
                    },
                    onShowFolderView = { folder ->
                        if (folder.wallpapers.isNotEmpty()) {
                            folderViewModel.onEvent(FolderEvent.LoadFolderView(folder))
                            navController.navigate(FolderView)
                        }
                    },
                    onDeleteAlbum = {
                        navController.navigateUp()
                    },
                    onSortClick = { folders, wallpapers ->
                        if (folders.isNotEmpty() || wallpapers.isNotEmpty()) {
                            sortViewModel.onEvent(SortEvent.LoadSortView(folders, wallpapers))
                            navController.navigate(SortView)
                        }
                    },
                )
            }
            else { navController.navigateUp() }
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
                onContactClick = {
                    SendContactIntent(context)
                },
                onResetClick = {
                    settingsViewModel.onEvent(SettingsEvent.Reset)
                    albumsViewModel.onEvent(AlbumsEvent.Reset)
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
    }
}

@SuppressLint("MissingPermission")
private fun CoroutineScope.scheduleWallpaperUpdate(
    job: Job?,
    settingsState: SettingsState,
    settingsViewModel: SettingsViewModel,
    scheduler: WallpaperAlarmSchedulerImpl,
    context: android.content.Context,
    refreshNextTime: Boolean = false,
    changeImmediate: Boolean = true,
    cancelImmediate: Boolean = true,
    firstLaunch: Boolean = true,
    origin: Int? = null,
    delay: Long = 0L
): Job {
    job?.cancel()
    return launch {
        if (delay > 0) delay(delay)
        if (refreshNextTime) {
            settingsViewModel.onEvent(SettingsEvent.RefreshNextSetTime)
        }

        val alarmItem = WallpaperAlarmItem(
            homeInterval = settingsState.scheduleSettings.homeInterval,
            lockInterval = settingsState.scheduleSettings.lockInterval,
            scheduleSeparately = settingsState.scheduleSettings.scheduleSeparately,
            setHome = settingsState.wallpaperSettings.setHomeWallpaper,
            setLock = settingsState.wallpaperSettings.setLockWallpaper,
            changeStartTime = settingsState.scheduleSettings.changeStartTime,
            startTime = settingsState.scheduleSettings.startTime,
            shuffle = settingsState.scheduleSettings.shuffle
        )

        scheduler.scheduleWallpaperAlarm(
            wallpaperAlarmItem = alarmItem,
            origin = origin,
            changeImmediate = changeImmediate,
            cancelImmediate = cancelImmediate,
            firstLaunch = firstLaunch
        )
        WallpaperAlarmSchedulerImpl.scheduleRefresh(context, settingsState.scheduleSettings.refresh)
    }
}

private fun CoroutineScope.updateWallpaperAlarm(
    job: Job?,
    settingsState: SettingsState,
    scheduler: WallpaperAlarmSchedulerImpl,
    context: android.content.Context,
    firstLaunch: Boolean = true,
): Job {
    job?.cancel()
    return launch {
        val alarmItem = WallpaperAlarmItem(
            homeInterval = settingsState.scheduleSettings.homeInterval,
            lockInterval = settingsState.scheduleSettings.lockInterval,
            scheduleSeparately = settingsState.scheduleSettings.scheduleSeparately,
            setHome = settingsState.wallpaperSettings.setHomeWallpaper,
            setLock = settingsState.wallpaperSettings.setLockWallpaper,
            changeStartTime = settingsState.scheduleSettings.changeStartTime,
            startTime = settingsState.scheduleSettings.startTime,
            shuffle = settingsState.scheduleSettings.shuffle
        )

        scheduler.updateWallpaperAlarm(
            wallpaperAlarmItem = alarmItem,
            firstLaunch = firstLaunch
        )
        WallpaperAlarmSchedulerImpl.scheduleRefresh(context, settingsState.scheduleSettings.refresh)
    }
}

private fun CoroutineScope.updateWallpaper(
    job: Job?,
    settingsState: SettingsState,
    scheduler: WallpaperAlarmSchedulerImpl,
): Job {
    job?.cancel()
    return launch {
        delay(500L)
        scheduler.updateWallpaper(
            scheduleSeparately = settingsState.scheduleSettings.scheduleSeparately,
            setHome = settingsState.wallpaperSettings.setHomeWallpaper,
            setLock = settingsState.wallpaperSettings.setLockWallpaper,
        )
    }
}