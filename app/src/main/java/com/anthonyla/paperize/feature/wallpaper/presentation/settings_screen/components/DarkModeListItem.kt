package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Three-state selector for dark mode in settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeListItem(darkMode: Boolean?, onDarkModeClick: (Boolean?) -> Unit) {
    val options = listOf(
        Triple(stringResource(R.string.dark_mode), Icons.Outlined.DarkMode, Icons.Filled.DarkMode),
        Triple(stringResource(R.string.automatic_device_mode), Icons.Outlined.BrightnessAuto, Icons.Filled.BrightnessAuto),
        Triple(stringResource(R.string.light_mode), Icons.Outlined.LightMode, Icons.Filled.LightMode)
    )
    var selectedIndex by remember {
        mutableIntStateOf(
            when (darkMode) {
                true -> 0
                false -> 2
                else -> 1
            }
        ) }
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    selectedIndex = when (selectedIndex) {
                        0 -> 1
                        1 -> 2
                        else -> 0
                    }
                    onDarkModeClick(
                        when (selectedIndex) {
                            0 -> true
                            2 -> false
                            else -> null
                        }
                    )
                },
            headlineContent = {
                Text(
                    text = stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.titleMedium

                ) },
            supportingContent = {
                Text(
                    text = stringResource(R.string.easier_on_the_eyes),
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis
                ) },
            trailingContent = {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .weight(1f)
                        .width(125.dp)
                        .height(36.dp)
                ) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = {
                                selectedIndex = index
                                onDarkModeClick(
                                    when (index) {
                                        0 -> true
                                        2 -> false
                                        else -> null
                                    }
                                ) },
                            selected = index == selectedIndex
                        ) {
                            Icon(
                                imageVector = if (index == selectedIndex) label.third else label.second,
                                contentDescription = label.first,
                                modifier = if (index != selectedIndex) Modifier.requiredSize(24.dp) else Modifier.requiredSize(12.dp)
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Brightness6,
                    contentDescription = stringResource(R.string.dark_mode),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            tonalElevation = 5.dp
        )
    }
}