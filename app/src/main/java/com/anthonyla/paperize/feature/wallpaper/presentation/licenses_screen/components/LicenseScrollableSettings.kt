package com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.LibraryDefaults

/**
 * Scrollable license screen to display open source licenses
 */
@Composable
fun LicenseScrollableSettings(
    largeTopAppBarHeightPx: Dp,
    lazyListState: LazyListState,
    offset: State<Int>
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        val offsetInDp = offset.value.toFloat() / 2f.dp.value
        val newHeight = largeTopAppBarHeightPx - offsetInDp.dp
        Spacer(Modifier.height(newHeight))

        Spacer(modifier = Modifier.height(16.dp))
        LibrariesContainer(
            modifier = Modifier.fillMaxSize(),
            lazyListState = lazyListState,
            showVersion = false,
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
                badgeBackgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
                badgeContentColor = MaterialTheme.colorScheme.primary,
                dialogConfirmButtonColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}