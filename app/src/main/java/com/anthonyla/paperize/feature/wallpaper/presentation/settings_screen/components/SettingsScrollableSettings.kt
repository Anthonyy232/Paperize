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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.data.Contact
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.flow.StateFlow

/**
 * Scrollable settings screen to wrap all settings components
 */
@Composable
fun SettingsScrollableSettings(
    settingsState: StateFlow<SettingsState>,
    largeTopAppBarHeightPx: Dp,
    scroll: ScrollState,
    onDynamicThemingClick: (Boolean) -> Unit,
    onDarkModeClick: (Boolean?) -> Unit,
    onAnimateClick: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLicenseClick: () -> Unit
) {
    val state = settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var toContact by rememberSaveable { mutableStateOf(false) }
    if (toContact) {
        Contact(context)
        toContact = false
    }

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
        AnimationListItem(
            animate = state.value.animate,
            onAnimateClick = { onAnimateClick(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        ListSectionTitle("About")
        Spacer(modifier = Modifier.height(16.dp))
        PrivacyPolicyListItem (onPrivacyPolicyClick = onPrivacyClick)
        Spacer(modifier = Modifier.height(16.dp))
        LicenseListItem(onLicenseClick = onLicenseClick)
        Spacer(modifier = Modifier.height(16.dp))
        ContactListItem(onContactClick = { toContact = true })
        //GPL and License and link to github etc, contact, terms of service, privacy policy
    }
}