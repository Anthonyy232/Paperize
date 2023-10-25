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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.feature.wallpaper.presentation.settings.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeListItem(darkMode: Boolean?, onDarkModeClick: (Boolean?) -> Unit) {
    val options = listOf(
        Triple("Dark Mode", Icons.Outlined.DarkMode, Icons.Filled.DarkMode),
        Triple("Automatic Device Mode", Icons.Outlined.BrightnessAuto, Icons.Filled.BrightnessAuto),
        Triple("Light Mode", Icons.Outlined.LightMode, Icons.Filled.LightMode)
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
                    ) },
            headlineContent = {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.titleMedium

                ) },
            supportingContent = {
                Text(
                    text = "Easier on the eyes",
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis
                ) },
            trailingContent = {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .weight(1f)
                        .width(100.dp)
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
                            selected = index == selectedIndex,
                            icon = { null },
                        ) {
                            Icon(
                                contentDescription = label.first,
                                imageVector =
                                if(selectedIndex != index)
                                    label.second
                                else
                                    label.third,
                                modifier = Modifier.requiredSize(18.dp)
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Brightness6,
                    contentDescription = "Dark Mode",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            tonalElevation = 5.dp
        )
    }
}