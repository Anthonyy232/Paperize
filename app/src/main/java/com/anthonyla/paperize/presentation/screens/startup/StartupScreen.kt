package com.anthonyla.paperize.presentation.screens.startup

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import com.anthonyla.paperize.presentation.components.OnboardingLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.theme.AppSpacing

/**
 * Startup screen shown on first launch - Enhanced with Material 3 Expressive design
 * Displays privacy policy and asks for agreement
 */
@Composable
fun StartupScreen(
    onAgree: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon = remember {
        val drawable = AppCompatResources.getDrawable(context, R.mipmap.ic_launcher_round)!!
        val size = 192 // Use a fixed high resolution size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }
    
    OnboardingLayout(
        title = stringResource(R.string.app_name),
        modifier = modifier,
        iconContent = {
            Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.welcome),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.small))

                // Cleaner privacy notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.large)
                    ) {
                        Text(
                            text = stringResource(R.string.privacy_notice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.small))

                        Text(
                            text = stringResource(
                                R.string.please_read_and_agree_to_the_following_privacy_notice_to_use_the_app
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        actions = {
            Button(
                onClick = onAgree,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = stringResource(R.string.agree),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = AppSpacing.small)
                )
            }
        }
    )
}
