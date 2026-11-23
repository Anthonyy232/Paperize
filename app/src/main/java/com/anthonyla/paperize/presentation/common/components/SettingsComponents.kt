package com.anthonyla.paperize.presentation.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

/** Related [SettingsRow]s drawn as one rounded block, split by hairline gaps instead of nested cards. */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = AppSpacing.large, end = AppSpacing.large, bottom = AppSpacing.small)
            )
        }
        Column(
            modifier = Modifier.clip(MaterialTheme.shapes.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.hairline),
            content = content
        )
    }
}

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = MaterialTheme.colorScheme.surfaceContainer
    val body: @Composable () -> Unit = {
        Column(
            modifier = contentModifier.padding(horizontal = AppSpacing.large, vertical = AppSpacing.medium),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
    if (onClick == null) {
        Surface(modifier = modifier.fillMaxWidth(), color = color, content = body)
    } else {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = color, content = body)
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    SettingsRow(modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)) {
        SettingSwitchRow(title, description, checked, onCheckedChange = null)
    }
}

/** A null [onCheckedChange] leaves toggling to a parent that handles the whole row. */
@Composable
fun SettingSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingText(
            title = title,
            description = description,
            modifier = Modifier
                .weight(1f)
                .padding(end = AppSpacing.large)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingText(title: String, description: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
