package com.anthonyla.paperize.core.util

import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.content.res.Configuration
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anthonyla.paperize.core.ScalingType
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperUtilInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val files = mutableListOf<File>()

    @After
    fun cleanUp() {
        files.forEach(File::delete)
    }

    @Test
    fun imageDecoderAppliesExifRotation() {
        val image = createJpeg(width = 40, height = 20)
        ExifInterface(image).apply {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString()
            )
            saveAttributes()
        }

        val result = retrieveBitmap(
            context = context,
            wallpaperUri = Uri.fromFile(image),
            width = 20,
            height = 40,
            scaling = ScalingType.STRETCH
        )

        assertNotNull(result)
        assertEquals(20, result?.width)
        assertEquals(40, result?.height)
        result?.recycle()
    }

    @Test
    fun staticFillRetainsWideSourceOverflow() {
        val image = createJpeg(width = 400, height = 200)

        val scrolling = retrieveBitmap(
            context = context,
            wallpaperUri = Uri.fromFile(image),
            width = 100,
            height = 200,
            scaling = ScalingType.FILL,
            preserveSourceOverflow = true
        )
        val exactCanvas = retrieveBitmap(
            context = context,
            wallpaperUri = Uri.fromFile(image),
            width = 100,
            height = 200,
            scaling = ScalingType.FILL,
            preserveSourceOverflow = false
        )

        assertEquals(400, scrolling?.width)
        assertEquals(200, scrolling?.height)
        assertEquals(100, exactCanvas?.width)
        assertEquals(200, exactCanvas?.height)
        scrolling?.recycle()
        exactCanvas?.recycle()
    }

    @Test
    fun everyStaticScalingModeProducesOneExactScreenCanvas() {
        val image = createJpeg(width = 160, height = 90)

        ScalingType.entries.forEach { scaling ->
            val result = retrieveBitmap(
                context = context,
                wallpaperUri = Uri.fromFile(image),
                width = 120,
                height = 200,
                scaling = scaling,
                preserveSourceOverflow = false
            )

            assertNotNull("Expected a bitmap for $scaling", result)
            assertEquals("Unexpected width for $scaling", 120, result?.width)
            assertEquals("Unexpected height for $scaling", 200, result?.height)
            result?.recycle()
        }
    }

    @Test
    fun android17FoldableSizingIncludesInactiveBuiltInPanel() {
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val builtInDisplays = displayManager.getDisplays(
            "android.hardware.display.category.BUILT_IN_DISPLAYS"
        )
        assertTrue("Expected both foldable panels", builtInDisplays.size >= 2)

        val expected = selectLargestDisplayDimensions(
            builtInDisplays.flatMap { display ->
                display.supportedModes.map { mode ->
                    mode.physicalWidth to mode.physicalHeight
                }
            }
        )
        val actual = ScreenMetricsCompat.getScreenSize(context)

        assertNotNull(expected)
        assertEquals(expected?.first, actual.width)
        assertEquals(expected?.second, actual.height)
    }

    @Test
    fun staticRenderSizeDoesNotRotateWithForegroundAppConfiguration() {
        val landscapeConfiguration = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }
        val landscapeContext = context.createConfigurationContext(landscapeConfiguration)
        val naturalPanel = ScreenMetricsCompat.getScreenSize(context)
        val renderSize = getDeviceScreenSize(landscapeContext)

        assertEquals(naturalPanel.width, renderSize.width)
        assertEquals(naturalPanel.height, renderSize.height)
    }

    @Test
    fun disabledStaticEffectsLeaveBitmapUntouched() {
        val source = mutableBitmap(32, 32, Color.rgb(240, 80, 20))

        val result = processBitmap(
            source = source,
            enableDarken = false,
            darkenPercent = 100,
            enableBlur = false,
            blurPercent = 100,
            enableVignette = false,
            vignettePercent = 100,
            enableGrayscale = false,
            grayscalePercent = 100
        )

        assertSame(source, result)
        assertEquals(Color.rgb(240, 80, 20), result.getPixel(16, 16))
        result.recycle()
    }

    @Test
    fun staticDarkenAndGrayscaleEffectsChangePixels() {
        val white = mutableBitmap(32, 32, Color.WHITE)
        val darkened = processBitmap(
            source = white,
            enableDarken = true,
            darkenPercent = 100
        )
        assertTrue(Color.red(darkened.getPixel(16, 16)) <= 2)
        if (darkened !== white) white.recycle()
        darkened.recycle()

        val red = mutableBitmap(32, 32, Color.RED)
        val grayscale = processBitmap(
            source = red,
            enableGrayscale = true,
            grayscalePercent = 100
        )
        val grayPixel = grayscale.getPixel(16, 16)
        assertTrue(kotlin.math.abs(Color.red(grayPixel) - Color.green(grayPixel)) <= 2)
        assertTrue(kotlin.math.abs(Color.green(grayPixel) - Color.blue(grayPixel)) <= 2)
        if (grayscale !== red) red.recycle()
        grayscale.recycle()
    }

    @Test
    fun staticVignetteDarkensEdgesMoreThanCenter() {
        val source = mutableBitmap(96, 96, Color.WHITE)
        val result = processBitmap(
            source = source,
            enableVignette = true,
            vignettePercent = 75
        )

        val center = Color.red(result.getPixel(48, 48))
        val corner = Color.red(result.getPixel(0, 0))
        assertTrue("Expected vignette corner ($corner) below center ($center)", corner < center)
        if (result !== source) source.recycle()
        result.recycle()
    }

    @Test
    fun staticBlurSoftensSharpBoundary() {
        val source = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                source.setPixel(x, y, if (x < 48) Color.BLACK else Color.WHITE)
            }
        }

        val result = processBitmap(
            source = source,
            enableBlur = true,
            blurPercent = 60
        )

        val boundary = Color.red(result.getPixel(48, 48))
        assertTrue("Expected blurred boundary, got $boundary", boundary in 2..253)
        if (result !== source) source.recycle()
        result.recycle()
    }

    private fun mutableBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }

    private fun createJpeg(width: Int, height: Int): File {
        val file = File.createTempFile("paperize-test-", ".jpg", context.cacheDir)
        files += file
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.MAGENTA)
        file.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
        }
        bitmap.recycle()
        return file
    }
}
