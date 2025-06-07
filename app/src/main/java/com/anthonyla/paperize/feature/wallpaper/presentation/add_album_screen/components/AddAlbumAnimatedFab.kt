package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

@Composable
fun AddAlbumAnimatedFab(
    isLoading: Boolean,
    animate: Boolean,
    onImageClick: () -> Unit,
    onFolderClick: () -> Unit
) {
    val expandedWidth = 165.dp
    val collapsedFabSize = 96.dp
    val expandedFabHeight = 60.dp
    val menuItemHeight = 56.dp
    val menuItemCount = 2

    val menuColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
    val fabColor = MaterialTheme.colorScheme.primaryContainer

    var isFabExpanded by remember { mutableStateOf(false) }
    BackHandler(isFabExpanded) { isFabExpanded = false }

    val transition = updateTransition(targetState = isFabExpanded, label = "fabTransition")

    val fabWidth by transition.animateDp(
        label = "fabWidth",
        transitionSpec = {
            if (animate) {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            } else {
                snap()
            }
        }
    ) { expanded ->
        if (expanded) expandedWidth else collapsedFabSize
    }

    val fabHeight by transition.animateDp(
        label = "fabHeight",
        transitionSpec = {
            if (animate) {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            } else {
                snap()
            }
        }
    ) { expanded ->
        if (expanded) expandedFabHeight else collapsedFabSize
    }

    val menuHeight by transition.animateDp(
        label = "menuHeight",
        transitionSpec = {
            if (animate) {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            } else {
                snap()
            }
        }
    ) { expanded ->
        if (expanded) menuItemHeight * menuItemCount else 0.dp
    }

    val fabIconTransitionSpec: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = {
        if (animate) {
            (scaleIn(tween(100, 300, FastOutSlowInEasing)) + fadeIn(tween(250, 300, LinearEasing)))
                .togetherWith(fadeOut(tween(10, 0, LinearEasing)))
        } else {
            EnterTransition.None.togetherWith(ExitTransition.None)
        }
    }

    ElevatedCard(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = menuColor
        )
    ) {
        Column(
            modifier = Modifier
                .height(menuHeight)
                .width(fabWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                label = stringResource(R.string.add_images_btn),
                enter = if (animate) fadeIn(tween(300, 150, LinearEasing)) else EnterTransition.None,
                exit = if (animate) fadeOut(tween(200, 50, LinearEasing)) + scaleOut() else ExitTransition.None
            ) {
                MenuItemRow(
                    icon = Icons.Filled.PhotoLibrary,
                    text = stringResource(R.string.add_images_btn),
                    onClick = {
                        onImageClick()
                        isFabExpanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(menuItemHeight)
                )
            }

            AnimatedVisibility(
                visible = isFabExpanded,
                label = stringResource(R.string.add_folder_btn),
                enter = if (animate) fadeIn(tween(300, 150, LinearEasing)) else EnterTransition.None,
                exit = if (animate) fadeOut(tween(200, 50, LinearEasing)) + scaleOut() else ExitTransition.None
            ) {
                MenuItemRow(
                    icon = Icons.Filled.Folder,
                    text = stringResource(R.string.add_folder_btn),
                    onClick = {
                        onFolderClick()
                        isFabExpanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(menuItemHeight)
                )
            }
        }

        LargeFloatingActionButton(
            onClick = { if (!isLoading) isFabExpanded = !isFabExpanded },
            modifier = Modifier
                .width(fabWidth)
                .height(fabHeight),
            shape = RoundedCornerShape(28.dp),
            containerColor = fabColor,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            AnimatedContent(
                targetState = isFabExpanded,
                transitionSpec = fabIconTransitionSpec,
                label = "FabContent"
            ) { expanded ->
                Icon(
                    imageVector = if (expanded) Icons.Filled.Remove else Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_wallpaper)
                )
            }
        }
    }
}

/**
 * Reusable Composable for a single menu item row.
 */
@Composable
private fun MenuItemRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}