package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.album.components.WallpaperItem

@Composable
fun SetLockScreenSwitch(
    albumUri : String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        tonalElevation = 5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column (horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row {
                Text(
                    text = "Set as lock screen",
                    modifier = Modifier.padding(16.dp)
                )
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            AnimatedVisibility(
                visible = checked,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing))
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Lock screen")
                        if (albumUri != null) {
                            WallpaperItem(
                                wallpaperUri = albumUri,
                                itemSelected = false,
                                selectionMode = false,
                                onActivateSelectionMode = {},
                                onItemSelection = {},
                                onWallpaperViewClick = {},
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Home screen")
                        if (albumUri != null) {
                            WallpaperItem(
                                wallpaperUri = albumUri,
                                itemSelected = false,
                                selectionMode = false,
                                onActivateSelectionMode = {},
                                onItemSelection = {},
                                onWallpaperViewClick = {},
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}