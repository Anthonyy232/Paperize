package com.anthonyla.paperize.feature.wallpaper.presentation.startup_screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.sharp.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.anthonyla.paperize.R
import kotlinx.coroutines.launch

// Inspired by https://github.com/Ashinch/ReadYou
@Composable
fun StartupScreen(
    onAgree: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showToS by remember { mutableStateOf(false) }
    var seenToS by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity
    BackHandler {
        activity?.moveTaskToBack(true)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(PaddingValues(horizontal = 8.dp)),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            ) },
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
                    text = "Please read and agree to the following privacy notice to use the app.",
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                showToS = true
                            } },
                        modifier = Modifier.padding(PaddingValues(vertical = 16.dp)),
                    ) {
                        Text(text = "Privacy Notice", textAlign = TextAlign.Start)
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    if (seenToS) {
                        onAgree()
                    } else {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Please read and agree to the privacy notice.",
                                actionLabel = "View",
                                duration = SnackbarDuration.Short,
                            )
                            when (result) {
                                SnackbarResult.Dismissed -> {}
                                SnackbarResult.ActionPerformed -> {
                                    showToS = true
                                }
                            }
                        }
                    }
                },
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
        seenToS = true
        AlertDialog(
            modifier = Modifier.fillMaxHeight(0.8f),
            onDismissRequest = { showToS = false },
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Privacy Notice",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            text = { PrivacyNoticeText() },
            confirmButton = {
                Button(
                    onClick = { showToS = false },
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun PrivacyNoticeText() {
    LazyColumn {
        item {
            SectionText("Hello!", "Welcome to Paperize! We respect your privacy and protect your personal info. By using our app, you're okay with our privacy policy.")
        }
        item {
            SectionText("What We Collect", "We ask for notification access to personalize your wallpapers. We don't store or share your notifications. We also access your local files for wallpapers, but don't upload or transfer them.")
        }
        item {
            SectionText("How We Use Info", "We don't collect personal data or track your usage. All your data stays on your device.")
        }
        item {
            SectionText("Security", "Your data is safe with us. It stays on your device at all times.")
        }
        item {
            SectionText("Third-Party Services", "No third-party ads here! We might have external links, but we're not responsible for their privacy practices.")
        }
        item {
            SectionText("For Kids", "Paperize isn't for kids under 13. We don't knowingly collect their info.")
        }
        item {
            SectionText("Policy Updates", "We might update this policy sometimes. You'll see any changes in the app and on GitHub.")
        }
        item {
            SectionText("Contact Us", "Got questions or concerns about our privacy policy? Email us at anthonyyla.dev@gmail.com.")
        }
    }
}

@Composable
fun SectionText(title: String, content: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}