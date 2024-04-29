package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PrivacyTip
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
 * A list item that displays the licenses
 */
@Composable
fun LicenseListItem(onLicenseClick: () -> Unit) {
    Row {
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onLicenseClick() },
            headlineContent = {
                Text(
                    text = stringResource(R.string.licenses),
                    style = MaterialTheme.typography.titleMedium
                ) },
            supportingContent = {
                Text(
                    text = "Click here to view the licenses",
                    style = MaterialTheme.typography.bodySmall
                ) },
            trailingContent = {},
            leadingContent = {
                Icon(
                    Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = stringResource(R.string.licenses),
                    tint = MaterialTheme.colorScheme.primary
                ) },
            tonalElevation = 5.dp
        )
    }
}