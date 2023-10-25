package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsEvent
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsLargeTopAppBar
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsTitle
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsSmallTopAppBar
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
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
            settingsState = viewModel.state,
            largeTopAppBarHeightPx = largeTopAppBarHeight,
            scroll = scroll,
            onDynamicThemingClick = {
                viewModel.onEvent(SettingsEvent.SetDynamicTheming(it))
            },
            onDarkModeClick = {
                viewModel.onEvent(SettingsEvent.SetDarkMode(it))
            }
        )
        SettingsSmallTopAppBar(onBackClick = onBackClick)
        SettingsTitle(
            scroll = scroll,
            largeTopAppBarHeight = largeTopAppBarHeight,
            smallTopAppBarHeight = smallTopAppBarHeight,
            paddingMedium = paddingMedium
        )
    }
}