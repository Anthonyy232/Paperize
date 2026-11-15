package com.anthonyla.paperize.presentation.screens.startup

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.graphics.drawable.toBitmap
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.components.OnboardingButton
import com.anthonyla.paperize.presentation.common.components.OnboardingLayout
import com.anthonyla.paperize.presentation.common.components.SettingText
import com.anthonyla.paperize.presentation.common.components.SettingsGroup
import com.anthonyla.paperize.presentation.common.components.SettingsRow
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

@Composable
fun StartupScreen(
    onAgree: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        AppCompatResources.getDrawable(context, R.mipmap.ic_launcher_round)
            ?.toBitmap(width = 384, height = 384)
            ?.asImageBitmap()
    }

    OnboardingLayout(
        title = stringResource(R.string.app_name),
        modifier = modifier,
        iconContent = {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.extraLarge)
            ) {
                Text(
                    text = stringResource(R.string.welcome),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsGroup {
                    SettingsRow(onClick = onPrivacyClick) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingText(
                                title = stringResource(R.string.privacy_notice),
                                description = stringResource(
                                    R.string.please_read_and_agree_to_the_following_privacy_notice_to_use_the_app
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        actions = {
            OnboardingButton(text = stringResource(R.string.agree), onClick = onAgree)
        }
    )
}
