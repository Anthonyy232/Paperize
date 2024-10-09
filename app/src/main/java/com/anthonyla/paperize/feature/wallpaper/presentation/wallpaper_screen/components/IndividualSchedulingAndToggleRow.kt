package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Composable to toggle individual scheduling settings and toggle wallpaper changer when scheduling separately
 */
@Composable
fun IndividualSchedulingAndToggleRow(
    animate: Boolean,
    scheduleSeparately: Boolean,
    enableChanger : Boolean,
    onToggleChanger: (Boolean) -> Unit,
    onScheduleSeparatelyChange: (Boolean) -> Unit,
) {
    val columnModifier = remember {
        if (animate) {
            Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else { Modifier }
    }

    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
    ) {
        Column(
            modifier = columnModifier
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.individual_scheduling),
                    fontWeight = FontWeight.W500,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Switch(
                    checked = scheduleSeparately,
                    onCheckedChange = onScheduleSeparatelyChange,
                )
            }
            if (animate) {
                AnimatedVisibility(
                    visible = scheduleSeparately,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 2.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.enable_wallpaper_changer),
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.W500,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Switch(
                            checked = enableChanger,
                            onCheckedChange = onToggleChanger
                        )
                    }
                }
            }
            else {
                if (scheduleSeparately) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.enable_wallpaper_changer),
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.W500
                        )
                        Switch(
                            checked = enableChanger,
                            onCheckedChange = onToggleChanger
                        )
                    }
                }
            }
        }
    }
}