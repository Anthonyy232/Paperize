package com.anthonyla.paperize.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource

@Composable
fun SnackbarMessageEffect(
    @StringRes message: Int?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit
) {
    val text = message?.let { stringResource(it) }
    LaunchedEffect(text) {
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            onShown()
        }
    }
}
