package com.anthonyla.paperize.presentation.screens.wallpaper.components

import android.app.WallpaperManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Displays current home and lock screen wallpapers
 */
@Composable
fun CurrentWallpaperPreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var homeWallpaper by remember { mutableStateOf<Drawable?>(null) }
    var lockWallpaper by remember { mutableStateOf<Drawable?>(null) }
    var hasPermission by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)

                // Get home screen wallpaper
                homeWallpaper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)
                } else {
                    wallpaperManager.drawable
                }

                // Get lock screen wallpaper
                lockWallpaper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                    } catch (e: Exception) {
                        // Lock wallpaper might not be set separately, use home wallpaper
                        homeWallpaper
                    }
                } else {
                    homeWallpaper
                }
            } catch (e: SecurityException) {
                hasPermission = false
            } catch (e: Exception) {
                // Handle other exceptions silently
            }
        }
    }

    if (!hasPermission) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Current Wallpapers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Home wallpaper preview
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Home Screen",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    homeWallpaper?.let { drawable ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(drawable)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Current home screen wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                // Lock wallpaper preview
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Lock Screen",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    lockWallpaper?.let { drawable ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(drawable)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Current lock screen wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}
