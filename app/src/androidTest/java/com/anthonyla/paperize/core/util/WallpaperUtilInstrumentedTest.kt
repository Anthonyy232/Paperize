package com.anthonyla.paperize.core.util

import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anthonyla.paperize.core.ScalingType
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
