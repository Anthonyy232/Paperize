package com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.components.PrivacyScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.components.PrivacyTitle

@Composable
fun PrivacyScreen(
    topInsets: Dp,
    onBackClick: () -> Unit,
) {
    val scroll: ScrollState = rememberScrollState(0)
    val largeTopAppBarHeight = 152.dp
    val smallTopAppBarHeight = 64.dp
    val paddingMedium = 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        PrivacyScrollableSettings(
            largeTopAppBarHeightPx = largeTopAppBarHeight,
            scroll = scroll,
            onBackClick = onBackClick
        )
        PrivacyTitle(
            scroll = scroll,
            largeTopAppBarHeight = largeTopAppBarHeight,
            smallTopAppBarHeight = smallTopAppBarHeight,
            paddingMedium = paddingMedium,
            topInset = topInsets
        )
    }
}