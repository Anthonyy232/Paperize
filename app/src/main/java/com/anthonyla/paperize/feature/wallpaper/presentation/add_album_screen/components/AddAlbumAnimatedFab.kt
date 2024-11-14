package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    val itemSize = 2
    val menuColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
    val fabColor = MaterialTheme.colorScheme.primaryContainer

    var isFabExpanded by remember { mutableStateOf(false) }
    BackHandler(isFabExpanded) { isFabExpanded = false }

    val fabTransition = if (animate) {
        updateTransition(targetState = isFabExpanded, label = "fabTransition")
    } else null

    val expandFabHorizontally = if (animate) {
        fabTransition!!.animateDp(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            },
            label = "expandHorizontally",
            targetValueByState = { expanded ->
                if (expanded) expandedWidth else 96.dp
            }
        ).value
    } else {
        if (isFabExpanded) expandedWidth else 96.dp
    }

    val shrinkFabVertically = if (animate) {
        fabTransition?.animateDp(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            },
            label = "shrinkVertically",
            targetValueByState = { expanded ->
                if (expanded) 60.dp else 96.dp
            }
        )?.value ?: 0.dp
    } else {
        if (isFabExpanded) 60.dp else 96.dp
    }

    val menuHeight = if (animate) {
        fabTransition?.animateDp(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            },
            label = "menuHeight",
            targetValueByState = { expanded ->
                if (expanded) (itemSize * 50).dp else 0.dp
            }
        )?.value ?: 0.dp
    } else {
        if (isFabExpanded) (itemSize * 50).dp else 0.dp
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
            modifier = Modifier.height(menuHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                label = stringResource(R.string.add_images_btn),
                enter = if (animate) fadeIn(tween(300, 150, LinearEasing)) else EnterTransition.None,
                exit = if (animate) fadeOut(tween(200, 50, LinearEasing)) + scaleOut() else ExitTransition.None
            ) {
                Row(
                    modifier = Modifier
                        .width(expandFabHorizontally)
                        .clickable {
                            onImageClick()
                            isFabExpanded = false
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = stringResource(R.string.add_images_btn),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.add_images_btn),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isFabExpanded,
                label = stringResource(R.string.add_folder_btn),
                enter = if (animate) fadeIn(tween(300, 150, LinearEasing)) else EnterTransition.None,
                exit = if (animate) fadeOut(tween(200, 50, LinearEasing)) + scaleOut() else ExitTransition.None
            ) {
                Row(
                    modifier = Modifier
                        .width(expandFabHorizontally)
                        .clickable {
                            onFolderClick()
                            isFabExpanded = false
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = stringResource(R.string.add_folder_btn),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.add_folder_btn),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }

        LargeFloatingActionButton(
            onClick = { if (!isLoading) isFabExpanded = !isFabExpanded },
            modifier = Modifier
                .width(expandFabHorizontally)
                .height(shrinkFabVertically),
            shape = RoundedCornerShape(28),
            containerColor = fabColor,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            if (animate) {
                AnimatedContent(
                    targetState = isFabExpanded,
                    transitionSpec = {
                        if (targetState) {
                            (scaleIn(tween(100, 300, FastOutSlowInEasing)) + fadeIn(tween(250, 300, LinearEasing)))
                                .togetherWith(fadeOut(tween(10, 0, LinearEasing)))
                        } else {
                            fadeIn(tween(250, 200, FastOutSlowInEasing))
                                .togetherWith(fadeOut(tween(10, 0, LinearEasing)))
                        }
                    },
                    label = "FabContent"
                ) { expanded ->
                    Icon(
                        imageVector = if (expanded) Icons.Filled.Remove else Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_wallpaper)
                    )
                }
            } else {
                Icon(
                    imageVector = if (isFabExpanded) Icons.Filled.Remove else Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_wallpaper)
                )
            }
        }
    }
}