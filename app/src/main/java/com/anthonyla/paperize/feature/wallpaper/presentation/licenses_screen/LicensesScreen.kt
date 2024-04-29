package com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.data.rememberCurrentOffset
import com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.components.LicenseScrollableSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.components.LicenseSmallTopAppBar
import com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.components.LicenseTitle

@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val offset = rememberCurrentOffset(lazyListState)
    val largeTopAppBarHeight = 152.dp
    val smallTopAppBarHeight = 64.dp
    val paddingMedium = 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LicenseScrollableSettings(
            largeTopAppBarHeightPx = largeTopAppBarHeight,
            lazyListState = lazyListState,
            offset = offset
        )
        LicenseSmallTopAppBar(onBackClick = onBackClick)
        LicenseTitle(
            largeTopAppBarHeight = largeTopAppBarHeight,
            smallTopAppBarHeight = smallTopAppBarHeight,
            paddingMedium = paddingMedium,
            offset = offset
        )
    }
}