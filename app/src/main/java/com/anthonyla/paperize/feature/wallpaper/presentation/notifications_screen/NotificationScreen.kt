package com.anthonyla.paperize.feature.wallpaper.presentation.notifications_screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.anthonyla.paperize.R
import kotlinx.coroutines.launch

@Composable
fun NotificationScreen(
    onAgree: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.notification_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val askPermission = rememberSaveable { mutableStateOf(false) }
    val askCount = rememberSaveable { mutableIntStateOf(0) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onAgree()
        } else {
            if (++askCount.intValue >= 2) {
                snackbarHostState.currentSnackbarData?.dismiss()
                onAgree()
            }
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.notification_permission_is_required_for_this_app_to_work_properly),
                    actionLabel = context.getString(R.string.allow),
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> { askPermission.value = true }
                }
            }
        }
    }

    fun handleFabClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) -> {
                    onAgree()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else { onAgree() }
    }

    LaunchedEffect (askPermission.value) {
        if (askPermission.value) {
            handleFabClick()
            askPermission.value = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(PaddingValues(horizontal = 8.dp, vertical = 8.dp)),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            ) },
        content = { it
            Column (modifier = Modifier.padding(32.dp)) {
                Spacer(modifier = Modifier.height(120.dp))
                Text(text = "Notifications", style = MaterialTheme.typography.headlineLarge)
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
                    text = stringResource(R.string.notification_info_message),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(8.dp),
                onClick = { askPermission.value = true },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowCircleRight,
                        contentDescription = stringResource(R.string.continue_button),
                    )
                },
                text = { Text(text = stringResource(R.string.continue_button)) },
            )
        }
    )
}

