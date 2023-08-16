package com.anthonyla.paperize.feature.wallpaper.presentation.library.components

import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

@Composable
fun AnimatedFloatingActionButton(menuOptions: FabMenuOptions, onClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val fabTransition = updateTransition(targetState = expanded, label = "")

    val expandHorizontally by fabTransition.animateDp(
        transitionSpec = { tween(200, 25, FastOutSlowInEasing) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) 155.dp else 60.dp
        }
    )

    val shrinkVertically by fabTransition.animateDp(
        transitionSpec = { tween(200, 25, FastOutSlowInEasing) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) 50.dp else 60.dp
        }
    )

    val menuHeight by fabTransition.animateDp(
        transitionSpec = { tween(150, 100, FastOutSlowInEasing) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) 100.dp else 0.dp
        }
    )
    ElevatedCard(
        modifier = Modifier.padding(4.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(500, 0, FastOutSlowInEasing)),
            exit = fadeOut(tween(500, 0, FastOutSlowInEasing)),
        ) {
            Column(
                modifier = Modifier
                    .height(menuHeight)
            ) {
                MenuRow(menuOptions.imageOption.text, menuOptions.imageOption.icon, expandHorizontally) {
                    onClick(menuOptions.imageOption.id)
                }
                MenuRow(menuOptions.folderOption.text, menuOptions.folderOption.icon, expandHorizontally) {
                    onClick(menuOptions.folderOption.id)
                }
            }
        }
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .width(expandHorizontally)
                .height(shrinkVertically)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (!expanded) Icons.Filled.Add else Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.add_wallpaper),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}