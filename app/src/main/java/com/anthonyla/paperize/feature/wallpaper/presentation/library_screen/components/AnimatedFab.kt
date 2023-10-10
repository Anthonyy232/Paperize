package com.anthonyla.paperize.feature.wallpaper.presentation.library_screen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

@Composable
fun AnimatedFab(menuOptions: FabMenuOptions, onImageClick: () -> Unit, onFolderClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val fabTransition = updateTransition(targetState = expanded, label = "")

    BackHandler(expanded) { expanded = false }

    val expandHorizontally by fabTransition.animateDp(
        transitionSpec = { tween(200, 25, FastOutSlowInEasing) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) 155.dp else 90.dp
        }
    )

    val shrinkVertically by fabTransition.animateDp(
        transitionSpec = { tween(200, 50, FastOutSlowInEasing) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) 50.dp else 90.dp
        }
    )

    val menuHeight by fabTransition.animateDp(
        transitionSpec = { tween(150, 50, FastOutSlowInEasing) }, label = "",
        targetValueByState = { isExpanded ->
            if (isExpanded) 100.dp else 90.dp
        }
    )
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .padding(4.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { expanded = false })
            }
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(300, 0, FastOutSlowInEasing)),
            exit = fadeOut(tween(300, 0, FastOutSlowInEasing)),
            modifier = Modifier.animateContentSize(tween(50, 0, FastOutSlowInEasing)),
        ) {
            Surface(tonalElevation = 5.dp) {
                Column(modifier = Modifier.height(menuHeight)) {
                    MenuRow(
                        text = menuOptions.imageOption.text,
                        icon = menuOptions.imageOption.icon,
                        horizontalExpansion = expandHorizontally,
                        onClick = {
                            expanded = false
                            onImageClick()
                        }
                    )
                    MenuRow(
                        text = menuOptions.folderOption.text,
                        icon = menuOptions.folderOption.icon,
                        horizontalExpansion = expandHorizontally,
                        onClick = {
                            expanded = false
                            onFolderClick()
                        }
                    )
                }
            }
        }
        Surface(
            tonalElevation = 3.dp
        ) {
            LargeFloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .width(expandHorizontally)
                    .height(shrinkVertically)
                    .animateContentSize(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (!expanded) Icons.Filled.Add else Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.add_wallpaper),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}