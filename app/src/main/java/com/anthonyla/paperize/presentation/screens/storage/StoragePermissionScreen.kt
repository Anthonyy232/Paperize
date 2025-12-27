package com.anthonyla.paperize.presentation.screens.storage
import com.anthonyla.paperize.presentation.theme.AppIconSizes
import com.anthonyla.paperize.presentation.components.OnboardingLayout
import com.anthonyla.paperize.core.util.PermissionUtil

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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
    var permissionGranted by remember { mutableStateOf(PermissionUtil.hasStoragePermission()) }
    // Check permission status on resume (when user returns from settings)
    DisposableEffect(Unit) {
        val listener = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                permissionGranted = PermissionUtil.hasStoragePermission()
            }
        }
        val lifecycleOwner = context as? LifecycleOwner
        lifecycleOwner?.lifecycle?.addObserver(listener)

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(listener)
        }
    }

    // Automatically continue when permission is granted
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            onContinue()
        }
    }

    OnboardingLayout(
        title = stringResource(R.string.storage_permission_title),
        modifier = modifier,
        iconContent = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(AppIconSizes.huge),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Cleaner explanation card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.storage_permission_description),
                            style = MaterialTheme.typography.bodyLarge,
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
            }
        },
        actions = {
            // Allow button
            Button(
                onClick = {
                    if (!PermissionUtil.hasStoragePermission()) {
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

            // Continue without permission button
            FilledTonalButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.continue_without_storage),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    modifier = Modifier.padding(vertical = AppSpacing.small)
                )
            }
        }
    )
}
