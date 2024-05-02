package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CardColors
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components.MenuRow

/**
 * Animated FAB that expands to show two options: Add image and Add folder
 * Source: https://github.com/rajaumair7890/AnimatedFABMenu
 */
@Composable
fun AddAlbumAnimatedFab(
    menuOptions: AddAlbumFabMenuOptions,
    animate: Boolean,
    onImageClick: () -> Unit,
    onFolderClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val fabTransition = updateTransition(targetState = expanded, label = "")

    BackHandler(expanded) { expanded = false }

    val expandHorizontalSize = Pair(155.dp, 90.dp)
    val expandHorizontally by fabTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) expandHorizontalSize.first else expandHorizontalSize.second
        }
    )

    val shrinkVerticalSize = Pair(50.dp, 90.dp)
    val shrinkVertically by fabTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) shrinkVerticalSize.first else shrinkVerticalSize.second
        }
    )

    val menuHeightSize = Pair(100.dp, 90.dp)
    val menuHeight by fabTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) menuHeightSize.first else menuHeightSize.second
        }
    )

    if (animate) {
        ElevatedCard(
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
                contentColor = Color.Transparent,
                disabledContentColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .padding(4.dp)
                .width(expandHorizontally)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { expanded = false })
                }
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(300, 0, FastOutSlowInEasing)),
                exit = fadeOut(tween(300, 0, FastOutSlowInEasing)),
                modifier = Modifier.animateContentSize(tween(50, 0, FastOutSlowInEasing)),
            ) {
                Surface(tonalElevation = 5.dp) {
                    Column(modifier = Modifier.height(menuHeight)) {
                        MenuRow(
                            text = menuOptions.imageOption.text,
                            icon = menuOptions.imageOption.icon,
                            horizontalExpansion = expandHorizontally,
                            onClick = {
                                expanded = false
                                onImageClick()
                            }
                        )
                        MenuRow(
                            text = menuOptions.folderOption.text,
                            icon = menuOptions.folderOption.icon,
                            horizontalExpansion = expandHorizontally,
                            onClick = {
                                expanded = false
                                onFolderClick()
                            }
                        )
                    }
                }
            }
            Surface(
                tonalElevation = 3.dp,
            ) {
                LargeFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .width(expandHorizontally)
                        .height(shrinkVertically)
                        .animateContentSize(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (!expanded) Icons.Filled.Add else Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.add_wallpaper),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
    else {
        ElevatedCard(
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
                contentColor = Color.Transparent,
                disabledContentColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .padding(4.dp)
                .width(if (expanded) expandHorizontalSize.first else expandHorizontalSize.second)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { expanded = false })
                }
        ) {
            if (expanded) {
                Surface(tonalElevation = 5.dp) {
                    Column(modifier = Modifier.height(if (expanded) menuHeightSize.first else menuHeightSize.second)) {
                        MenuRow(
                            text = menuOptions.imageOption.text,
                            icon = menuOptions.imageOption.icon,
                            horizontalExpansion = if (expanded) expandHorizontalSize.first else expandHorizontalSize.second,
                            onClick = {
                                expanded = false
                                onImageClick()
                            }
                        )
                        MenuRow(
                            text = menuOptions.folderOption.text,
                            icon = menuOptions.folderOption.icon,
                            horizontalExpansion = if (expanded) expandHorizontalSize.first else expandHorizontalSize.second,
                            onClick = {
                                expanded = false
                                onFolderClick()
                            }
                        )
                    }
                }
            }
            Surface(
                tonalElevation = 3.dp,
            ) {
                LargeFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .width(if (expanded) expandHorizontalSize.first else expandHorizontalSize.second)
                        .height(if (expanded) shrinkVerticalSize.first else shrinkVerticalSize.second)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (!expanded) Icons.Filled.Add else Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.add_wallpaper),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}