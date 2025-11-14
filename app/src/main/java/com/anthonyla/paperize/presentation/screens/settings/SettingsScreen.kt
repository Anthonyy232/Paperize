package com.anthonyla.paperize.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anthonyla.paperize.R

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val appSettings by viewModel.appSettings.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance section
            ListItem(
                headlineContent = { Text(stringResource(R.string.appearance)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SwitchListItem(
                title = stringResource(R.string.dark_mode),
                description = stringResource(R.string.easier_on_the_eyes),
                checked = appSettings.darkMode ?: false,
                onCheckedChange = { viewModel.updateDarkMode(it) }
            )

            if (appSettings.darkMode == true) {
                SwitchListItem(
                    title = stringResource(R.string.amoled_mode),
                    description = stringResource(R.string.full_dark_mode),
                    checked = appSettings.amoledTheme,
                    onCheckedChange = { viewModel.updateAmoledTheme(it) }
                )
            }

            SwitchListItem(
                title = stringResource(R.string.dynamic_theming),
                description = stringResource(R.string.material_you),
                checked = appSettings.dynamicTheming,
                onCheckedChange = { viewModel.updateDynamicTheming(it) }
            )

            SwitchListItem(
                title = stringResource(R.string.animation),
                description = stringResource(R.string.increase_visual_appeal),
                checked = appSettings.animate,
                onCheckedChange = { viewModel.updateAnimate(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About section
            ListItem(
                headlineContent = { Text(stringResource(R.string.about)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                onClick = onNavigateToPrivacy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.privacy_policy)) },
                    supportingContent = { Text(stringResource(R.string.click_here_to_view_our_privacy_policy)) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Reset data
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.reset_all_data))
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_to_default)) },
                text = { Text(stringResource(R.string.are_you_sure_you_want_to_reset_all_settings_and_data_to_default)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SwitchListItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = modifier.padding(horizontal = 16.dp)
    )
}
