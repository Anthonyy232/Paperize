package com.anthonyla.paperize.presentation.screens.wallpaper.components

import android.app.WallpaperManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.theme.AppShapes
import com.anthonyla.paperize.presentation.theme.AppSpacing
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Retry helper for reading wallpapers with exponential backoff
 *
 * Android's WallpaperManager writes wallpapers to disk asynchronously after setBitmap() returns.
 * The encoding process can take 1-2 seconds, during which time reading the wallpaper back will
 * fail with ImageDecoder.DecodeException. This helper retries with increasing delays to allow
 * the system time to complete encoding.
 *
 * @param maxAttempts Maximum number of retry attempts (default 4)
 * @param delayMs Initial delay in milliseconds between retries (default 500ms)
 * @param block The operation to retry
 * @return The result of the operation, or null if all attempts fail
 */
private suspend fun <T> retryWallpaperRead(
    maxAttempts: Int = 4,
    delayMs: Long = 500,
    block: suspend () -> T
): T? {
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: android.graphics.ImageDecoder.DecodeException) {
            lastException = e
            // Only delay if we have more attempts left
            if (attempt < maxAttempts - 1) {
                // Exponential backoff: 500ms, 1000ms, 1500ms, 2000ms
                delay(delayMs * (attempt + 1))
            }
        } catch (e: Exception) {
            // For non-decode exceptions, don't retry
            return null
        }
    }

    // All attempts failed
    return null
}

/**
 * Displays current home and lock screen wallpapers
 *
 * Automatically updates when wallpapers change using WallpaperManager.OnColorsChangedListener.
 * This allows the preview to stay in sync with system wallpaper changes from any source
 * (this app's wallpaper changer, system settings, other apps, etc.).
 *
 * Uses smooth fade animations and placeholder boxes to prevent layout jumping.
 * Adapts to different screen sizes and orientations following Material 3 responsive design.
 * Respects the app's animate setting for accessibility and user preference.
 */
@Composable
fun CurrentWallpaperPreview(
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var homeWallpaper by remember { mutableStateOf<Drawable?>(null) }
    var lockWallpaper by remember { mutableStateOf<Drawable?>(null) }
    var hasPermission by remember { mutableStateOf(true) }

    // Use rememberSaveable to prevent reloading on navigation
    var refreshTrigger by rememberSaveable { mutableIntStateOf(0) }
    var pendingRefresh by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Calculate appropriate aspect ratio based on device screen
    // Use actual screen dimensions for accurate wallpaper preview
    val screenAspectRatio = remember(configuration) {
        val screenWidth = configuration.screenWidthDp.toFloat()
        val screenHeight = configuration.screenHeightDp.toFloat()
        // Use the shorter dimension as width for portrait-oriented preview
        val aspectRatio = min(screenWidth, screenHeight) / kotlin.math.max(screenWidth, screenHeight)
        aspectRatio
    }

    // Listen for wallpaper changes with debouncing
    // When both home and lock wallpapers are set, the listener fires twice.
    // Debouncing prevents multiple rapid refreshes that can cause UI flashing.
    DisposableEffect(Unit) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val handler = Handler(Looper.getMainLooper())
        val listener = WallpaperManager.OnColorsChangedListener { _, _ ->
            // Mark that a refresh is pending instead of triggering immediately
            pendingRefresh = true
        }

        // Register listener for both home and lock screen wallpaper changes
        wallpaperManager.addOnColorsChangedListener(listener, handler)

        onDispose {
            wallpaperManager.removeOnColorsChangedListener(listener)
        }
    }

    // Debounced refresh effect
    // Waits 2 seconds after the last wallpaper change before refreshing
    // This ensures both home and lock wallpaper changes complete before preview updates
    LaunchedEffect(pendingRefresh) {
        if (pendingRefresh) {
            delay(Constants.WALLPAPER_CHANGE_DEBOUNCE_MS)
            // Atomically update state to prevent race conditions
            pendingRefresh = false
            refreshTrigger++
        }
    }

    // Fetch wallpapers on initial load and when refreshTrigger changes
    LaunchedEffect(refreshTrigger) {
        // Only set loading on initial load, not on refresh
        if (refreshTrigger == 0) {
            isLoading = true
        }

        val (home, lock, hasAccess) = withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)

                // API 34+ has getDrawable(int which), earlier versions use getDrawable()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Get home screen wallpaper with retry logic
                    // Android's WallpaperManager writes wallpapers asynchronously to disk
                    // We need to retry with increasing delays to allow encoding to complete
                    val homeDrawable = retryWallpaperRead(
                        maxAttempts = 4,
                        delayMs = 500
                    ) {
                        wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)
                    }

                    // Get lock screen wallpaper with retry logic
                    val lockDrawable = retryWallpaperRead(
                        maxAttempts = 4,
                        delayMs = 500
                    ) {
                        wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                    } ?: homeDrawable

                    Triple(homeDrawable, lockDrawable, true)
                } else {
                    // For API < 34, use getDrawable() without flags (gets current wallpaper)
                    val drawable = retryWallpaperRead(
                        maxAttempts = 4,
                        delayMs = 500
                    ) {
                        wallpaperManager.getDrawable()
                    }
                    // Lock screen wallpaper not separately accessible on older APIs
                    Triple(drawable, drawable, true)
                }
            } catch (e: SecurityException) {
                Triple(null, null, false)
            } catch (e: Exception) {
                // Handle other exceptions silently
                Triple(null, null, true)
            }
        }
        // Update state on main thread
        homeWallpaper = home
        lockWallpaper = lock
        hasPermission = hasAccess
        isLoading = false
    }

    // Don't show if no permission
    if (!hasPermission) {
        return
    }

    // Always render the Card with reserved space to prevent layout jumping
    // Constrain max width for tablets following Material 3 responsive design
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)  // Material 3 guideline: max content width on large screens
            .padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
        shape = AppShapes.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.current_wallpapers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = AppSpacing.medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Lock wallpaper preview (on the left)
                WallpaperPreviewBox(
                    wallpaper = lockWallpaper,
                    isLoading = isLoading,
                    aspectRatio = screenAspectRatio,
                    contentDescription = stringResource(R.string.content_desc_current_lock_wallpaper),
                    animate = animate,
                    modifier = Modifier.weight(1f)
                )

                // Home wallpaper preview (on the right)
                WallpaperPreviewBox(
                    wallpaper = homeWallpaper,
                    isLoading = isLoading,
                    aspectRatio = screenAspectRatio,
                    contentDescription = stringResource(R.string.content_desc_current_home_wallpaper),
                    animate = animate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Displays a single wallpaper preview with smooth fade animation
 *
 * Uses device screen aspect ratio for accurate representation and placeholder
 * background to prevent layout jumping during loading.
 */
@Composable
private fun WallpaperPreviewBox(
    wallpaper: Drawable?,
    isLoading: Boolean,
    aspectRatio: Float,
    contentDescription: String,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .border(
                width = 3.dp,
                color = Color.Black,
                shape = AppShapes.imageShape
            )
            .clip(AppShapes.imageShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AnimatedVisibility(
            visible = !isLoading && wallpaper != null,
            enter = if (animate) {
                fadeIn(animationSpec = tween(300))
            } else {
                fadeIn(animationSpec = tween(0))
            },
            exit = if (animate) {
                fadeOut(animationSpec = tween(300))
            } else {
                fadeOut(animationSpec = tween(0))
            }
        ) {
            wallpaper?.let { drawable ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(drawable)
                        .size(Size(600, 1200))  // Limit size for performance - suitable for preview
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
