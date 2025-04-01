package com.anthonyla.paperize.feature.wallpaper.glance_widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.anthonyla.paperize.R
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver

class PaperizeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }
}

@Composable
private fun WidgetContent(context: Context) {
    val ACTION_CHANGE_WALLPAPER = "com.anthonyla.paperize.SHORTCUT"
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(
                {

                    val intent = Intent(ACTION_CHANGE_WALLPAPER).apply {
                        setClass(context, WallpaperBootAndChangeReceiver::class.java)
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                    context.sendBroadcast(intent)
                }
            )
    ) {
        Image(
            provider = ImageProvider(R.drawable.next_wallpaper_icon),
            contentDescription = "Paperize Widget",
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
        )
    }
}
