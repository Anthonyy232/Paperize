package com.anthonyla.paperize.presentation.screens.library.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.presentation.common.components.InteractiveCard
import com.anthonyla.paperize.presentation.theme.AppBorderWidths
import com.anthonyla.paperize.presentation.theme.AppElevation
import com.anthonyla.paperize.presentation.theme.AppRadii
import com.anthonyla.paperize.presentation.theme.AppShapes
import com.anthonyla.paperize.presentation.theme.AppSpacing

@Composable
fun AlbumItem(
    album: AlbumSummary,
    onAlbumViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cover = rememberAsyncImagePainter(
        ImageRequest.Builder(context).data(album.coverUri).size(Size(400, 400)).build(),
        contentScale = ContentScale.Crop
    )
    val coverState by cover.state.collectAsState()

    InteractiveCard(
        onClick = onAlbumViewClick,
        modifier = modifier.fillMaxSize(),
        defaultRadius = AppRadii.large,
        pressedRadius = AppRadii.huge,
        elevation = AppElevation.level1
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.medium),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(AppShapes.smallCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                        width = AppBorderWidths.medium,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (coverState is AsyncImagePainter.State.Success) {
                    Image(
                        painter = cover,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PhotoAlbum,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(0.45f),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
