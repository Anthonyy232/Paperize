package com.anthonyla.paperize.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import com.anthonyla.paperize.domain.model.AlbumSummary
import com.anthonyla.paperize.presentation.screens.library.components.AlbumItem
import java.io.File
import androidx.compose.runtime.mutableIntStateOf
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.presentation.screens.wallpaper.components.TimeIntervalPicker
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.anthonyla.paperize.R
import com.anthonyla.paperize.presentation.common.theme.PaperizeTheme
import com.anthonyla.paperize.presentation.screens.album_view.components.SortBottomSheet
import com.anthonyla.paperize.presentation.screens.album_view.components.SortOption
import com.anthonyla.paperize.presentation.screens.album_view.components.WallpaperItem
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import com.anthonyla.paperize.core.Result
import com.anthonyla.paperize.domain.model.Album
import com.anthonyla.paperize.presentation.screens.library.LibraryScreen
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.semantics.SemanticsActions
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.AppSettings
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.model.WallpaperEffects
import com.anthonyla.paperize.presentation.screens.wallpaper.WallpaperScreen

class AlbumUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun effectSlidersRouteDraftChangesToEnabledScreensAndLiveMode() {
        val settings = mutableStateOf(ScheduleSettings(
            homeEnabled = true, lockEnabled = true,
            homeEffects = WallpaperEffects(enableBlur = true, blurPercentage = 45),
            lockEffects = WallpaperEffects(enableBlur = true, blurPercentage = 65),
            liveEffects = WallpaperEffects(enableBlur = true, blurPercentage = 80)
        ))
        val mode = mutableStateOf(WallpaperMode.STATIC)
        var latest = settings.value
        compose.setContent {
            PaperizeTheme(false, false) {
                WallpaperScreen(
                    albums = emptyList(), persistedScheduleSettings = settings.value,
                    appSettings = AppSettings(), wallpaperMode = mode.value,
                    onToggleChanger = {}, onSelectHomeAlbum = {}, onSelectLockAlbum = {}, onSelectLiveAlbum = {},
                    onUpdateScheduleSettings = { latest = it },
                    onUpdateScheduleSettingsDebounced = { latest = it },
                    onChangeWallpaperNow = {}, homeWallpaperUri = null, lockWallpaperUri = null
                )
            }
        }
        fun slide(index: Int, percentage: Float) {
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[index]
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(percentage) }
        }
        slide(0, 20f)
        slide(1, 75f)
        compose.runOnIdle {
            assertEquals(20, latest.homeEffects.blurPercentage)
            assertEquals(75, latest.lockEffects.blurPercentage)
            settings.value = latest.copy(homeEnabled = false)
        }
        slide(0, 55f)
        compose.runOnIdle {
            assertEquals(20, latest.homeEffects.blurPercentage)
            assertEquals(55, latest.lockEffects.blurPercentage)
            settings.value = latest
            mode.value = WallpaperMode.LIVE
        }
        slide(0, 40f)
        compose.runOnIdle {
            assertEquals(20, latest.homeEffects.blurPercentage)
            assertEquals(55, latest.lockEffects.blurPercentage)
            assertEquals(40, latest.liveEffects.blurPercentage)
        }
    }

    @Test fun failedAlbumCreationKeepsNameAndAllowsRetryAfterPendingSave() {
        val pending = CompletableDeferred<Result<Album>>()
        var attempts = 0
        compose.setContent {
            PaperizeTheme(false, false) {
                LibraryScreen(emptyList(), {}, { name ->
                    assertEquals("Mountains", name)
                    attempts++
                    if (attempts == 1) pending.await()
                    else Result.Success(Album.empty(id = "album", name = name))
                })
            }
        }
        compose.onNodeWithContentDescription(context.getString(R.string.add_album)).performClick()
        compose.onNodeWithText(context.getString(R.string.album_name)).performTextInput("Mountains")
        compose.onNodeWithText(context.getString(R.string.save)).performClick().assertIsNotEnabled()
        compose.runOnIdle { pending.complete(Result.Error(IllegalStateException("Storage unavailable"))) }
        compose.onNodeWithText(context.getString(R.string.album_create_failed)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.album_name)).assertTextContains("Mountains")
        compose.onNodeWithText(context.getString(R.string.save)).performClick()
        compose.onNodeWithText(context.getString(R.string.album_name)).assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }

    @Test fun sortSheetExposesCurrentSelectionAndAppliesNewChoice() {
        var chosen: SortOption? = null
        var dismissed = false
        compose.setContent {
            PaperizeTheme(false, false) {
                SortBottomSheet(SortOption.DATE_ADDED_DESC, { chosen = it }, { dismissed = true })
            }
        }
        compose.onNodeWithText(context.getString(R.string.sort_date_added_desc)).assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.sort_name_asc)).performClick()
        compose.runOnIdle { assertEquals(SortOption.NAME_ASC, chosen); assertTrue(dismissed) }
    }

    @Test fun wallpaperThumbnailsExposeNameAndSelection() {
        compose.setContent {
            PaperizeTheme(false, false) {
                WallpaperItem("content://test/image", "Mountains.jpg", true, true, {}, {}, Modifier.size(160.dp))
            }
        }
        compose.onNodeWithContentDescription("Mountains.jpg")
            .assertIsSelected().assertHasClickAction()
    }
    @Test fun albumCoverLoadsThroughCoilAndFallsBackForMissingFiles() {
        val file = File.createTempFile("album-cover", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = mutableStateOf<String?>(file.toURI().toString())
        try {
            compose.setContent {
                PaperizeTheme(false, false) {
                    AlbumItem(AlbumSummary.empty("album").copy(name = "Cover", coverUri = uri.value), {}, Modifier.size(180.dp))
                }
            }
            fun centerColor(): Int {
                val pixels = compose.onNodeWithContentDescription("Cover").captureToImage().toPixelMap()
                return pixels[pixels.width / 2, pixels.height / 2].toArgb()
            }
            compose.waitUntil(5_000) { centerColor() == Color.RED }
            compose.runOnIdle { uri.value = File(context.cacheDir, "missing-cover.png").toURI().toString() }
            compose.waitUntil(5_000) { centerColor() != Color.RED }
            compose.onNodeWithContentDescription("Cover").assertIsDisplayed()
        } finally {
            file.delete()
        }
    }

    @Test fun intervalEditsDebounceAndExternalValuesCancelPendingEdits() {
        val minutes = mutableIntStateOf(60)
        val changes = mutableListOf<Int>()
        compose.setContent {
            PaperizeTheme(false, false) {
                TimeIntervalPicker("Interval", minutes.intValue, { changes.add(it) })
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(Constants.DEBOUNCE_DELAY_MS + 50)
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        compose.onNodeWithText(context.getString(R.string.hours_txt)).performTextReplacement("0")
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText(context.getString(R.string.mins)).performTextReplacement("1")
        compose.mainClock.advanceTimeBy(Constants.DEBOUNCE_DELAY_MS + 50)
        compose.runOnIdle { assertEquals(listOf(Constants.MIN_INTERVAL_MINUTES), changes) }

        compose.onNodeWithText(context.getString(R.string.mins)).performTextReplacement("30")
        compose.runOnIdle { minutes.intValue = 120 }
        compose.mainClock.advanceTimeBy(Constants.DEBOUNCE_DELAY_MS + 50)
        compose.runOnIdle { assertEquals(1, changes.size) }
        compose.onNodeWithText(context.getString(R.string.hours_txt)).assertTextContains("2")
    }

}
