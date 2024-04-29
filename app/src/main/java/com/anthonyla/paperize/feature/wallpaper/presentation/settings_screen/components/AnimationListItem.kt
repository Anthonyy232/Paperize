package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Enable animation switch
 */
@Composable
fun AnimationListItem(animate: Boolean, onAnimateClick: (Boolean) -> Unit) {
    Row {
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onAnimateClick(!animate) },
            headlineContent = {
                Text(
                    text = "Animation",
                    style = MaterialTheme.typography.titleMedium
                ) },
            supportingContent = {
                Text(
                    text = "Increase visual appeal",
                    style = MaterialTheme.typography.bodySmall
                ) },
            trailingContent = {
                Switch(
                    modifier = Modifier.semantics { contentDescription = "Animation" },
                    checked = animate,
                    onCheckedChange = onAnimateClick,
                    enabled = true
                ) },
            leadingContent = {
                Icon(
                    Icons.Outlined.Animation,
                    contentDescription = "Animation",
                    tint = MaterialTheme.colorScheme.primary
                ) },
            tonalElevation = 5.dp
        )
    }
}