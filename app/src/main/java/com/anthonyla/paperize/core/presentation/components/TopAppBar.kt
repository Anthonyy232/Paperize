package com.anthonyla.paperize.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.anthonyla.paperize.R
import com.anthonyla.paperize.data.Contact
import com.anthonyla.paperize.feature.wallpaper.util.navigation.SettingsNavScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar (
    navController: NavController,
    isTopLevel: Boolean,
    modifier: Modifier = Modifier
) {
    // Is drop-down-menu expanded/pressed
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Open email to contact if contact button is pressed
    var toContact by rememberSaveable { mutableStateOf(false) }
    if (toContact) {
        Contact(LocalContext.current)
        toContact = false
        expanded = false
    }

    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {
            if(!isTopLevel){
                IconButton(
                    onClick = { navController.navigateUp() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back)
                    )
                }
            }
        },
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
        scrollBehavior = null,
        actions = {
            if (isTopLevel) {
                IconButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.MoreVertIcon)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = !expanded },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dropdownmenu_settings)) },
                            onClick = {
                                expanded = !expanded
                                navController.navigate(SettingsNavScreens.Settings.route) {
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
        }
    )
}