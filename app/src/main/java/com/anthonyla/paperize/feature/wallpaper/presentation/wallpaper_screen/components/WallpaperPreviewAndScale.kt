package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * SetLockScreenSwitch is a composable that displays a switch to set the wallpaper as the lock screen wallpaper
 * Also shows a preview of lock and home screen when enabled along with brightness added
 */
@Composable
fun WallpaperPreviewAndScale(
    currentHomeWallpaper: String?,
    currentLockWallpaper: String?,
    darken: Boolean,
    homeDarkenPercentage: Int,
    lockDarkenPercentage: Int,
    homeEnabled: Boolean,
    lockEnabled: Boolean,
    scaling: ScalingConstants,
    onScalingChange: (ScalingConstants) -> Unit,
    blur: Boolean,
    homeBlurPercentage: Int,
    lockBlurPercentage: Int,
    vignette: Boolean,
    homeVignettePercentage: Int,
    lockVignettePercentage: Int,
    grayscale: Boolean,
    homeGrayscalePercentage: Int,
    lockGrayscalePercentage: Int
) {
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (scaling) {
                ScalingConstants.FILL -> 0
                ScalingConstants.FIT -> 1
                ScalingConstants.STRETCH -> 2
                ScalingConstants.NONE -> 3
            }
        )
    }
    val options = listOf(
        stringResource(R.string.fill),
        stringResource(R.string.fit),
        stringResource(R.string.stretch),
        stringResource(R.string.none)
    )

    Surface(
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            if (lockEnabled && homeEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.lock_screen),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W500
                        )
                        if (currentLockWallpaper != null) {
                            PreviewItem(
                                wallpaperUri = currentLockWallpaper,
                                darken = darken,
                                darkenPercentage = lockDarkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = lockBlurPercentage,
                                vignette = vignette,
                                vignettePercentage = lockVignettePercentage,
                                grayscale = grayscale,
                                grayscalePercentage = lockGrayscalePercentage
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W500
                        )
                        if (currentHomeWallpaper != null) {
                            PreviewItem(
                                wallpaperUri = currentHomeWallpaper,
                                darken = darken,
                                darkenPercentage = homeDarkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = homeBlurPercentage,
                                vignette = vignette,
                                vignettePercentage = homeVignettePercentage,
                                grayscale = grayscale,
                                grayscalePercentage = homeGrayscalePercentage
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lockEnabled) {
                        Text(
                            text = stringResource(R.string.lock_screen),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W500
                        )
                        if (currentLockWallpaper != null) {
                            PreviewItem(
                                wallpaperUri = currentLockWallpaper,
                                darken = darken,
                                darkenPercentage = homeDarkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = homeBlurPercentage,
                                vignette = vignette,
                                vignettePercentage = homeVignettePercentage,
                                grayscale = grayscale,
                                grayscalePercentage = homeGrayscalePercentage
                            )
                        }
                    }
                    if (homeEnabled) {
                        Text(
                            text = stringResource(R.string.home),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W500
                        )
                        if (currentHomeWallpaper != null) {
                            PreviewItem(
                                wallpaperUri = currentHomeWallpaper,
                                darken = darken,
                                darkenPercentage = homeDarkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = homeBlurPercentage,
                                vignette = vignette,
                                vignettePercentage = homeVignettePercentage,
                                grayscale = grayscale,
                                grayscalePercentage = homeGrayscalePercentage
                            )
                        }
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
                                    3 -> ScalingConstants.NONE
                                    else -> ScalingConstants.FILL
                                }
                            )
                        },
                        selected = index == selectedIndex,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = when (index) {
                                    0 -> painterResource(id = R.drawable.fill)
                                    1 -> painterResource(id = R.drawable.fit)
                                    2 -> painterResource(id = R.drawable.stretch)
                                    3 -> painterResource(id = R.drawable.none)
                                    else -> painterResource(id = R.drawable.fill)
                                },
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}