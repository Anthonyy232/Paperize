package com.anthonyla.paperize.core.presentation.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetTransparentSystemBars(darkMode: Boolean) {
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current
    SideEffect {
        with(view.context as Activity) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !darkMode
            )
        }
    }
}