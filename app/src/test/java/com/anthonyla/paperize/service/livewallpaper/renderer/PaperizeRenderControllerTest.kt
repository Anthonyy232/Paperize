package com.anthonyla.paperize.service.livewallpaper.renderer

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaperizeRenderControllerTest {
    @Test fun `reload during selection discards the stale loader and selects again`() = runTest {
        val uploads = mutableListOf<Pair<ImageLoader, Boolean>>()
        val first = CompletableDeferred<ImageLoader>()
        val stale = mockk<ContentUriImageLoader>()
        val current = mockk<ContentUriImageLoader>()
        var selections = 0
        val controller = PaperizeRenderController({ loader, skip -> uploads.add(loader to skip) }, backgroundScope) {
            if (selections++ == 0) first.await() else current
        }
        controller.visible = true
        controller.reloadCurrentArtwork()
        runCurrent()
        controller.reloadCurrentArtwork()
        first.complete(stale)
        runCurrent()

        assertEquals(listOf(current to false), uploads)
    }

    @Test fun `reloads requested while hidden select once when visible`() = runTest {
        val uploads = mutableListOf<Pair<ImageLoader, Boolean>>()
        val loader = mockk<ContentUriImageLoader>()
        var selections = 0
        val controller = PaperizeRenderController({ loader, skip -> uploads.add(loader to skip) }, backgroundScope) {
            selections++
            loader
        }
        controller.reloadCurrentArtwork()
        controller.reloadCurrentArtwork()
        runCurrent()
        assertTrue(uploads.isEmpty())

        controller.visible = true
        runCurrent()
        assertEquals(listOf(loader to false), uploads)
        assertEquals(1, selections)
    }

    @Test fun `selection with nothing readable is retried until it succeeds`() = runTest {
        val uploads = mutableListOf<Pair<ImageLoader, Boolean>>()
        val loader = mockk<ContentUriImageLoader>()
        var selections = 0
        val controller = PaperizeRenderController({ loader, skip -> uploads.add(loader to skip) }, backgroundScope) {
            if (selections++ < 2) null else loader
        }
        controller.visible = true
        controller.reloadCurrentArtwork()
        runCurrent()
        assertEquals(1, selections)

        advanceTimeBy(1_001)
        assertEquals(2, selections)
        advanceTimeBy(1_001)
        assertEquals(2, selections)
        advanceTimeBy(1_000)
        assertEquals(listOf(loader to false), uploads)

        advanceTimeBy(120_000)
        assertEquals(3, selections)
    }

    @Test fun `retry while hidden waits until visible`() = runTest {
        val uploads = mutableListOf<Pair<ImageLoader, Boolean>>()
        val loader = mockk<ContentUriImageLoader>()
        var selections = 0
        val controller = PaperizeRenderController({ loader, skip -> uploads.add(loader to skip) }, backgroundScope) {
            if (selections++ == 0) null else loader
        }
        controller.visible = true
        controller.reloadCurrentArtwork()
        runCurrent()
        controller.visible = false
        advanceTimeBy(60_000)
        assertEquals(1, selections)

        controller.visible = true
        runCurrent()
        assertEquals(listOf(loader to false), uploads)
    }
}
