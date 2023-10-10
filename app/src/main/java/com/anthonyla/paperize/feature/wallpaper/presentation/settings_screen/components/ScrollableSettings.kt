package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState

@Composable
fun ScrollableSettings(
    settingsState: SettingsState,
    largeTopAppBarHeightPx: Dp,
    scroll: ScrollState,
    onDynamicThemingClick: (Boolean) -> Unit,
    onDarkModeClick: (Boolean?) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(largeTopAppBarHeightPx))
        Spacer(modifier = Modifier.height(16.dp))
        ListSectionTitle("Appearance")

        Spacer(modifier = Modifier.height(16.dp))
        DarkModeListItem(
            settingsState = settingsState,
            onDarkModeClick = onDarkModeClick
        )

        Spacer(modifier = Modifier.height(16.dp))
        DynamicThemingListItem(
            settingsState = settingsState,
            onDynamicThemingClick = onDynamicThemingClick
        )
    }
}