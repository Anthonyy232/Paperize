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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anthonyla.paperize.R

/**
 * Scrollable privacy screen to display privacy note
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
        PrivacyNoticeText()
    }
}

@Composable
fun PrivacyNoticeText() {
    SectionText(stringResource(R.string.introduction), stringResource(R.string.welcome_t) + " Paperize! " + stringResource(R.string.this_privacy_notice_outlines_how_we_collect_use_and_protect_your_personal_information_when_you_use_our_mobile_application_by_using) + " Paperize, " + stringResource(R.string.you_agree_to_the_terms_described_in_this_notice))
    SectionText(stringResource(R.string.data_collection),
        stringResource(R.string.notification_access) + " Paperize " + stringResource(R.string.requests_notification_access_to_provide_you_with_personalized_wallpaper_recommendations_based_on_your_notifications_we_do_not_store_any_notification_content_or_personally_identifiable_information_pii_outside_of_your_device_local_files_we_accesses_files_stored_on_your_device_to_allow_you_to_set_them_as_wallpapers_we_do_not_upload_or_transfer_these_files_to_any_external_servers))
    SectionText(stringResource(R.string.information_usage),
        stringResource(R.string.personal_data_we_do_not_collect_or_store_any_personal_data_such_as_names_email_addresses_or_phone_numbers_usage_analytics_we_do_not_track_your_usage_behavior_or_collect_analytics_data))
    SectionText(stringResource(R.string.data_security),
        stringResource(R.string.on_device_storage_all_data_used_by) + " Paperize " + stringResource(R.string.remains_on_your_device_we_do_not_transmit_or_store_any_data_externally))
    SectionText(stringResource(R.string.third_party_services),
        stringResource(R.string.advertisements) + " Paperize " + stringResource(R.string.does_not_display_third_party_ads_external_links_our_app_may_contain_links_to_external_websites_please_note_that_we_are_not_responsible_for_the_privacy_practices_of_these_sites))
    SectionText(stringResource(R.string.children_s_privacy), "Paperize " + stringResource(R.string.is_not_intended_for_children_under_the_age_of_13_we_do_not_knowingly_collect_any_information_from_children))
    SectionText(stringResource(R.string.changes_to_this_notice),
        stringResource(R.string.we_may_update_this_privacy_notice_from_time_to_time_any_changes_will_be_reflected_in_the_app_and_on_our_website))
    SectionText(stringResource(R.string.contact_us), stringResource(R.string.if_you_have_any_questions_or_concerns_about_this_privacy_notice_please_contact_me_at) + " anthonyyla.dev@gmail.com.")
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