package com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.anthonyla.paperize.R

/**
 * Title for the privacy screen.
 */
@Composable
fun PrivacyTitle(
    largeTopAppBarHeight: Dp,
    smallTopAppBarHeight: Dp,
    paddingMedium: Dp,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    topInset: Dp,
) {
    val titlePaddingStart = 16.dp
    val titlePaddingEnd = 64.dp
    val titleFontScaleStart = 1f
    val titleFontScaleEnd = 0.7f
    var titleHeightPx by remember { mutableFloatStateOf(0f) }
    var titleWidthPx by remember { mutableFloatStateOf(0f) }

    val fraction = (topInset - 74.dp) / (151.dp - 74.dp)
    val statusPadding = lerp(0.dp, 24.dp, fraction.coerceIn(0f, 1f))

    Text(
        text = stringResource(id = R.string.privacy_policy),
        style = MaterialTheme.typography.headlineMedium,
        fontSize = 30.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .graphicsLayer {
                val collapseRange: Float = (largeTopAppBarHeight.toPx() - smallTopAppBarHeight.toPx())
                val collapseFraction: Float = (scroll.value / collapseRange).coerceIn(0f, 1f)

                val scaleXY = lerp(
                    titleFontScaleStart.dp,
                    titleFontScaleEnd.dp,
                    collapseFraction
                )

                val titleExtraStartPadding = titleWidthPx.toDp() * (1 - scaleXY.value) / 2f

                val titleYFirstInterpolatedPoint = lerp(
                    largeTopAppBarHeight - titleHeightPx.toDp() - paddingMedium + statusPadding,
                    largeTopAppBarHeight / 2,
                    collapseFraction
                )

                val titleXFirstInterpolatedPoint = lerp(
                    titlePaddingStart,
                    (titlePaddingEnd - titleExtraStartPadding) * 5 / 4,
                    collapseFraction
                )

                val titleYSecondInterpolatedPoint = lerp(
                    largeTopAppBarHeight / 2,
                    (smallTopAppBarHeight + statusPadding - titleHeightPx.toDp() / 2) - 5.dp,
                    collapseFraction
                )

                val titleXSecondInterpolatedPoint = lerp(
                    (titlePaddingEnd - titleExtraStartPadding) * 5 / 4,
                    titlePaddingEnd - titleExtraStartPadding,
                    collapseFraction
                )

                val titleY = lerp(
                    titleYFirstInterpolatedPoint,
                    titleYSecondInterpolatedPoint,
                    collapseFraction
                )

                val titleX = lerp(
                    titleXFirstInterpolatedPoint,
                    titleXSecondInterpolatedPoint,
                    collapseFraction
                )

                translationY = titleY.toPx()
                translationX = titleX.toPx()
                scaleX = scaleXY.value
                scaleY = scaleXY.value
            }
            .onGloballyPositioned {
                titleHeightPx = it.size.height.toFloat()
                titleWidthPx = it.size.width.toFloat()
            }
    )
}