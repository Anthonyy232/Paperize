package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsTitle
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsSmallTopAppBar
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(
    settingsState: StateFlow<SettingsState>,
    onBackClick: () -> Unit,
    onDarkModeClick: (Boolean?) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onAnimateClick: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLicenseClick: () -> Unit
) {
    val scroll: ScrollState = rememberScrollState(0)
    val largeTopAppBarHeight = 152.dp
    val smallTopAppBarHeight = 64.dp
    val paddingMedium = 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsScrollableSettings(
            settingsState = settingsState,
            largeTopAppBarHeightPx = largeTopAppBarHeight,
            scroll = scroll,
            onDarkModeClick = onDarkModeClick,
            onDynamicThemingClick = onDynamicThemingClick,
            onAnimateClick = onAnimateClick,
            onPrivacyClick = onPrivacyClick,
            onLicenseClick = onLicenseClick
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