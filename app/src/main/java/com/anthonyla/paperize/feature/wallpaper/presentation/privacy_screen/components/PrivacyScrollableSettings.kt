package com.anthonyla.paperize.feature.wallpaper.presentation.privacy_screen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scrollable privacy screen to display privacy policy
 */
@Composable
fun PrivacyScrollableSettings(
    largeTopAppBarHeightPx: Dp,
    scroll: ScrollState,
) {
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
        PrivacyPolicyText()
    }
}

@Composable
fun PrivacyPolicyText() {
    SectionText("Introduction", "Welcome to Paperize! This privacy policy outlines how we collect, use, and protect your personal information when you use our mobile application. By using Paperize, you agree to the terms described in this policy.")
    SectionText("Data Collection", "Notification Access: Paperize requests notification access to provide you with personalized wallpaper recommendations based on your notifications. We do not store any notification content or personally identifiable information (PII) outside of your device.\n\nLocal Files: Paperize accesses files stored on your device to allow you to set them as wallpapers. We do not upload or transfer these files to any external servers.")
    SectionText("Information Usage", "Personal Data: We do not collect or store any personal data such as names, email addresses, or phone numbers.\n\nUsage Analytics: Paperize does not track your usage behavior or collect analytics data.")
    SectionText("Data Security", "On-Device Storage: All data used by Paperize remains on your device. We do not transmit or store any data externally.")
    SectionText("Third-Party Services", "Advertisements: Paperize does not display third-party ads.\n\nExternal Links: Our app may contain links to external websites. Please note that we are not responsible for the privacy practices of these sites.")
    SectionText("Children’s Privacy", "Paperize is not intended for children under the age of 13. We do not knowingly collect any information from children.")
    SectionText("Changes to this Policy", "We may update this privacy policy from time to time. Any changes will be reflected in the app and on our website.")
    SectionText("Contact Us", "If you have any questions or concerns about this privacy policy, please contact me at anthonyyla.dev@gmail.com.")
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