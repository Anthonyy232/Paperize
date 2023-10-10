package com.anthonyla.paperize.feature.wallpaper.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anthonyla.paperize.data.Contact
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.HomeScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsScreen
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.WallpaperEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper.WallpaperViewModel
import com.anthonyla.paperize.feature.wallpaper.util.navigation.NavScreens

@Composable
fun PaperizeApp(
    wallpaperViewModel: WallpaperViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    /** Image picker **/
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            for (uri in uris) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                wallpaperViewModel.onEvent(WallpaperEvent.AddWallpaper(uri.toString()))
            }
        }
    )

    val wallpaperState = wallpaperViewModel.state.value
    val settingsState = settingsViewModel.state.value
    val navController = rememberNavController()
    var toContact by rememberSaveable { mutableStateOf(false) }
    if (toContact) { Contact(LocalContext.current) }

    NavHost(
        navController = navController,
        startDestination = NavScreens.Home.route,
        modifier = Modifier.navigationBarsPadding()
    ) {
        composable(NavScreens.Home.route) {
            HomeScreen(
                wallpaperState = wallpaperState,
                settingsState = settingsState,
                onSettingsClick = { navController.navigate(NavScreens.Settings.route) },
                onContactClick = { toContact = true },
                onLaunchImagePhotoPicker = {
                    multiplePhotoPickerLauncher.launch(arrayOf("image/*"))
                },
                onDeleteImagesClick = { image ->
                    image.forEach {
                        wallpaperViewModel.onEvent(WallpaperEvent.DeleteWallpaper(it))
                    }
                }
            )
        }
        composable(NavScreens.Settings.route) {
            SettingsScreen(
                settingsState = settingsState,
                onBackClick = { navController.navigateUp() },
                onDynamicThemingClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDynamicTheming(it))
                },
                onDarkModeClick = {
                    settingsViewModel.onEvent(SettingsEvent.SetDarkMode(it))
                }
            )
        }
    }
}