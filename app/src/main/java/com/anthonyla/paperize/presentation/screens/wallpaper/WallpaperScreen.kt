package com.anthonyla.paperize.presentation.screens.wallpaper

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anthonyla.paperize.R
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WallpaperViewModel = hiltViewModel()
) {
    val wallpaper by viewModel.wallpaper.collectAsState()
    val bitmap by viewModel.bitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showSetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wallpaper?.fileName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { showSetDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.set_as_lock_screen))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                bitmap != null -> {
                    val zoomState = rememberZoomState()
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = wallpaper?.fileName,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState),
                        contentScale = ContentScale.Fit
                    )
                }
                else -> Text(stringResource(R.string.cannot_change_wallpaper))
            }
        }

        if (showSetDialog) {
            AlertDialog(
                onDismissRequest = { showSetDialog = false },
                title = { Text(stringResource(R.string.change_wallpaper)) },
                text = { Text("Set as...") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setAsWallpaper(WallpaperManager.FLAG_SYSTEM) {
                                showSetDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.home))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.setAsWallpaper(WallpaperManager.FLAG_LOCK) {
                                showSetDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.lock))
                    }
                }
            )
        }
    }
}
