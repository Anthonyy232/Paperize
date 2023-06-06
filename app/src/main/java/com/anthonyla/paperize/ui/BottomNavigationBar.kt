package com.anthonyla.paperize.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarDefaults.containerColor
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anthonyla.paperize.data.navigation.BottomNavScreens


/**
 * Implementation of the bottom navigation bar composable
 * Three screens = Wallpaper, Collection, Configure
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    screens: List<BottomNavScreens>,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor),
        tonalElevation = NavigationBarDefaults.Elevation,
        windowInsets = NavigationBarDefaults.windowInsets,
        modifier = Modifier
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        screens.forEach { screen ->
            NavigationBarItem(
                colors  = NavigationBarItemDefaults.colors(),
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                label = {
                    Text(
                        text = stringResource(id = screen.resourceId),
                        fontWeight = FontWeight.SemiBold,
                    ) },
                icon = {
                    if (currentDestination != null) {
                        Icon(imageVector =
                        if (currentDestination.route == screen.route) screen.filledIcon else screen.unfilledIcon,
                            contentDescription = "${stringResource(id = screen.resourceId)} Icon",
                        )
                    }
                },
                alwaysShowLabel = false,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}