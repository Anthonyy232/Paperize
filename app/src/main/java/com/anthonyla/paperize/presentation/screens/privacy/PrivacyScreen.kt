package com.anthonyla.paperize.presentation.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.components.BackTopAppBar
import com.anthonyla.paperize.presentation.common.theme.AppSpacing

@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            BackTopAppBar(title = stringResource(R.string.privacy_policy), onBack = onNavigateBack)
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSpacing.large)
                .verticalScroll(rememberScrollState())
        ) {
            PrivacySection(
                title = stringResource(R.string.privacy_intro_title),
                content = stringResource(R.string.privacy_intro_content, stringResource(R.string.app_name))
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))

            PrivacySection(
                title = stringResource(R.string.privacy_data_collection_title),
                content = stringResource(R.string.privacy_data_collection_content, stringResource(R.string.app_name))
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))

            PrivacySection(
                title = stringResource(R.string.privacy_information_usage_title),
                content = stringResource(R.string.privacy_information_usage_content, stringResource(R.string.app_name))
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))

            PrivacySection(
                title = stringResource(R.string.privacy_data_security_title),
                content = stringResource(R.string.privacy_data_security_content, stringResource(R.string.app_name))
            )
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
