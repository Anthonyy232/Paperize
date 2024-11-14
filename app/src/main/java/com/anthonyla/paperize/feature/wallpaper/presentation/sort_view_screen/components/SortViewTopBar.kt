package com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Top bar for the folder view screen.
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
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(16.dp)
                    .requiredSize(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.home_screen),
                )
            }
        },
        actions = {
            Row {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .padding(16.dp)
                        .requiredSize(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = stringResource(R.string.sort_items),
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
                            text = { Text("Sort Alphabetically (A-Z)") },
                            onClick = {
                                onSortAlphabetically()
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort Alphabetically (Z-A)") },
                            onClick = {
                                onSortAlphabeticallyReverse()
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Last Modified (Newest)") },
                            onClick = {
                                onSortByLastModified()
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Last Modified (Oldest)") },
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
                        .padding(16.dp)
                        .requiredSize(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.sort_items),
                    )
                }
            }
        }
    )
}