package com.anthonyla.paperize.presentation.screens.album_view.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.anthonyla.paperize.domain.model.Folder
import com.anthonyla.paperize.presentation.common.components.SelectableGridCard
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

@Composable
fun FolderItem(
    folder: Folder,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableGridCard(
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.5f)
                .align(Alignment.Center),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Text(
            text = folder.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppSpacing.small)
        )
    }
}
