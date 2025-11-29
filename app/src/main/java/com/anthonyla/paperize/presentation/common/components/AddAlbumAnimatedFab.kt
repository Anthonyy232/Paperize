package com.anthonyla.paperize.presentation.common.components

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R

/**
 * Material 3 Expressive FAB Menu for adding images and folders to albums
 *
 * Migrated to use FloatingActionButtonMenu, ToggleFloatingActionButton, and
 * FloatingActionButtonMenuItem from Material 3 Expressive design system.
 *
 * Features:
 * - Physics-based animations from expressive motion scheme
 * - Staggered menu item reveal animations
 * - Smooth icon morphing between Add and Close
 * - Accessibility support with proper traversal and custom actions
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddAlbumAnimatedFab(
    isLoading: Boolean,
    onImageClick: () -> Unit,
    onFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Collapse menu when back button is pressed
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                checked = fabMenuExpanded,
                onCheckedChange = { if (!isLoading) fabMenuExpanded = !fabMenuExpanded }
            ) {
                // Animate between Add and Close icons based on checked progress
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = if (fabMenuExpanded) {
                        stringResource(R.string.content_desc_collapse)
                    } else {
                        stringResource(R.string.add_wallpapers)
                    },
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        }
    ) {
        // Menu items are revealed with staggered animation when menu expands
        FloatingActionButtonMenuItem(
            onClick = {
                onImageClick()
                fabMenuExpanded = false
            },
            icon = {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null
                )
            },
            text = {
                Text(stringResource(R.string.add_wallpapers))
            }
        )

        FloatingActionButtonMenuItem(
            onClick = {
                onFolderClick()
                fabMenuExpanded = false
            },
            icon = {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null
                )
            },
            text = {
                Text(stringResource(R.string.add_folder))
            }
        )
    }
}
