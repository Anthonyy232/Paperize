package com.anthonyla.paperize.presentation.common.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Expandable animated FAB for adding images and folders to albums
 */
@Composable
fun AddAlbumAnimatedFab(
    isLoading: Boolean,
    animate: Boolean,
    onImageClick: () -> Unit,
    onFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedWidth = 165.dp
    val collapsedFabSize = 96.dp
    val expandedFabHeight = 60.dp
    val menuItemHeight = 56.dp
    val menuItemCount = 2

    val menuColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fabColor = MaterialTheme.colorScheme.primaryContainer

    var isFabExpanded by remember { mutableStateOf(false) }
    BackHandler(isFabExpanded) { isFabExpanded = false }

    val transition = updateTransition(targetState = isFabExpanded, label = "fabTransition")

    val fabWidth by transition.animateDp(
        label = "fabWidth",
        transitionSpec = {
            if (animate) {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
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
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
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
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            } else {
                snap()
            }
        }
    ) { expanded ->
        if (expanded) menuItemHeight * menuItemCount else 0.dp
    }

    Card(
        modifier = modifier.padding(4.dp),
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
                label = stringResource(R.string.add_wallpapers),
                enter = if (animate) fadeIn(tween(300, 150, LinearEasing)) else EnterTransition.None,
                exit = if (animate) fadeOut(tween(200, 50, LinearEasing)) + scaleOut() else ExitTransition.None
            ) {
                MenuItemRow(
                    icon = Icons.Filled.PhotoLibrary,
                    text = stringResource(R.string.add_wallpapers),
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
                label = "Add Folder",
                enter = if (animate) fadeIn(tween(300, 150, LinearEasing)) else EnterTransition.None,
                exit = if (animate) fadeOut(tween(200, 50, LinearEasing)) + scaleOut() else ExitTransition.None
            ) {
                MenuItemRow(
                    icon = Icons.Filled.Folder,
                    text = "Add Folder",
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
                label = "FabContent"
            ) { expanded ->
                Icon(
                    imageVector = if (expanded) Icons.Filled.Remove else Icons.Filled.Add,
                    contentDescription = if (expanded) "Collapse" else stringResource(R.string.add_wallpapers)
                )
            }
        }
    }
}

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
