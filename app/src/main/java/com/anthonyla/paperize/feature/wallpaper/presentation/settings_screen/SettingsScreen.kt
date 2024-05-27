package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.SettingsTitle
import kotlinx.coroutines.flow.StateFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: StateFlow<SettingsState>,
    topInsets: Dp,
    onBackClick: () -> Unit,
    onDarkModeClick: (Boolean?) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onAnimateClick: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val scroll: ScrollState = rememberScrollState(0)
    val largeTopAppBarHeight = TopAppBarDefaults.LargeAppBarExpandedHeight
    val smallTopAppBarHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight
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
            onLicenseClick = onLicenseClick,
            onResetClick = onResetClick,
            onBackClick = onBackClick,
        )
        SettingsTitle(
            scroll = scroll,
            largeTopAppBarHeight = largeTopAppBarHeight,
            smallTopAppBarHeight = smallTopAppBarHeight,
            paddingMedium = paddingMedium,
            topInset = topInsets
        )
    }
}