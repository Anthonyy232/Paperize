package com.anthonyla.paperize.presentation.screens.sort.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing

/**
 * Top bar for the sort view screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortViewTopBar(
    title: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSortAlphabetically: () -> Unit,
    onSortByLastModified: () -> Unit,
    onSortAlphabeticallyReverse: () -> Unit,
    onSortByLastModifiedReverse: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
        title = { Text(title) },
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
                )
            }
        },
        actions = {
            Row {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .padding(AppSpacing.large)
                        .requiredSize(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.content_desc_sort_items),
                    )
                }
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                ) {
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_a_z)) },
                            onClick = {
                                onSortAlphabetically()
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_z_a)) },
                            onClick = {
                                onSortAlphabeticallyReverse()
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_newest)) },
                            onClick = {
                                onSortByLastModified()
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_oldest)) },
                            onClick = {
                                onSortByLastModifiedReverse()
                                showSortMenu = false
                            }
                        )
                    }
                }
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .padding(AppSpacing.large)
                        .requiredSize(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.content_desc_save),
                    )
                }
            }
        }
    )
}
