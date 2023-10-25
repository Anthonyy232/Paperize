package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AlbumItem(
    //albumCover: Uri,
) {
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(false, label = "")

    val paddingTransition by transition.animateDp(label = "") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "") { selected ->
        if (selected) 18.dp else 10.dp
    }

    ElevatedCard(
        elevation = CardDefaults.cardElevation(),
        modifier = Modifier.size(width = 240.dp, height = 100.dp),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Text(
            text = "Text",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
        )
    }
}