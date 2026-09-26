package com.anthonyla.paperize.presentation.screens.album_view.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing

enum class SortOption(val labelRes: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    DATE_ADDED_ASC(R.string.sort_date_added_asc),
    DATE_ADDED_DESC(R.string.sort_date_added_desc),
    DATE_MODIFIED_ASC(R.string.sort_date_modified_asc),
    DATE_MODIFIED_DESC(R.string.sort_date_modified_desc)
}

/**
 * Bottom sheet for sorting wallpapers and folders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    selectedOption: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).selectableGroup()) {
            Text(
                text = stringResource(R.string.sort),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(AppSpacing.large)
            )

            HorizontalDivider()

            SortOption.entries.forEachIndexed { index, option ->
                if (index > 0 && index % 2 == 0) HorizontalDivider()
                ListItem(
                    content = { Text(stringResource(option.labelRes)) },
                    leadingContent = {
                        Icon(
                            if (option == SortOption.NAME_ASC || option == SortOption.NAME_DESC)
                                Icons.Default.SortByAlpha else Icons.Default.AccessTime,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        RadioButton(selected = selectedOption == option, onClick = null)
                    },
                    modifier = Modifier.selectable(
                        selected = selectedOption == option,
                        role = Role.RadioButton,
                        onClick = {
                            onSortSelected(option)
                            onDismiss()
                        }
                    )
                )
            }
            // Bottom padding
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
