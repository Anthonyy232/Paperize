package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import android.content.Context
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
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
    animate: Boolean = true
) {
    fun isValidUri(context: Context, uriString: String?): Boolean {
        val uri = uriString?.toUri()
        return try {
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                inputStream?.close()
            }
            true
        } catch (e: Exception) { false }
    }

    val showCoverUri = album.coverUri != null && isValidUri(LocalContext.current, album.coverUri)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp)),
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
                        imageModel = { album.coverUri },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        ),
                        requestBuilder = {
                            Glide
                                .with(LocalContext.current)
                                .asBitmap()
                                .transition(BitmapTransitionOptions.withCrossFade())
                        },
                        loading = {
                            if (animate) {
                                Box(modifier = Modifier.matchParentSize()) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        },
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
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}