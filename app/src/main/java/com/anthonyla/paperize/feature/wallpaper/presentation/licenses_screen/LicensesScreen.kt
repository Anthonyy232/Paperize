package com.anthonyla.paperize.feature.wallpaper.presentation.licenses_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.anthonyla.paperize.R
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.LibraryDefaults
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val topBarState = rememberCollapsingToolbarScaffoldState()

    // Collapsing top bar
    val largeTopAppBarHeight = TopAppBarDefaults.LargeAppBarExpandedHeight
    val startPadding = 64.dp
    val endPadding = 16.dp
    val titleFontScaleStart = 30
    val titleFontScaleEnd = 21
    val titleExtraStartPadding = 32.dp
    val collapseFraction = topBarState.toolbarState.progress
    val firstPaddingInterpolation = lerp((endPadding * 5 / 4), endPadding, collapseFraction) + titleExtraStartPadding
    val secondPaddingInterpolation = lerp(startPadding, (endPadding * 5 / 4), collapseFraction)
    val dynamicPaddingStart = lerp(firstPaddingInterpolation, secondPaddingInterpolation, collapseFraction)
    val textSize = (titleFontScaleEnd + (titleFontScaleStart - titleFontScaleEnd) * collapseFraction).sp

    Scaffold {
        CollapsingToolbarScaffold(
            state = topBarState,
            modifier = Modifier.fillMaxSize().padding(it),
            scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
            toolbar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(largeTopAppBarHeight)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
                        .pin()
                )
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.home_screen)
                    )
                }
                Text(
                    text = stringResource(R.string.licenses),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = textSize,
                    modifier = Modifier
                        .road(Alignment.CenterStart, Alignment.BottomStart)
                        .padding(dynamicPaddingStart, 16.dp, 16.dp, 16.dp),
                )
            }
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            ) {
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
    }
}