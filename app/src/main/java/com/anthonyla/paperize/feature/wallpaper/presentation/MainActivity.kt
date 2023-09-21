package com.anthonyla.paperize.feature.wallpaper.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.themes.PaperizeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Paperize)
        setContent {
            PaperizeTheme() {
                val navController = rememberNavController()
                PaperizeApp(navController)
            }
        }
    }
}