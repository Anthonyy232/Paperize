package com.anthonyla.paperize.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.presentation.theme.AppIconSizes
import com.anthonyla.paperize.presentation.theme.AppSpacing

@Composable
fun OnboardingLayout(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Delay to allow navigation transition to complete (approx 300ms)
        // preventing "double dip" of animations
        kotlinx.coroutines.delay(300)
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.extraLarge)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) + 
                        slideInVertically(
                            animationSpec = tween(500),
                            initialOffsetY = { 50 }
                        )
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp) // Larger container for the icon background
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppIconSizes.huge),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 100)) + 
                        slideInVertically(
                            animationSpec = tween(500, delayMillis = 100),
                            initialOffsetY = { 50 }
                        )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + 
                        slideInVertically(
                            animationSpec = tween(500, delayMillis = 200),
                            initialOffsetY = { 50 }
                        )
            ) {
                content()
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        // Actions pinned to bottom
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 300)) + 
                    slideInVertically(
                        animationSpec = tween(500, delayMillis = 300),
                        initialOffsetY = { 100 }
                    )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                actions()
            }
        }
    }
}
