package com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Scrollable privacy screen to display privacy note
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScrollableSettings(
    largeTopAppBarHeightPx: Dp,
    scroll: ScrollState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
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
                }
            )
        }
    ) { it
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(largeTopAppBarHeightPx))
            Spacer(modifier = Modifier.height(16.dp))
            PrivacyNoticeText()
        }
    }
}

@Composable
fun PrivacyNoticeText() {
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
fun SectionText(title: String, content: String) {
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