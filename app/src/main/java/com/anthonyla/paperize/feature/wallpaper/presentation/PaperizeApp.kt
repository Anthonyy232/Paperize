package com.anthonyla.paperize.feature.wallpaper.presentation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anthonyla.paperize.core.presentation.components.BottomNavigationBar
import com.anthonyla.paperize.core.presentation.components.TopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.library.components.AnimatedFab
import com.anthonyla.paperize.feature.wallpaper.presentation.library.components.FabMenuOptions
import com.anthonyla.paperize.feature.wallpaper.util.navigation.AddEditNavScreens
import com.anthonyla.paperize.feature.wallpaper.util.navigation.BottomNavScreens
import com.anthonyla.paperize.feature.wallpaper.util.navigation.SettingsNavScreens
import com.anthonyla.paperize.feature.wallpaper.util.navigation.navGraph

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PaperizeApp(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Scaffold (
        topBar = {
            val showBackButton = navBackStackEntry?.destination?.route in listOf(
                SettingsNavScreens.Settings.route,
                AddEditNavScreens.ImageAdd.route
            )
            val showDropDownMenu = navBackStackEntry?.destination?.route in listOf(
                BottomNavScreens.Wallpaper.route,
                BottomNavScreens.Library.route,
                BottomNavScreens.Configure.route
            )
            val showTitle = when (navBackStackEntry?.destination?.route) {
                SettingsNavScreens.Settings.route -> "Settings"
                AddEditNavScreens.ImageAdd.route -> ""
                else -> ""
            }
            TopBar(
                navController = navController,
                title = showTitle,
                showBackButton = showBackButton,
                showMenuButton = showDropDownMenu
            )
        },
        bottomBar = {
            val showNavigationBar = navBackStackEntry?.destination?.route in listOf(
                BottomNavScreens.Wallpaper.route,
                BottomNavScreens.Library.route,
                BottomNavScreens.Configure.route
            )
            if (showNavigationBar) BottomNavigationBar(
                navController = navController,
                screens = listOf(
                    BottomNavScreens.Wallpaper,
                    BottomNavScreens.Library,
                    BottomNavScreens.Configure
                )
            )
        },
        floatingActionButton = {
            // Show floating action button if current screen is in the list
            val showFabButton = navBackStackEntry?.destination?.route in listOf(
                BottomNavScreens.Library.route,
            )
            var optionClicked by rememberSaveable { mutableStateOf("") }
            if (showFabButton) {
                AnimatedFab(FabMenuOptions()) {
                    optionClicked = it
                    val options = FabMenuOptions()
                    when (optionClicked) {
                        options.imageOption.id ->
                            navController.navigate(AddEditNavScreens.ImageAdd.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        options.folderOption.id -> null
                    }
                }
            }
        }
    ) { innerPadding -> NavHost (
            navController,
            startDestination = "bottomNavigation",
            Modifier.padding(innerPadding)
        ) { navGraph(navController) }
    }
}