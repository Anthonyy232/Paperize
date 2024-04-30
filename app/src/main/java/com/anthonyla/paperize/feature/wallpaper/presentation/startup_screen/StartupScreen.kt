package com.anthonyla.paperize.feature.wallpaper.presentation.startup_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.onboarding_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showToS by remember { mutableStateOf(false) }
    var seenToS by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(PaddingValues(horizontal = 8.dp))
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
                    text = "Please read and agree to the following privacy policy to use the app.",
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
                        Text(text = "Privacy Policy", textAlign = TextAlign.Start)
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
                                message = "Please read and agree to the privacy policy.",
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
                        text = "Privacy Policy",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            text = { PrivacyPolicyText() },
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
fun PrivacyPolicyText() {
    LazyColumn {
        item {
            SectionText("Introduction", "Welcome to Paperize! This privacy policy outlines how we collect, use, and protect your personal information when you use our mobile application. By using Paperize, you agree to the terms described in this policy.")
        }
        item {
            SectionText("Data Collection", "Notification Access: Paperize requests notification access to provide you with personalized wallpaper recommendations based on your notifications. We do not store any notification content or personally identifiable information (PII) outside of your device.\n\nLocal Files: Paperize accesses files stored on your device to allow you to set them as wallpapers. We do not upload or transfer these files to any external servers.")
        }
        item {
            SectionText("Information Usage", "Personal Data: We do not collect or store any personal data such as names, email addresses, or phone numbers.\n\nUsage Analytics: Paperize does not track your usage behavior or collect analytics data.")
        }
        item {
            SectionText("Data Security", "On-Device Storage: All data used by Paperize remains on your device. We do not transmit or store any data externally.\n\nEncryption: We use encryption protocols to secure communication between the app and your device.")
        }
        item {
            SectionText("Third-Party Services", "Advertisements: Paperize does not display third-party ads.\n\nExternal Links: Our app may contain links to external websites. Please note that we are not responsible for the privacy practices of these sites.")
        }
        item {
            SectionText("Children’s Privacy", "Paperize is not intended for children under the age of 13. We do not knowingly collect any information from children.")
        }
        item {
            SectionText("Changes to this Policy", "We may update this privacy policy from time to time. Any changes will be reflected in the app and on our website.")
        }
        item {
            SectionText("Contact Us", "If you have any questions or concerns about this privacy policy, please contact me at anthonyyla.dev@gmail.com.")
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