package com.anthonyla.livewallpaper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.anthonyla.livewallpaper.navigation.BottomNavScreens
import com.anthonyla.livewallpaper.navigation.BottomNavigationBar
import com.anthonyla.livewallpaper.navigation.bottomNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Bottom navigation
    val bottomNavOptions = listOf(
        BottomNavScreens.Wallpaper,
        BottomNavScreens.Library,
        BottomNavScreens.Setting
    )
    Scaffold (
        bottomBar = { BottomNavigationBar(navController = navController, screens = bottomNavOptions) },
    ) { innerPadding ->
        NavHost(navController, startDestination = "bottomNavigation", Modifier.padding(innerPadding)) {
            bottomNav(navController)
        }
    }
}