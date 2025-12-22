package com.anthonyla.paperize.presentation.screens.wallpaper.components
import com.anthonyla.paperize.presentation.theme.AppIconSizes
import com.anthonyla.paperize.presentation.theme.AppBorderWidths
import com.anthonyla.paperize.core.constants.Constants

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.anthonyla.paperize.R
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.presentation.theme.AppShapes
import com.anthonyla.paperize.presentation.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSelectionBottomSheet(
    albums: List<AlbumSummary>,
    selectedAlbums: List<AlbumSummary>,
    onAlbumSelect: (AlbumSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.select_album),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.large),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            LazyColumn {
                items(albums, key = { it.id }) { album ->
                    val isSelected = selectedAlbums.any { it.id == album.id }
                    AlbumSelectionItem(
                        album = album,
                        isSelected = isSelected,
                        onClick = { onAlbumSelect(album) }
                    )
                    HorizontalDivider()
                }
            }

            if (albums.isEmpty()) {
                Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
                Text(
                    text = stringResource(R.string.no_albums_found),
                    modifier = Modifier
                        .padding(AppSpacing.extraLarge)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            }
        }
    }
}

@Composable
private fun AlbumSelectionItem(
    album: AlbumSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showCoverUri by remember(album.coverUri) {
        mutableStateOf(!album.coverUri.isNullOrEmpty() && isValidUri(context, album.coverUri))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.medium),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album cover thumbnail
        Box(
            modifier = Modifier
                .size(AppIconSizes.extraLarge)
                .aspectRatio(1f)
                .border(
                    width = AppBorderWidths.thin,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    shape = AppShapes.imageShape
                )
                .clip(AppShapes.imageShape),
            contentAlignment = Alignment.Center
        ) {
            if (showCoverUri) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(album.coverUri))
                        .size(Size(Constants.LIST_THUMBNAIL_SIZE, Constants.LIST_THUMBNAIL_SIZE))  // Small thumbnail for list item
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AppIconSizes.extraLarge)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PhotoAlbum,
                    contentDescription = album.name,
                    modifier = Modifier.size(AppIconSizes.medium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.width(AppSpacing.large))

        // Album info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(R.plurals.wallpaper_count, album.wallpaperCount, album.wallpaperCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Selection indicator
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isSelected) stringResource(R.string.currently_selected_album) else null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Check if URI is valid and accessible
 */
private fun isValidUri(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrEmpty()) return false
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { true } ?: false
    } catch (e: Exception) {
        false
    }
}
