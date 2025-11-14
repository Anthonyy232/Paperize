package com.anthonyla.paperize.presentation.screens.effects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.components.SettingSliderItem
import com.anthonyla.paperize.presentation.common.components.SettingSwitchItem
import com.anthonyla.paperize.presentation.common.components.SettingSwitchWithSliderItem

/**
 * Wallpaper effects configuration screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperEffectsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WallpaperEffectsViewModel = hiltViewModel()
) {
    val effects by viewModel.effects.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Effects") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Blur effect
            SettingSwitchWithSliderItem(
                title = stringResource(R.string.change_blur),
                description = stringResource(R.string.add_blur_to_the_image),
                checked = effects.enableBlur,
                onCheckedChange = { viewModel.updateBlurEnabled(it) },
                sliderValue = effects.blurPercentage.toFloat(),
                onSliderValueChange = { viewModel.updateBlurPercentage(it.toInt()) },
                valueRange = 0f..100f,
                steps = 19,
                valueLabel = stringResource(R.string.percentage, effects.blurPercentage)
            )

            // Brightness effect
            SettingSwitchWithSliderItem(
                title = stringResource(R.string.change_brightness),
                description = stringResource(R.string.change_the_image_brightness),
                checked = effects.enableDarken,
                onCheckedChange = { viewModel.updateDarkenEnabled(it) },
                sliderValue = effects.darkenPercentage.toFloat(),
                onSliderValueChange = { viewModel.updateDarkenPercentage(it.toInt()) },
                valueRange = -100f..100f,
                steps = 39,
                valueLabel = stringResource(R.string.percentage, effects.darkenPercentage)
            )

            // Vignette effect
            SettingSwitchWithSliderItem(
                title = stringResource(R.string.change_vignette),
                description = stringResource(R.string.darken_the_edges_of_the_image),
                checked = effects.enableVignette,
                onCheckedChange = { viewModel.updateVignetteEnabled(it) },
                sliderValue = effects.vignettePercentage.toFloat(),
                onSliderValueChange = { viewModel.updateVignettePercentage(it.toInt()) },
                valueRange = 0f..100f,
                steps = 19,
                valueLabel = stringResource(R.string.percentage, effects.vignettePercentage)
            )

            // Grayscale effect
            SettingSwitchItem(
                title = stringResource(R.string.gray_filter),
                description = stringResource(R.string.make_the_colors_grayscale),
                checked = effects.enableGrayscale,
                onCheckedChange = { viewModel.updateGrayscaleEnabled(it) }
            )

            Divider()

            // Additional settings
            Text(
                text = "Additional Options",
                style = MaterialTheme.typography.titleMedium
            )

            SettingSwitchItem(
                title = stringResource(R.string.shuffle),
                description = stringResource(R.string.shuffle_the_wallpapers),
                checked = effects.shuffle,
                onCheckedChange = { viewModel.updateShuffle(it) }
            )

            SettingSwitchItem(
                title = stringResource(R.string.skip_landscape_mode),
                description = stringResource(R.string.prevent_changing_during_landscape_mode),
                checked = effects.skipLandscape,
                onCheckedChange = { viewModel.updateSkipLandscape(it) }
            )

            SettingSwitchItem(
                title = stringResource(R.string.skip_non_interactive_state),
                description = stringResource(R.string.prevent_changing_during_non_interactive_state),
                checked = effects.skipNonInteractive,
                onCheckedChange = { viewModel.updateSkipNonInteractive(it) }
            )
        }
    }
}
