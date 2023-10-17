package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar (
    showSelectionModeAppBar: Boolean,
    selectionCount: Int,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onContactClick: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        title = { if (!showSelectionModeAppBar) Text(stringResource(R.string.appname)) },
        actions = {
            if (!showSelectionModeAppBar) {
                var menuExpanded by rememberSaveable { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.MoreVertIcon)
                    )
                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = DpOffset(0.dp, 5.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dropdownmenu_settings)) },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.contact_button)) },
                                onClick = {
                                    menuExpanded = false
                                    onContactClick()
                                }
                            )
                        }
                    }
                }
            }
            else {
                var showAlertDialog by rememberSaveable { mutableStateOf(false) }
                if (showAlertDialog) DeleteImagesAlertDialog (
                    onDismissRequest = { showAlertDialog = false },
                    onConfirmation = {
                        showAlertDialog = false
                        //deleteImagesOnClick()
                    }
                )
                IconButton(
                    onClick = { showAlertDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.select_all_images_for_deletion),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        },
        navigationIcon = {
            if (showSelectionModeAppBar) {
                Row (
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { }
                    ) {
                        val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                        if (true) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.all_images_selected_for_deletion),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(2.dp, bgColor, CircleShape)
                                    .clip(CircleShape)
                                    .background(bgColor)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.select_all_images_for_deletion),
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    Text("$selectionCount selected")
                }
            }
        }
    )
}