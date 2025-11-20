package com.anthonyla.paperize.presentation.screens.storage

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.theme.AppSpacing

/**
 * Screen to request storage permission for wallpaper preview
 * Allows optional MANAGE_EXTERNAL_STORAGE permission to read current wallpaper
 */
@Composable
fun StoragePermissionScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    var hasShownScreen by remember { mutableStateOf(false) }

    // Check permission status on resume (when user returns from settings)
    DisposableEffect(Unit) {
        val listener = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                permissionGranted = Environment.isExternalStorageManager()
            }
        }
        val lifecycleOwner = context as? LifecycleOwner
        lifecycleOwner?.lifecycle?.addObserver(listener)

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(listener)
        }
    }

    // Mark screen as shown after first composition
    LaunchedEffect(Unit) {
        hasShownScreen = true
    }

    // Auto-skip if permission granted after screen has been shown
    // This prevents immediate navigation on first render if permission is already granted
    LaunchedEffect(permissionGranted, hasShownScreen) {
        if (permissionGranted && hasShownScreen) {
            kotlinx.coroutines.delay(Constants.PERMISSION_SCREEN_TRANSITION_DELAY_MS)  // Brief delay for smooth transition
            onContinue()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.extraLarge)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Enhanced icon with larger size
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(AppSpacing.large))

        Text(
            text = stringResource(R.string.storage_permission_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        // Enhanced card with better styling
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.large),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.storage_permission_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Text(
                    text = stringResource(R.string.storage_permission_optional),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

        // Enhanced allow button
        Button(
            onClick = {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } else {
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = if (permissionGranted) {
                    stringResource(R.string.permission_granted)
                } else {
                    stringResource(R.string.allow)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = AppSpacing.small)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        // Enhanced continue without permission button
        FilledTonalButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.continue_without_storage),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.padding(vertical = AppSpacing.small)
            )
        }
    }
}
