package com.anthonyla.paperize.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
fun TopBar (
    navController: NavController,
    title: String?,
    showBackButton: Boolean,
    showMenuButton: Boolean,
    modifier: Modifier = Modifier
) {

    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    var toContactPressed by rememberSaveable { mutableStateOf(false) }
    if (toContactPressed) {
        Contact(LocalContext.current)
        toContactPressed = false
        menuExpanded = false
    }

    TopAppBar(
        title = { if (title != null) { Text(title) } },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navController.navigateUp() }) {
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
            if (showMenuButton) {
                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.MoreVertIcon)
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = !menuExpanded },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dropdownmenu_settings)) },
                            onClick = {
                                menuExpanded = !menuExpanded
                                navController.navigate(SettingsNavScreens.Settings.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Contact") },
                            onClick = { toContactPressed = true }
                        )
                    }
                }
            }
        }
    )
}