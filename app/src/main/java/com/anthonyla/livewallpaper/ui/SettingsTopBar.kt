package com.anthonyla.livewallpaper.ui

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.windowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar (
    navController: NavController,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {},
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
        scrollBehavior = null,
        actions = {}
    )
}
