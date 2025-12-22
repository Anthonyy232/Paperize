package com.anthonyla.paperize.service.livewallpaper.gl

import android.os.Build
import android.util.Log
import kotlin.math.min

/**
 * Utility class for handling GL compatibility across different OEMs and GPU vendors.
 * 
 * Provides OEM-specific workarounds for:
 * - Texture size limits (some OEMs misreport GL_MAX_TEXTURE_SIZE)
 * - EGL configuration fallbacks (different GPUs need different configs)
 * - Parallax support detection (some launchers don't report offsets)
 */
object GLCompatibility {

    private const val TAG = "GLCompatibility"

    // Known problematic manufacturers for various GL issues
    private val LOW_END_GPU_MANUFACTURERS = setOf(
        "MEDIATEK",  // Mali-T/G series often have texture issues
        "ALLWINNER", // Low-end tablets
        "ROCKCHIP",  // Budget tablets
        "SPREADTRUM" // Budget phones
    )

    // Manufacturers with known parallax/offset issues
    private val PARALLAX_ISSUE_MANUFACTURERS = setOf(
        "SAMSUNG",   // One UI often doesn't report offsets properly
        "XIAOMI",    // MIUI Launcher has inconsistent offset reporting
        "HUAWEI",    // EMUI has similar issues
        "OPPO",      // ColorOS launcher issues
        "VIVO",      // OriginOS/FuntouchOS issues
        "REALME",    // RealmeUI uses OPPO's base
        "ONEPLUS"    // OxygenOS (older versions)
    )

    // Texture size limits by device tier
    private const val TEXTURE_SIZE_LOW_END = 2048
    private const val TEXTURE_SIZE_MID_RANGE = 4096

    /**
     * Get the device manufacturer in uppercase for comparison.
     */
    fun getManufacturer(): String = Build.MANUFACTURER.uppercase()


    /**
     * Check if the device likely has a low-end GPU that may have texture issues.
     */
    fun isLowEndGPU(): Boolean {
        val manufacturer = getManufacturer()
        val isLowEnd = LOW_END_GPU_MANUFACTURERS.any { manufacturer.contains(it) }
        
        // Also check for low RAM as a heuristic for low-end devices
        // Note: This is a simple heuristic; actual RAM check would require context
        
        return isLowEnd
    }

    /**
     * Get a safe maximum texture size based on device capabilities.
     * This is more conservative than the GL-reported value to avoid OOM on problematic devices.
     * 
     * @param glReportedMax The maximum texture size reported by GL_MAX_TEXTURE_SIZE
     * @return A safe texture size to use
     */
    fun getSafeMaxTextureSize(glReportedMax: Int = 0): Int {
        val manufacturer = getManufacturer()
        
        // Use conservative limits for known problematic manufacturers
        val safeLimit = when {
            isLowEndGPU() -> {
                Log.d(TAG, "Low-end GPU detected ($manufacturer), using conservative texture size")
                TEXTURE_SIZE_LOW_END
            }
            else -> TEXTURE_SIZE_MID_RANGE
        }

        // If we have a GL-reported max, cap at that value
        return if (glReportedMax > 0) {
            min(safeLimit, glReportedMax)
        } else {
            safeLimit
        }
    }

    /**
     * Check if the device's default launcher likely supports reliable offset notifications
     * for parallax scrolling.
     * 
     * Many OEM launchers don't properly call onOffsetsChanged(), making parallax
     * scrolling impossible or erratic.
     * 
     * @return true if offsets are likely to work reliably
     */
    fun supportsReliableOffsets(): Boolean {
        val manufacturer = getManufacturer()
        val hasIssues = PARALLAX_ISSUE_MANUFACTURERS.any { manufacturer.contains(it) }
        
        if (hasIssues) {
            Log.d(TAG, "Manufacturer $manufacturer may have parallax/offset issues with stock launcher")
        }
        
        // Return true anyway - we don't disable parallax outright, but we detect it
        // The caller can use this to show a toast or fallback gracefully
        return !hasIssues
    }

    /**
     * Check if we should warn the user about potential parallax issues.
     * Only returns true if parallax would likely not work on this device.
     */
    fun shouldWarnAboutParallax(): Boolean {
        return !supportsReliableOffsets()
    }


    /**
     * Log device information for debugging.
     */
    fun logDeviceInfo() {
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.i(TAG, "Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        Log.i(TAG, "Board: ${Build.BOARD}, Hardware: ${Build.HARDWARE}")
        Log.i(TAG, "Low-end GPU: ${isLowEndGPU()}")
        Log.i(TAG, "Supports reliable offsets: ${supportsReliableOffsets()}")
    }

}
