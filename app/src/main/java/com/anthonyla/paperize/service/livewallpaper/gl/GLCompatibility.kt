package com.anthonyla.paperize.service.livewallpaper.gl

import android.os.Build
import android.util.Log
import kotlin.math.min

/**
 * Utility class for handling GL compatibility across different OEMs and GPU vendors.
 * 
 * Provides OEM-specific workarounds for:
 * - Texture size limits (some OEMs misreport GL_MAX_TEXTURE_SIZE)
 * - Parallax support detection (some launchers don't report offsets)
 */
object GLCompatibility {

    private const val TAG = "GLCompatibility"

    private val LOW_END_GPU_MANUFACTURERS = setOf(
        "MEDIATEK",  // Mali-T/G series often have texture issues
        "ALLWINNER", // Low-end tablets
        "ROCKCHIP",  // Budget tablets
        "SPREADTRUM" // Budget phones
    )

    private val PARALLAX_ISSUE_MANUFACTURERS = setOf(
        "SAMSUNG",   // One UI often doesn't report offsets properly
        "XIAOMI",    // MIUI Launcher has inconsistent offset reporting
        "HUAWEI",    // EMUI has similar issues
        "OPPO",      // ColorOS launcher issues
        "VIVO",      // OriginOS/FuntouchOS issues
        "REALME",    // RealmeUI uses OPPO's base
        "ONEPLUS"    // OxygenOS (older versions)
    )

    private const val TEXTURE_SIZE_LOW_END = 2048
    private const val TEXTURE_SIZE_MID_RANGE = 4096

    private fun getManufacturer(): String = Build.MANUFACTURER.uppercase()

    private fun isLowEndGPU(): Boolean {
        val manufacturer = getManufacturer()
        return LOW_END_GPU_MANUFACTURERS.any { manufacturer.contains(it) }
    }

    fun getSafeMaxTextureSize(glReportedMax: Int = 0): Int {
        val manufacturer = getManufacturer()
        
        val safeLimit = if (isLowEndGPU()) {
            Log.d(TAG, "Low-end GPU detected ($manufacturer), using conservative texture size")
            TEXTURE_SIZE_LOW_END
        } else {
            TEXTURE_SIZE_MID_RANGE
        }

        return if (glReportedMax > 0) {
            min(safeLimit, glReportedMax)
        } else {
            safeLimit
        }
    }

    fun shouldWarnAboutParallax(): Boolean {
        val manufacturer = getManufacturer()
        val hasIssues = PARALLAX_ISSUE_MANUFACTURERS.any { manufacturer.contains(it) }
        
        if (hasIssues) {
            Log.d(TAG, "Manufacturer $manufacturer may have parallax/offset issues with stock launcher")
        }
        
        return hasIssues
    }

}
