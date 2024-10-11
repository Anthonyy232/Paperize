package com.anthonyla.paperize.baselineprofile

import android.os.SystemClock
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object PaperizeScenarios {
    fun runStartingScenario(device: UiDevice) {
        device.waitForIdle()
        device.testWelcome()
        device.testNotifications()
        device.testSettings()
        device.testLibrary()
        device.testWallpaper()
    }
}

private fun UiDevice.testWelcome(): Boolean {
    runAction(By.res("paperize:privacy_notice_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:dismiss_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:floating_agree_button")) { click() }
    waitForIdle()
    return true
}

fun UiDevice.testNotifications(): Boolean {
    runAction(By.res("paperize:floating_notification_button")) { click() }
    waitForIdle()
    return findObject(UiSelector().text("Allow")).run {
        try {
            click()
            waitForIdle()
            true
        } catch (_: Exception) {
            waitForIdle()
            false
        }
    } == true
}

fun UiDevice.testSettings(): Boolean {
    runAction(By.res("paperize:home_to_settings_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:settings_column")) {
        setGestureMargins(this)
        scroll(Direction.DOWN, 1f)
        scroll(Direction.DOWN, 1f)
    }
    waitForIdle()
    runAction(By.res("paperize:settings_column")) {
        setGestureMargins(this)
        scroll(Direction.UP, 1f)
        scroll(Direction.UP, 1f)
    }
    waitForIdle()
    runAction(By.res("paperize:settings_to_home_button")) { click() }
    waitForIdle()
    return true
}

fun UiDevice.testWallpaper(): Boolean {
    runAction(By.res("paperize:lock_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:home_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:individual_scheduling_switch")) { click() }
    waitForIdle()
    return true
}

fun UiDevice.testLibrary(): Boolean {
    runAction(By.res("paperize:Library_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:library_floating_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:cancel_album_button")) { click() }
    waitForIdle()
    runAction(By.res("paperize:Wallpaper_button")) { click() }
    waitForIdle()
    return true
}

/** https://github.com/chrisbanes/tivi/blob/main/android-app/common-test/src/main/kotlin/app/tivi/app/test/AppScenarios.kt */
private fun UiDevice.runAction(
    selector: BySelector,
    maxRetries: Int = 6,
    action: UiObject2.() -> Unit,
) {
    waitForObject(selector)

    retry(maxRetries = maxRetries, delay = 1.seconds) {
        waitForIdle()
        requireNotNull(findObject(selector)).action()
    }
}
private fun retry(maxRetries: Int, delay: Duration, block: () -> Unit) {
    repeat(maxRetries) { run ->
        val result = runCatching { block() }
        if (result.isSuccess) { return }
        if (run == maxRetries - 1) {
            result.getOrThrow()
        } else {
            SystemClock.sleep(delay.inWholeMilliseconds)
        }
    }
}
private fun UiDevice.setGestureMargins(uiObject: UiObject2) {
    uiObject.setGestureMargins(
        (displayWidth * 0.1f).toInt(),
        (displayHeight * 0.2f).toInt(),
        (displayWidth * 0.1f).toInt(),
        (displayHeight * 0.2f).toInt(),
    )
}
private fun UiDevice.waitForObject(selector: BySelector, timeout: Duration = 5.seconds): UiObject2 {
    if (wait(Until.hasObject(selector), timeout)) {
        return findObject(selector)
    }
    error("Object with selector [$selector] not found")
}
private fun <R> UiDevice.wait(condition: SearchCondition<R>, timeout: Duration): R {
    return wait(condition, timeout.inWholeMilliseconds)
}
/** https://github.com/chrisbanes/tivi/blob/main/android-app/common-test/src/main/kotlin/app/tivi/app/test/AppScenarios.kt */

