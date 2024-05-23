package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem

/**
 * SetLockScreenSwitch is a composable that displays a switch to set the wallpaper as the lock screen wallpaper
 * Also shows a preview of lock and home screen when enabled along with brightness added
 */
@Composable
fun WallpaperPreviewAndScale(
    currentHomeWallpaper: String?,
    currentLockWallpaper: String?,
    animate: Boolean,
    darken: Boolean,
    darkenPercentage: Int,
    homeEnabled: Boolean,
    lockEnabled: Boolean,
    scaling: ScalingConstants,
    onScalingChange: (ScalingConstants) -> Unit,
    blur: Boolean,
    blurPercentage: Int,
) {
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (scaling) {
                ScalingConstants.FILL -> 0
                ScalingConstants.FIT -> 1
                ScalingConstants.STRETCH -> 2
            }
        )
    }
    val options = listOf(
        stringResource(R.string.fill),
        stringResource(R.string.fit),
        stringResource(R.string.stretch)
    )

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modifier = if (lockEnabled && homeEnabled) {
                    Modifier.weight(1f).padding(8.dp)
                } else {
                    Modifier.fillMaxSize(0.5f).padding(8.dp)
                }

                if (lockEnabled) {
                    Column(
                        modifier = modifier,
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = stringResource(R.string.lock_screen), fontWeight = FontWeight.W500)
                        if (currentLockWallpaper != null) {
                            WallpaperItem(
                                wallpaperUri = currentLockWallpaper,
                                itemSelected = false,
                                selectionMode = false,
                                onActivateSelectionMode = {},
                                onItemSelection = {},
                                onWallpaperViewClick = {},
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(
                                        3.dp,
                                        color = Color.Black,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                aspectRatio = 9f / 19.5f,
                                clickable = false,
                                animate = animate,
                                darken = darken,
                                darkenPercentage = darkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = blurPercentage
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                if (homeEnabled) {
                    Column(
                        modifier = modifier,
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = stringResource(R.string.home), fontWeight = FontWeight.W500)
                        if (currentHomeWallpaper != null) {
                            WallpaperItem(
                                wallpaperUri = currentHomeWallpaper,
                                itemSelected = false,
                                selectionMode = false,
                                onActivateSelectionMode = {},
                                onItemSelection = {},
                                onWallpaperViewClick = {},
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(
                                        3.dp,
                                        color = Color.Black,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                aspectRatio = 9f / 19.5f,
                                clickable = false,
                                animate = animate,
                                darken = darken,
                                darkenPercentage = darkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = blurPercentage
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            SingleChoiceSegmentedButtonRow (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp))
            ) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = {
                            selectedIndex = index
                            onScalingChange(
                                when (index) {
                                    0 -> ScalingConstants.FILL
                                    1 -> ScalingConstants.FIT
                                    2 -> ScalingConstants.STRETCH
                                    else -> ScalingConstants.FILL
                                }
                            ) },
                        selected = index == selectedIndex
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = when (index) {
                                    0 -> painterResource(id = R.drawable.fill)
                                    1 -> painterResource(id = R.drawable.fit)
                                    2 -> painterResource(id = R.drawable.stretch)
                                    else -> painterResource(id = R.drawable.fill)
                                },
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis )
                        }
                    }
                }
            }
        }
    }
}