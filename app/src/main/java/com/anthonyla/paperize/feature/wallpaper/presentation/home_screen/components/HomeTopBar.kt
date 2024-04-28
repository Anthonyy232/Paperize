package com.anthonyla.paperize.feature.wallpaper.presentation.home_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components.DeleteImagesAlertDialog

/**
 * Top bar for the home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar (
    showSelectionModeAppBar: Boolean,
    selectionCount: Int,
    onSettingsClick: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        title = { if (!showSelectionModeAppBar) Text(stringResource(R.string.appname)) },
        actions = {
            if (showSelectionModeAppBar) {
                var showAlertDialog by rememberSaveable { mutableStateOf(false) }
                if (showAlertDialog) DeleteImagesAlertDialog (
                    onDismissRequest = { showAlertDialog = false },
                    onConfirmation = {
                        showAlertDialog = false
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
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.all_images_selected_for_deletion),
                            modifier = Modifier
                                .padding(4.dp)
                                .border(2.dp, bgColor, CircleShape)
                                .clip(CircleShape)
                                .background(bgColor)
                        )
                    }
                    Text("$selectionCount selected")
                }
            } else {
                IconButton(
                    onClick = { onSettingsClick() },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.SettingsIcon),
                    )
                }
            }
        }
    )
}