package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Animated FAB that expands to show two options: Add image and Add folder
 * Source: https://github.com/rajaumair7890/AnimatedFABMenu
 */
@Composable
fun AddAlbumAnimatedFab(
    onImageClick: () -> Unit,
    onFolderClick: () -> Unit
) {
    val expandedWidth = 165.dp
    val itemSize = 2
    val menuColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
    val fabColor = MaterialTheme.colorScheme.primaryContainer
    var isFabExpanded by remember { mutableStateOf(false) }
    BackHandler(isFabExpanded) { isFabExpanded = false }

    val fabTransition = updateTransition(targetState = isFabExpanded, label = "fabTransition")

    val expandFabHorizontally by fabTransition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "expandVertically",
        targetValueByState = {isExpanded ->
            if (isExpanded) expandedWidth else 96.dp
        }
    )

    val shrinkFabVertically by fabTransition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "shrinkHorizontally",
        targetValueByState = {isExpanded ->
            if (isExpanded) 60.dp else 96.dp
        }
    )

    val menuHeight by fabTransition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "menuHeight",
        targetValueByState = {isExpanded ->
            if (isExpanded) (itemSize * 50).dp else 0.dp
        }
    )

    ElevatedCard(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        colors = CardDefaults.elevatedCardColors().copy(
            containerColor = menuColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.height(menuHeight)
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                label = stringResource(R.string.add_images_btn),
                enter = fadeIn(tween(300, 150, LinearEasing)),
                exit = fadeOut(tween(200, 50, LinearEasing)) + scaleOut(),
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
                        modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            AnimatedVisibility(
                visible = isFabExpanded,
                label = stringResource(R.string.add_folder_btn),
                enter = fadeIn(tween(300, 150, LinearEasing)),
                exit = fadeOut(tween(200, 50, LinearEasing)) + scaleOut(),
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
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.add_folder_btn),
                        modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LargeFloatingActionButton(
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
            shape = RoundedCornerShape(28),
            containerColor = fabColor,
            onClick = { isFabExpanded = !isFabExpanded },
            modifier = Modifier
                .width(expandFabHorizontally)
                .height(shrinkFabVertically)
        ) {
            AnimatedContent(
                isFabExpanded,
                transitionSpec = {
                    when (targetState) {
                        true -> {
                            (
                                    scaleIn(
                                        tween(100, 300, FastOutSlowInEasing)
                                    ) + fadeIn(
                                        tween(250, 300, LinearEasing)
                                    )
                                    ) .togetherWith(
                                    fadeOut(
                                        tween(10, 0, LinearEasing)
                                    )
                                )
                        }
                        false -> {
                            fadeIn(
                                tween(250, 200, FastOutSlowInEasing)
                            ).togetherWith(
                                fadeOut(
                                    tween(10, 0, LinearEasing),
                                )
                            )
                        }
                    }
                },
                label = "FabContent"
            ) { isExpanded ->
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.Remove else Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_wallpaper),
                )
            }
        }
    }

}