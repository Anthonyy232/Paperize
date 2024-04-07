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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import kotlinx.coroutines.flow.StateFlow

/**
 * Scrollable settings screen to wrap all settings components
 */
@Composable
fun ScrollableSettings(
    settingsState: StateFlow<SettingsState>,
    largeTopAppBarHeightPx: Dp,
    scroll: ScrollState,
    onDynamicThemingClick: (Boolean) -> Unit,
    onDarkModeClick: (Boolean?) -> Unit
) {
    val state = settingsState.collectAsStateWithLifecycle()
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

        Spacer(modifier = Modifier.height(16.dp))

        ListSectionTitle("Appearance")
        Spacer(modifier = Modifier.height(16.dp))
        DarkModeListItem(
            darkMode = state.value.darkMode,
            onDarkModeClick = { onDarkModeClick(it) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DynamicThemingListItem(
            dynamicTheming = state.value.dynamicTheming,
            onDynamicThemingClick = { onDynamicThemingClick(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ListSectionTitle("Privacy")
        //add crash reporting settings
        Spacer(modifier = Modifier.height(16.dp))
        DynamicThemingListItem(
            dynamicTheming = state.value.dynamicTheming,
            onDynamicThemingClick = { onDynamicThemingClick(it) }
        )

        //GPL and License and link to github etc.
    }
}