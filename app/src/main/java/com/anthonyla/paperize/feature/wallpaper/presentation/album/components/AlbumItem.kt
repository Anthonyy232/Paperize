package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.anthonyla.paperize.core.decompress
import com.anthonyla.paperize.core.isValidUri
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * AlbumItem composable is a single item view for the Album list. Shows the thumbnail cover image.
 */
@Composable
fun AlbumItem(
    album: Album,
    onAlbumViewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val showCoverUri by remember { mutableStateOf(!album.coverUri.isNullOrEmpty() && isValidUri(context, album.coverUri)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        onClick = onAlbumViewClick
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (showCoverUri) {
                    GlideImage(
                        imageModel = {
                            album.coverUri?.decompress("content://com.android.externalstorage.documents/")?.toUri()
                        },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            requestSize = IntSize(300, 300),
                        ),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.padding(6.dp))
            Text(
                text = album.displayedAlbumName,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Start),
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}