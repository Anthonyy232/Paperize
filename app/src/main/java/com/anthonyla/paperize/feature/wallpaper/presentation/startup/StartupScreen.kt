package com.anthonyla.paperize.feature.wallpaper.presentation.startup

import android.view.textclassifier.TextLinks.TextLink
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.sharp.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.anthonyla.paperize.R

@Composable
fun StartupScreen(
    onAgree: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    var showToS by remember { mutableStateOf(false) }

    Scaffold(
        content = { it
            Column (modifier = Modifier.padding(32.dp)) {
                Spacer(modifier = Modifier.height(120.dp))
                Text(text = stringResource(R.string.welcome), style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxHeight(0.5f)
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.agree),
                    modifier = Modifier.padding(PaddingValues(vertical = 16.dp))
                )
                Text(
                    text = "Please read and agree to the following terms of service and privacy policy to use the app.",
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showToS = true },
                        modifier = Modifier.padding(PaddingValues(vertical = 16.dp)),
                    ) {
                        Text(text = "Terms of Service and Privacy Policy", textAlign = TextAlign.Start)
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(8.dp),
                onClick = onAgree,
                icon = {
                    Icon(
                        imageVector = Icons.Sharp.CheckCircleOutline,
                        contentDescription = stringResource(R.string.agree),
                    )
                },
                text = { Text(text = stringResource(R.string.agree)) },
            )
        }
    )

    if (showToS) {
        AlertDialog(
            onDismissRequest = { showToS = false },
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Terms of Service\nPrivacy Policy",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            text = { Text(text = "ToS, Privacy TBD") },
            confirmButton = {
                Button(onClick = { showToS = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}