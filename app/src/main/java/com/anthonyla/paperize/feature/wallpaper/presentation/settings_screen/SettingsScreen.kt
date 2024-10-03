package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.AmoledListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.AnimationListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ContactListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.DarkModeListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.DynamicThemingListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.LicenseListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ListSectionTitle
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.PaperizeListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.PrivacyPolicyListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ResetListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.TranslateListItem
import kotlinx.coroutines.flow.StateFlow
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: StateFlow<SettingsState>,
    onBackClick: () -> Unit,
    onDarkModeClick: (Boolean?) -> Unit,
    onAmoledClick: (Boolean) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onAnimateClick: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onResetClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val topBarState = rememberCollapsingToolbarScaffoldState()
    val state = settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val translateLink = "https://crowdin.com/project/paperize/invite?h=d8d7a7513d2beb0c96ba9b2a5f85473e2084922"
    val githubLink = "https://github.com/Anthonyy232/Paperize"
    val fdroidLink = "https://f-droid.org/en/packages/com.anthonyla.paperize/"
    val izzyOnDroidLink = "https://apt.izzysoft.de/fdroid/index/apk/com.anthonyla.paperize"

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
                    text = stringResource(id = R.string.settings_screen),
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
                ListSectionTitle(stringResource(R.string.appearance))
                Spacer(modifier = Modifier.height(16.dp))
                DarkModeListItem(
                    darkMode = state.value.darkMode,
                    onDarkModeClick = { onDarkModeClick(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (state.value.darkMode == null || state.value.darkMode == true) {
                    AmoledListItem(
                        amoledMode = state.value.amoledTheme,
                        onAmoledClick = { onAmoledClick(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                DynamicThemingListItem(
                    dynamicTheming = state.value.dynamicTheming,
                    onDynamicThemingClick = { onDynamicThemingClick(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AnimationListItem(
                    animate = state.value.animate,
                    onAnimateClick = { onAnimateClick(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ListSectionTitle(stringResource(R.string.about))
                Spacer(modifier = Modifier.height(16.dp))
                TranslateListItem(
                    onClick = {
                        val openURL = Intent(Intent.ACTION_VIEW)
                        openURL.data = Uri.parse(translateLink)
                        context.startActivity(openURL)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrivacyPolicyListItem (onPrivacyPolicyClick = onPrivacyClick)
                Spacer(modifier = Modifier.height(16.dp))
                LicenseListItem(onLicenseClick = onLicenseClick)
                Spacer(modifier = Modifier.height(16.dp))
                ContactListItem(onContactClick = onContactClick)
                Spacer(modifier = Modifier.height(16.dp))
                PaperizeListItem(
                    onGitHubClick = {
                        val openURL = Intent(Intent.ACTION_VIEW)
                        openURL.data = Uri.parse(githubLink)
                        context.startActivity(openURL)
                    },
                    onFdroidClick = {
                        val openURL = Intent(Intent.ACTION_VIEW)
                        openURL.data = Uri.parse(fdroidLink)
                        context.startActivity(openURL)
                    },
                    onIzzyOnDroidClick = {
                        val openURL = Intent(Intent.ACTION_VIEW)
                        openURL.data = Uri.parse(izzyOnDroidLink)
                        context.startActivity(openURL)
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                ResetListItem(onResetClick = onResetClick)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}