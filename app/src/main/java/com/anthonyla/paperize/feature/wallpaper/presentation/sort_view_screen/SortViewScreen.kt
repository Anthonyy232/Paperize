package com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.components.SortViewTopBar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CardColors
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SortViewScreen(
    sortViewModel: SortViewModel,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val view = LocalView.current
    val state = sortViewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val expandedFolderId = remember { mutableStateOf<String?>(null) }
    val reorderableLazyListStateFolder = rememberReorderableLazyListState(lazyListState) { from, to ->
        sortViewModel.onEvent(SortEvent.ShiftFolder(from, to))
    }
    val reorderableLazyListStateWallpaper = rememberReorderableLazyListState(lazyListState) { from, to ->
        sortViewModel.onEvent(SortEvent.ShiftWallpaper(from, to))
    }
    val folderWallpapersListState = rememberLazyListState()
    val reorderableFolderWallpapersState = rememberReorderableLazyListState(folderWallpapersListState) { from, to ->
        expandedFolderId.value?.let { folderId ->
            sortViewModel.onEvent(SortEvent.ShiftFolderWallpaper(folderId, from, to))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SortViewTopBar(
                title = "",
                onBackClick = onBackClick,
                onSaveClick = onSaveClick,
                onSortAlphabetically = { sortViewModel.onEvent(SortEvent.SortAlphabetically) },
                onSortAlphabeticallyReverse = { sortViewModel.onEvent(SortEvent.SortAlphabeticallyReverse) },
                onSortByLastModified = { sortViewModel.onEvent(SortEvent.SortByLastModified) },
                onSortByLastModifiedReverse = { sortViewModel.onEvent(SortEvent.SortByLastModifiedReverse) },
            )
        }
    ) {
        SharedTransitionLayout (modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = !expandedFolderId.value.isNullOrEmpty(), label = "",
            ) {  targetState ->
                with (this@SharedTransitionLayout) {
                    if (!targetState) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            state = lazyListState,
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.value.folders.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.folders),
                                        modifier = Modifier.padding(8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(state.value.folders, key = { it.folderUri }) { folder ->
                                ReorderableItem(
                                    state = reorderableLazyListStateFolder,
                                    key = folder.folderUri
                                ) { isDragging ->
                                    val interactionSource =
                                        remember { MutableInteractionSource() }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.DRAG_START
                                                        )
                                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.GESTURE_START
                                                        )
                                                    } else {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.LONG_PRESS
                                                        )
                                                    }
                                                },
                                                onDragStopped = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.GESTURE_END
                                                        )
                                                    } else {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.LONG_PRESS
                                                        )
                                                    }
                                                },
                                                interactionSource = interactionSource,
                                            ),
                                        onClick = { expandedFolderId.value = folder.folderUri }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = folder.folderName ?: "",
                                                modifier = Modifier.sharedElement(
                                                    rememberSharedContentState(
                                                        folder.folderName ?: ""
                                                    ),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                )
                                            )
                                            IconButton(onClick = {
                                                expandedFolderId.value = folder.folderUri
                                            }) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = stringResource(R.string.expand)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.value.wallpapers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.wallpapers_sort),
                                        modifier = Modifier.padding(8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(state.value.wallpapers, key = { it.wallpaperUri }) { wallpaper ->
                                ReorderableItem(
                                    reorderableLazyListStateWallpaper,
                                    key = wallpaper.wallpaperUri
                                ) { isDragging ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.DRAG_START
                                                        )
                                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.GESTURE_START
                                                        )
                                                    } else {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.LONG_PRESS
                                                        )
                                                    }
                                                },
                                                onDragStopped = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.GESTURE_END
                                                        )
                                                    } else {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.LONG_PRESS
                                                        )
                                                    }
                                                },
                                                interactionSource = interactionSource,
                                            ),
                                        onClick = {}
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = wallpaper.fileName,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            GlideImage(
                                                imageModel = { wallpaper.wallpaperUri.toUri() },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it),
                            state = folderWallpapersListState,
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val currentFolder = state.value.folders.find { it.folderUri == expandedFolderId.value }
                            if (currentFolder != null) {
                                item {
                                    ListItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingContent = {
                                            GlideImage(
                                                imageModel = { currentFolder.coverUri?.toUri() },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                            )
                                        },
                                        headlineContent = {
                                            Text(text = currentFolder.folderName ?: "")
                                        },
                                        supportingContent = {
                                            Text(text = currentFolder.wallpapers.size.toString().plus(" " + stringResource(R.string.wallpapers)))
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { expandedFolderId.value = null }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.close)
                                                )
                                            }
                                        }
                                    )
                                }
                                items(currentFolder.wallpapers, key = { it.wallpaperUri }) { wallpaper ->
                                    ReorderableItem(
                                        reorderableFolderWallpapersState,
                                        key = wallpaper.wallpaperUri
                                    ) { isDragging ->
                                        val interactionSource = remember { MutableInteractionSource() }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .longPressDraggableHandle(
                                                    onDragStarted = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.DRAG_START
                                                            )
                                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.GESTURE_START
                                                            )
                                                        } else {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.LONG_PRESS
                                                            )
                                                        }
                                                    },
                                                    onDragStopped = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.GESTURE_END
                                                            )
                                                        } else {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.LONG_PRESS
                                                            )
                                                        }
                                                    },
                                                    interactionSource = interactionSource,
                                                ),
                                            onClick = {}
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = wallpaper.fileName,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                GlideImage(
                                                    imageModel = { wallpaper.wallpaperUri.toUri() },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}