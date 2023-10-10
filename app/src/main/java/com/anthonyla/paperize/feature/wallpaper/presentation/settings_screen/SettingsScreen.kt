package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components.HomeTopBar
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsLargeTopAppBar
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.Title
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsSmallTopAppBar

@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    onBackClick: () -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onDarkModeClick: (Boolean?) -> Unit
) {
    val scroll: ScrollState = rememberScrollState(0)
    val largeTopAppBarHeight = 152.dp
    val smallTopAppBarHeight = 64.dp
    val paddingMedium = 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsLargeTopAppBar(
            scroll = scroll,
            modifier = Modifier.height(largeTopAppBarHeight)
        )
        ScrollableSettings(
            settingsState = settingsState,
            largeTopAppBarHeightPx = largeTopAppBarHeight,
            scroll = scroll,
            onDynamicThemingClick = onDynamicThemingClick,
            onDarkModeClick = onDarkModeClick
        )
        SettingsSmallTopAppBar(onBackClick = onBackClick)
        Title(
            scroll = scroll,
            largeTopAppBarHeight = largeTopAppBarHeight,
            smallTopAppBarHeight = smallTopAppBarHeight,
            paddingMedium = paddingMedium
        )
    }
}