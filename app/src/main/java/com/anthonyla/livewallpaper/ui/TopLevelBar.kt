package com.anthonyla.livewallpaper.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.windowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.anthonyla.livewallpaper.R
import com.anthonyla.livewallpaper.data.Contact
import com.anthonyla.livewallpaper.data.navigation.SettingsNavScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelBar (
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var toContact by rememberSaveable { mutableStateOf(false) }
    if (toContact) {
        Contact(LocalContext.current)
        toContact = false
        expanded = false
    }
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {},
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
        scrollBehavior = null,
        actions = {
            IconButton(
                onClick = {
                    expanded = !expanded }
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.MoreVertIcon)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dropdownmenu_settings)) },
                        onClick = {
                            expanded = false
                            navController.navigate(SettingsNavScreens.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Contact") },
                        onClick = { toContact = true }
                    )
                }
            }
        }
    )
}