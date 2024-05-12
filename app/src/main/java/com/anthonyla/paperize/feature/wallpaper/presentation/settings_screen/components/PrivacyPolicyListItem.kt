package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * Privacy policy card
 */
@Composable
fun PrivacyPolicyListItem(onPrivacyPolicyClick: () -> Unit) {
    Row {
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onPrivacyPolicyClick() },
            headlineContent = {
                Text(
                    text = stringResource(R.string.privacy_policy),
                    style = MaterialTheme.typography.titleMedium
                ) },
            supportingContent = {
                Text(
                    text = stringResource(R.string.click_here_to_view_our_privacy_policy),
                    style = MaterialTheme.typography.bodySmall
                ) },
            trailingContent = {},
            leadingContent = {
                Icon(
                    Icons.Outlined.PrivacyTip,
                    contentDescription = stringResource(R.string.privacy_policy),
                    tint = MaterialTheme.colorScheme.primary
                ) },
            tonalElevation = 5.dp
        )
    }
}