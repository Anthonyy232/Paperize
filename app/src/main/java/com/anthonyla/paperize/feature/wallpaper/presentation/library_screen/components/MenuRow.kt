package com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A row that contains an icon, text, and an onClick listener.
 */
@Composable
fun MenuRow(text: String, icon: ImageVector?, horizontalExpansion: Dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(horizontalExpansion)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = text,
            overflow = TextOverflow.Visible,
            maxLines = 1,
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}