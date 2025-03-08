package com.anthonyla.paperize.feature.wallpaper.presentation.add_album_screen.components

import android.R.attr.contentDescription
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Top bar for the Add Album screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumSmallTopBar(
    title: String,
    isEmpty: Boolean,
    allSelected: Boolean,
    selectedCount: Int,
    selectionMode: Boolean,
    showSpotlight: Boolean,
    isLoad: Boolean,
    onSelectAllClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteSelected: () -> Unit,
    onConfirmationClick: (String) -> Unit,
    onSortClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var editableTitle by rememberSaveable { mutableStateOf(title) }
    var showSelectionDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = stringResource(R.string.spotlight_animation))
    val radiusValues = listOf(50f, 60f, 75f).map { targetValue ->
        infiniteTransition.animateFloat(
            initialValue = 25f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "circle_${targetValue}_animation"
        )
    }

    if (showSelectionDeleteDialog) {
        DeleteImagesAlertDialog(
            onDismissRequest = { showSelectionDeleteDialog = false },
            onConfirmation = {
                showSelectionDeleteDialog = false
                onDeleteSelected()
            }
        )
    }

    TopAppBar(
        colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
        title = {
            if (!selectionMode) {
                TextField(
                    value = editableTitle,
                    onValueChange = { editableTitle = it },
                    placeholder = { Text(stringResource(R.string.enter_the_name_of_the_album)) },
                    singleLine = true,
                    readOnly = isLoad,
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    isError = editableTitle.isEmpty(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        },
        navigationIcon = {
            if (!selectionMode) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .requiredSize(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.home_screen)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                    IconButton(onClick = onSelectAllClick) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = stringResource(
                                if (allSelected) R.string.all_images_selected_for_deletion
                                else R.string.select_all_images_for_deletion
                            ),
                            modifier = if (allSelected) {
                                Modifier
                                    .padding(4.dp)
                                    .border(2.dp, bgColor, CircleShape)
                                    .clip(CircleShape)
                                    .background(bgColor)
                            } else {
                                Modifier.padding(6.dp)
                            }
                        )
                    }
                    Text(stringResource(R.string.selected, selectedCount))
                }
            }
        },
        actions = {
            if (!selectionMode) {
                if (!isEmpty && editableTitle.isNotEmpty()) {
                    Row {
                        Box {
                            IconButton(onClick = onSortClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Sort,
                                    contentDescription = stringResource(R.string.sort_items),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { onConfirmationClick(editableTitle) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = stringResource(R.string.add_album),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            if (!showSpotlight) {
                                val primaryColor = MaterialTheme.colorScheme.inversePrimary
                                val secondaryColor = MaterialTheme.colorScheme.secondary
                                val colors = listOf(primaryColor, secondaryColor, primaryColor)
                                val animationValues = radiusValues.map { it.value }
                                val pairs = animationValues.zip(colors)

                                Canvas(modifier = Modifier.matchParentSize()) {
                                    pairs.forEach { (radius, color) ->
                                        drawCircle(
                                            color = color,
                                            radius = radius,
                                            center = Offset(size.width / 2, size.height / 2),
                                            style = Stroke(width = 5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                IconButton(onClick = { showSelectionDeleteDialog = true }) {
                    Icon(
                        imageVector = if (showSelectionDeleteDialog) Icons.Filled.Delete else Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.select_all_images_for_deletion),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    )
}