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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = context.getString(R.string.welcome_animation) },
                    safeMode = true,
                    enableMergePaths = true
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.agree),
                    modifier = Modifier.padding(PaddingValues(vertical = 16.dp))
                )
                Text(
                    text = stringResource(R.string.please_read_and_agree_to_the_following_privacy_notice_to_use_the_app),
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
                        Text(text = stringResource(R.string.privacy_notice), textAlign = TextAlign.Start)
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
                                message = context.getString(R.string.please_read_and_agree_to_the_privacy_notice),
                                actionLabel = context.getString(R.string.view),
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
                        text = stringResource(R.string.privacy_notice),
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
                    Text(stringResource(R.string.dismiss))
                }
            }
        )
    }
}

@Composable
fun PrivacyNoticeText() {
    LazyColumn {
        item {
            SectionText(stringResource(R.string.hello), stringResource(R.string.welcome_to) + "Paperize! " + stringResource(
                R.string.we_respect_your_privacy_and_protect_your_personal_info_by_using_our_app_you_re_okay_with_our_privacy_policy
            ) )
        }
        item {
            SectionText(stringResource(R.string.what_we_collect),
                stringResource(R.string.we_ask_for_notification_access_to_personalize_your_wallpapers_we_don_t_store_or_share_your_notifications_we_also_access_your_local_files_for_wallpapers_but_don_t_upload_or_transfer_them))
        }
        item {
            SectionText(stringResource(R.string.how_we_use_info),
                stringResource(R.string.we_don_t_collect_personal_data_or_track_your_usage_all_your_data_stays_on_your_device))
        }
        item {
            SectionText(stringResource(R.string.security),
                stringResource(R.string.your_data_is_safe_with_us_it_stays_on_your_device_at_all_times))
        }
        item {
            SectionText(stringResource(R.string.third_party_services),
                stringResource(R.string.no_third_party_ads_here_we_might_have_external_links_but_we_re_not_responsible_for_their_privacy_practices))
        }
        item {
            SectionText(stringResource(R.string.for_kids), "Paperize " + stringResource(R.string.isn_t_for_kids_under_13_we_don_t_knowingly_collect_their_info))
        }
        item {
            SectionText(stringResource(R.string.policy_updates),
                stringResource(R.string.we_might_update_this_policy_sometimes_you_ll_see_any_changes_in_the_app_and_on_github))
        }
        item {
            SectionText(stringResource(R.string.contact_us), stringResource(R.string.got_questions_or_concerns_about_our_privacy_policy_email_us_at) + "anthonyyla.dev@gmail.com.")
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