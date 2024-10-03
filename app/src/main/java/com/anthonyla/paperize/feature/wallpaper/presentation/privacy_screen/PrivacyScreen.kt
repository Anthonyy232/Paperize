package com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.anthonyla.paperize.R
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBackClick: () -> Unit,
) {
    val topBarState = rememberCollapsingToolbarScaffoldState()

    // Collapsing top bar
    val largeTopAppBarHeight = TopAppBarDefaults.LargeAppBarExpandedHeight
    val startPadding = 64.dp
    val endPadding = 16.dp
    val titleFontScaleStart = 30
    val titleFontScaleEnd = 21
    val titleExtraStartPadding = 32.dp
    val collapseFraction = topBarState.toolbarState.progress
    val firstPaddingInterpolation = lerp((endPadding * 5 / 4), endPadding, collapseFraction) + titleExtraStartPadding
    val secondPaddingInterpolation = lerp(startPadding, (endPadding * 5 / 4), collapseFraction)
    val dynamicPaddingStart = lerp(firstPaddingInterpolation, secondPaddingInterpolation, collapseFraction)
    val textSize = (titleFontScaleEnd + (titleFontScaleStart - titleFontScaleEnd) * collapseFraction).sp

    Scaffold {
        CollapsingToolbarScaffold(
            state = topBarState,
            modifier = Modifier.fillMaxSize().padding(it),
            scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
            toolbar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(largeTopAppBarHeight)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
                        .pin()
                )
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.home_screen)
                    )
                }
                Text(
                    text = stringResource(id = R.string.privacy_policy),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = textSize,
                    modifier = Modifier
                        .road(Alignment.CenterStart, Alignment.BottomStart)
                        .padding(dynamicPaddingStart, 16.dp, 16.dp, 16.dp),
                )
            }
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            ) {
                PrivacyNoticeText()
            }
        }
    }
}

@Composable
private fun PrivacyNoticeText() {
    SectionText(stringResource(R.string.privacy_intro_title), stringResource(R.string.privacy_intro_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_data_collection_title), stringResource(R.string.privacy_data_collection_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_information_usage_title), stringResource(R.string.privacy_information_usage_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_data_security_title), stringResource(R.string.privacy_data_security_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_third_party_services_title), stringResource(R.string.privacy_third_party_services_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_children_privacy_title), stringResource(R.string.privacy_children_privacy_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_changes_notice_title), stringResource(R.string.privacy_changes_notice_content, stringResource(R.string.app_name)))
    SectionText(stringResource(R.string.privacy_contact_us_title), stringResource(R.string.privacy_contact_us_content, "anthonyyla.dev@gmail.com"))
}

@Composable
private fun SectionText(title: String, content: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,

        )
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}