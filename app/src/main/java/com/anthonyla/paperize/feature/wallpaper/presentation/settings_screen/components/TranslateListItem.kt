package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * A list item that links the user to the translation page
 */
@Composable
fun TranslateListItem(onClick: () -> Unit) {
    Row {
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
            headlineContent = {
                Text(
                    text = stringResource(R.string.translation),
                    style = MaterialTheme.typography.titleMedium
                ) },
            supportingContent = {
                Text(
                    text = stringResource(R.string.help_translate_the_app_on_crowdin),
                    style = MaterialTheme.typography.bodySmall
                ) },
            trailingContent = {},
            leadingContent = {
                Icon(
                    Icons.Outlined.Translate,
                    contentDescription = stringResource(R.string.translation),
                    tint = MaterialTheme.colorScheme.primary
                ) },
            tonalElevation = 5.dp
        )
    }
}