package com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import com.anthonyla.paperize.R
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState.ThemeSettings
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.AmoledListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.AnimationListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ContactListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.DarkModeListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.DynamicThemingListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ListSectionTitle
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.NotificationListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.PaperizeListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.PrivacyPolicyListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.ResetListItem
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.components.TranslateListItem
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

private object Links {
    const val TRANSLATE = "https://crowdin.com/project/paperize/invite?h=d8d7a7513d2beb0c96ba9b2a5f85473e2084922"
    const val GITHUB = "https://github.com/Anthonyy232/Paperize"
    const val FDROID = "https://f-droid.org/en/packages/com.anthonyla.paperize/"
    const val IZZY = "https://apt.izzysoft.de/fdroid/index/apk/com.anthonyla.paperize"
}

private object ToolbarConfig {
    @OptIn(ExperimentalMaterial3Api::class)
    val LargeTopAppBarHeight = TopAppBarDefaults.LargeAppBarExpandedHeight
    val StartPadding = 64.dp
    val EndPadding = 16.dp
    const val START = 30
    const val END = 21
    val TitleExtraStartPadding = 32.dp
}

@Composable
private fun calculateToolbarValues(collapseFraction: Float) = with(ToolbarConfig) {
    val firstPaddingInterpolation = lerp((EndPadding * 5 / 4), EndPadding, collapseFraction) + TitleExtraStartPadding
    val secondPaddingInterpolation = lerp(StartPadding, (EndPadding * 5 / 4), collapseFraction)
    val dynamicPaddingStart = lerp(firstPaddingInterpolation, secondPaddingInterpolation, collapseFraction)
    val textSize = (END + (START - END) * collapseFraction).sp
    Pair(dynamicPaddingStart, textSize)
}

@Composable
fun SettingsScreen(
    themeSettings: ThemeSettings,
    onBackClick: () -> Unit,
    onDarkModeClick: (Boolean?) -> Unit,
    onAmoledClick: (Boolean) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onAnimateClick: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onResetClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val context = LocalContext.current
    val topBarState = rememberCollapsingToolbarScaffoldState()
    val (dynamicPaddingStart, textSize) = calculateToolbarValues(topBarState.toolbarState.progress)

    Scaffold {
        CollapsingToolbarScaffold(
            state = topBarState,
            modifier = Modifier.fillMaxSize().padding(it),
            scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
            toolbar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ToolbarConfig.LargeTopAppBarHeight)
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
            SettingsContent(
                themeSettings = themeSettings,
                onDarkModeClick = onDarkModeClick,
                onAmoledClick = onAmoledClick,
                onDynamicThemingClick = onDynamicThemingClick,
                onAnimateClick = onAnimateClick,
                onPrivacyClick = onPrivacyClick,
                onResetClick = onResetClick,
                onContactClick = onContactClick,
                context = context
            )
        }
    }
}

@Composable
private fun SettingsContent(
    themeSettings: ThemeSettings,
    onDarkModeClick: (Boolean?) -> Unit,
    onAmoledClick: (Boolean) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onAnimateClick: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onResetClick: () -> Unit,
    onContactClick: () -> Unit,
    context: android.content.Context
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        AppearanceSection(themeSettings, onDarkModeClick, onAmoledClick, onDynamicThemingClick, onAnimateClick)
        AboutSection(context, onPrivacyClick, onContactClick, onResetClick)
    }
}

@Composable
private fun AppearanceSection(
    themeSettings: ThemeSettings,
    onDarkModeClick: (Boolean?) -> Unit,
    onAmoledClick: (Boolean) -> Unit,
    onDynamicThemingClick: (Boolean) -> Unit,
    onAnimateClick: (Boolean) -> Unit
) {
    ListSectionTitle(stringResource(R.string.appearance))
    Spacer(modifier = Modifier.height(16.dp))
    DarkModeListItem(
        darkMode = themeSettings.darkMode,
        onDarkModeClick = onDarkModeClick
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (themeSettings.darkMode == null || themeSettings.darkMode == true) {
        AmoledListItem(
            amoledMode = themeSettings.amoledTheme,
            onAmoledClick = onAmoledClick
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
    DynamicThemingListItem(
        dynamicTheming = themeSettings.dynamicTheming,
        onDynamicThemingClick = onDynamicThemingClick
    )
    Spacer(modifier = Modifier.height(16.dp))
    AnimationListItem(
        animate = themeSettings.animate,
        onAnimateClick = onAnimateClick
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AboutSection(
    context: android.content.Context,
    onPrivacyClick: () -> Unit,
    onContactClick: () -> Unit,
    onResetClick: () -> Unit
) {
    ListSectionTitle(stringResource(R.string.about))
    Spacer(modifier = Modifier.height(16.dp))
    NotificationListItem {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
    Spacer(modifier = Modifier.height(16.dp))
    TranslateListItem {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.TRANSLATE)))
    }
    Spacer(modifier = Modifier.height(16.dp))
    PrivacyPolicyListItem(onPrivacyClick)
    Spacer(modifier = Modifier.height(16.dp))
    ContactListItem(onContactClick)
    Spacer(modifier = Modifier.height(16.dp))
    PaperizeListItem(
        onGitHubClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.GITHUB))) },
        onFdroidClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.FDROID))) },
        onIzzyOnDroidClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.IZZY))) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    ResetListItem(onResetClick)
    Spacer(modifier = Modifier.height(16.dp))
}