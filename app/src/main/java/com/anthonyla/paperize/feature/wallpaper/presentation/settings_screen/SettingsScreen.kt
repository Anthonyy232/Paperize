package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsLargeTopAppBar
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsTitle
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsSmallTopAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(
    settingsState: StateFlow<SettingsState>,
    onBackClick: () -> Unit,
    onDarkModeClick: (Boolean?) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit
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
            onDarkModeClick = onDarkModeClick,
            onDynamicThemingClick = onDynamicThemingClick
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