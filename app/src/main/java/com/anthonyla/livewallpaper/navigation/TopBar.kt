package com.anthonyla.livewallpaper.navigation


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.anthonyla.livewallpaper.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar (
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {},
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
        scrollBehavior = null,
        actions = {
            IconButton(
                onClick = { expanded = !expanded }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
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
                        onClick = { /*do something*/ }
                    )
                }
            }
        }
    )
}