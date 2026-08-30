package com.anthonyla.paperize.presentation.screens.wallpaper_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.presentation.theme.AppSpacing
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperViewScreen(
    wallpaperUri: String,
    wallpaperName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WallpaperViewViewModel = hiltViewModel()
) {
    val zoomState = rememberZoomState()
    val wallpaperMode by viewModel.wallpaperMode.collectAsStateWithLifecycle()
    var showApplyDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    if (zoomState.scale <= 1.01f) {
                        Text(
                            text = wallpaperName,
                            color = MaterialTheme.colorScheme.surfaceBright,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(AppSpacing.large)
                            .requiredSize(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.surfaceBright
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                actions = {
                    if (wallpaperMode == WallpaperMode.STATIC) {
                        Button(
                            onClick = { showApplyDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.large)
                        ) {
                            Text(stringResource(R.string.set_wallpaper))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.individual_wallpaper_static_only),
                            color = MaterialTheme.colorScheme.surfaceBright,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.large)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(padding)
                    .zoomable(zoomState)
            ) {
                AsyncImage(
                    model = wallpaperUri.toUri(),
                    contentDescription = wallpaperName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(stringResource(R.string.set_wallpaper)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ScreenType.HOME to R.string.home_screen_btn,
                        ScreenType.LOCK to R.string.lock_screen_btn,
                        ScreenType.BOTH to R.string.home_and_lock_screens
                    ).forEach { (screenType, label) ->
                        TextButton(
                            onClick = {
                                showApplyDialog = false
                                viewModel.applyTo(screenType)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(label))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
