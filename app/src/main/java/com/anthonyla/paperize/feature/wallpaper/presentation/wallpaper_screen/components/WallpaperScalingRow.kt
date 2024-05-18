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
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants

/**
 *  Row for selecting the scaling of the wallpaper
 */
@Composable
fun WallpaperScalingRow(
    scaling: ScalingConstants,
    onScalingChange: (ScalingConstants) -> Unit
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
        Column {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(top = 16.dp, bottom = 4.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.wallpaper_scaling),
                    fontWeight = FontWeight.W500
                )
            }
            SingleChoiceSegmentedButtonRow (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp))
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
                                modifier = Modifier.size(36.dp)
                            )
                            Text(text = label)
                        }
                    }
                }
            }
        }
    }
}