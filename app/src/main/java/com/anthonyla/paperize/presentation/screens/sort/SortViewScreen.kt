package com.anthonyla.paperize.presentation.screens.sort

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.screens.sort.components.SortViewTopBar
import com.anthonyla.paperize.presentation.theme.AppShapes
import com.anthonyla.paperize.presentation.theme.AppSpacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SortViewScreen(
    albumId: String,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    sortViewModel: SortViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val state by sortViewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    var expandedFolderId by remember { mutableStateOf<String?>(null) }

    val onTopBarBackClick: () -> Unit = {
        if (expandedFolderId != null) {
            expandedFolderId = null
        } else {
            onBackClick()
        }
    }

    val onTopBarSaveClick: () -> Unit = {
        sortViewModel.saveChanges()
        onSaveClick()
    }

    // Logic to determine the title for the top bar
    val topBarTitle = remember(expandedFolderId, state.folders) {
        val currentFolderId = expandedFolderId
        if (!currentFolderId.isNullOrEmpty()) {
            state.folders.find { it.id == currentFolderId }?.name ?: ""
        } else {
            ""
        }
    }

    val reorderableLazyListStateFolder = rememberReorderableLazyListState(lazyListState) { from, to ->
        sortViewModel.onEvent(SortEvent.ShiftFolder(from, to))
    }
    val reorderableLazyListStateWallpaper = rememberReorderableLazyListState(lazyListState) { from, to ->
        sortViewModel.onEvent(SortEvent.ShiftWallpaper(from, to))
    }
    val folderWallpapersListState = rememberLazyListState()
    val reorderableFolderWallpapersState = rememberReorderableLazyListState(folderWallpapersListState) { from, to ->
        expandedFolderId?.let { folderId ->
            sortViewModel.onEvent(SortEvent.ShiftFolderWallpaper(folderId, from, to))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SortViewTopBar(
                title = topBarTitle,
                onBackClick = onTopBarBackClick,
                onSaveClick = onTopBarSaveClick,
                onSortAlphabetically = { sortViewModel.onEvent(SortEvent.SortAlphabetically) },
                onSortAlphabeticallyReverse = { sortViewModel.onEvent(SortEvent.SortAlphabeticallyReverse) },
                onSortByLastModified = { sortViewModel.onEvent(SortEvent.SortByLastModified) },
                onSortByLastModifiedReverse = { sortViewModel.onEvent(SortEvent.SortByLastModifiedReverse) },
            )
        }
    ) { paddingValues ->
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = !expandedFolderId.isNullOrEmpty(),
                label = "",
            ) { targetState ->
                with(this@SharedTransitionLayout) {
                    if (!targetState) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            state = lazyListState,
                            contentPadding = PaddingValues(AppSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        ) {
                            if (state.folders.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.folders),
                                        modifier = Modifier.padding(AppSpacing.small),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(state.folders, key = { it.uri }) { folder ->
                                ReorderableItem(
                                    state = reorderableLazyListStateFolder,
                                    key = folder.uri
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
                                                    } else {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.GESTURE_START
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
                                        colors = CardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                        onClick = { expandedFolderId = folder.id }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(AppSpacing.large),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = folder.displayName,
                                                modifier = Modifier.sharedElement(
                                                    rememberSharedContentState(folder.name),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(onClick = {
                                                expandedFolderId = folder.id
                                            }) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = stringResource(R.string.content_desc_expand)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.wallpapers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.wallpapers_sort),
                                        modifier = Modifier.padding(AppSpacing.small),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(state.wallpapers, key = { it.uri }) { wallpaper ->
                                ReorderableItem(
                                    reorderableLazyListStateWallpaper,
                                    key = wallpaper.uri
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
                                                    } else {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.GESTURE_START
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
                                        onClick = {},
                                        colors = CardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(AppSpacing.large)
                                        ) {
                                            Text(
                                                text = wallpaper.displayFileName,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(AppSpacing.small))
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(wallpaper.uri.toUri())
                                                    .size(Size(100, 100))
                                                    .build(),
                                                contentDescription = wallpaper.displayFileName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .border(
                                                        BorderStroke(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                        ), CircleShape
                                                    )
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            state = folderWallpapersListState,
                            contentPadding = PaddingValues(AppSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        ) {
                            val currentFolder = state.folders.find { it.id == expandedFolderId }
                            if (currentFolder != null) {
                                item {
                                    ListItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingContent = {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(currentFolder.coverUri?.toUri())
                                                    .size(Size(100, 100))
                                                    .build(),
                                                contentDescription = currentFolder.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(AppShapes.imageShape)
                                            )
                                        },
                                        headlineContent = {
                                            Text(
                                                text = currentFolder.name,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        supportingContent = {
                                            Text(text = pluralStringResource(R.plurals.wallpaper_count, currentFolder.wallpapers.size, currentFolder.wallpapers.size))
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { expandedFolderId = null }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.close)
                                                )
                                            }
                                        }
                                    )
                                }
                                items(currentFolder.wallpapers, key = { it.uri }) { wallpaper ->
                                    ReorderableItem(
                                        reorderableFolderWallpapersState,
                                        key = wallpaper.uri
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
                                                        } else {
                                                            view.performHapticFeedback(
                                                                HapticFeedbackConstants.GESTURE_START
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
                                            onClick = {},
                                            colors = CardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            ),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.padding(AppSpacing.large)
                                            ) {
                                                Text(
                                                    text = wallpaper.displayFileName,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(AppSpacing.small))
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(wallpaper.uri.toUri())
                                                        .size(Size(100, 100))
                                                        .build(),
                                                    contentDescription = wallpaper.displayFileName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .border(
                                                            BorderStroke(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.onSecondaryContainer
                                                            ), CircleShape
                                                        )
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
