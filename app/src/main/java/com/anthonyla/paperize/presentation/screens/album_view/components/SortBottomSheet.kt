package com.anthonyla.paperize.presentation.screens.album_view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_ADDED_ASC,
    DATE_ADDED_DESC,
    DATE_MODIFIED_ASC,
    DATE_MODIFIED_DESC
}

/**
 * Bottom sheet for sorting wallpapers and folders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.sort),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            HorizontalDivider()

            // Name Ascending
            ListItem(
                headlineContent = { Text("Name (A-Z)") },
                leadingContent = {
                    Icon(Icons.Default.SortByAlpha, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onSortSelected(SortOption.NAME_ASC)
                    onDismiss()
                }
            )

            // Name Descending
            ListItem(
                headlineContent = { Text("Name (Z-A)") },
                leadingContent = {
                    Icon(Icons.Default.SortByAlpha, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onSortSelected(SortOption.NAME_DESC)
                    onDismiss()
                }
            )

            HorizontalDivider()

            // Date Added Ascending
            ListItem(
                headlineContent = { Text("Date Added (Oldest First)") },
                leadingContent = {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onSortSelected(SortOption.DATE_ADDED_ASC)
                    onDismiss()
                }
            )

            // Date Added Descending
            ListItem(
                headlineContent = { Text("Date Added (Newest First)") },
                leadingContent = {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onSortSelected(SortOption.DATE_ADDED_DESC)
                    onDismiss()
                }
            )

            HorizontalDivider()

            // Date Modified Ascending
            ListItem(
                headlineContent = { Text("Date Modified (Oldest First)") },
                leadingContent = {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onSortSelected(SortOption.DATE_MODIFIED_ASC)
                    onDismiss()
                }
            )

            // Date Modified Descending
            ListItem(
                headlineContent = { Text("Date Modified (Newest First)") },
                leadingContent = {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    onSortSelected(SortOption.DATE_MODIFIED_DESC)
                    onDismiss()
                }
            )

            // Bottom padding
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
