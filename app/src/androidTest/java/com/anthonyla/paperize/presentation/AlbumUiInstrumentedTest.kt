package com.anthonyla.paperize.presentation

import android.content.Context
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

class AlbumUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

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
}
