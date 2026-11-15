package com.anthonyla.paperize

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

// Not a test: puts the status bar in demo mode for the store screenshots.
class DemoMode {
    @Test
    fun enter() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val demo = "am broadcast -a com.android.systemui.demo -e command"
        listOf(
            "settings put global sysui_demo_allowed 1",
            "$demo enter",
            "$demo clock -e hhmm 1200",
            "$demo battery -e level 100 -e plugged false -e powersave false",
            "$demo network -e wifi show -e level 4 -e fully true",
            "$demo network -e mobile hide",
            "$demo notifications -e visible false",
        ).forEach { command ->
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
        }
    }
}
