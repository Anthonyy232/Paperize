package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

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
                    Modifier
                        .weight(1f)
                        .padding(8.dp)
                } else {
                    Modifier
                        .fillMaxSize(0.5f)
                        .padding(8.dp)
                }

                if (lockEnabled) {
                    Column(
                        modifier = modifier,
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = stringResource(R.string.lock_screen), fontWeight = FontWeight.W500)
                        if (currentLockWallpaper != null) {
                            PreviewItem(
                                wallpaperUri = currentLockWallpaper,
                                darken = darken,
                                darkenPercentage = if (!homeEnabled) homeDarkenPercentage else lockDarkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercentage = if (!homeEnabled) homeBlurPercentage else lockBlurPercentage,
                                vignette = vignette,
                                vignettePercentage = if (!homeEnabled) homeVignettePercentage else lockVignettePercentage,
                                grayscale = grayscale,
                                grayscalePercentage = if (!homeEnabled) homeGrayscalePercentage else lockGrayscalePercentage
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
                                    3 -> ScalingConstants.NONE
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
                                    3 -> painterResource(id = R.drawable.none)
                                    else -> painterResource(id = R.drawable.fill)
                                },
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis )
                        }
                    }
                }
            }
        }
    }
}