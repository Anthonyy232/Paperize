package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.tabItems

@Composable
fun HomeTabRow(
    scroll: ScrollState,
    smallTopAppBarHeight: Dp,
    tabIndex: Int,
    modifier: Modifier = Modifier,
    onIndexUpdate: (Int) -> Unit
) {
    Surface (
        modifier = modifier
            .graphicsLayer {
                translationY = -scroll.value.toFloat() / 2f
                alpha = 0f
            }
    ) {
        SecondaryTabRow(
            selectedTabIndex = tabIndex,
            indicator = { tabPositions ->
                if (tabIndex < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        shape = RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 6.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp,
                        ),
                        width = 60.dp
                    )
                }
            }
        ) {
            tabItems.forEachIndexed { index, item ->
                Tab(
                    selected = (index == tabIndex),
                    onClick = { onIndexUpdate(index) },
                    text = { Text(text = item.title) },
                    icon = {
                        Icon(
                            imageVector =
                            if (index == tabIndex)
                                item.filledIcon
                            else
                                item.unfilledIcon,
                            contentDescription = item.title
                        )
                    }
                )
            }
        }
    }
}