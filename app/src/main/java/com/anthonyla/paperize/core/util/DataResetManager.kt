package com.anthonyla.paperize.core.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.anthonyla.paperize.core.constants.Constants

/**
 * Manages one-time data reset for major version upgrades.
 *
 * Uses a separate preferences file (version_prefs) to track reset status,
 * ensuring the flag survives the data wipe itself.
 */
object DataResetManager {
    private const val TAG = "DataResetManager"

    private const val VERSION_PREFS_NAME = "version_prefs"
    private const val KEY_LAST_RESET_VERSION = "last_reset_version"

    /**
     * Target reset version - increment this for future major version upgrades
     * that require a complete data wipe.
     *
     * Version 4 = v4.0.0 release (complete app rewrite)
     */
    private const val TARGET_RESET_VERSION = 4

    /**
     * Performs a complete data reset if needed for the current app version.
     *
     * This should be called at the very start of Application.onCreate(),
     * before any other initialization occurs.
     *
     * @return true if a reset was performed, false otherwise
     */
    fun performResetIfNeeded(context: Context): Boolean {
        val versionPrefs = context.getSharedPreferences(VERSION_PREFS_NAME, Context.MODE_PRIVATE)
        val lastResetVersion = versionPrefs.getInt(KEY_LAST_RESET_VERSION, 0)

        if (lastResetVersion >= TARGET_RESET_VERSION) {
            Log.d(TAG, "No reset needed (last=$lastResetVersion, target=$TARGET_RESET_VERSION)")
            return false
        }

        Log.w(TAG, "Performing data reset: upgrading from version $lastResetVersion to $TARGET_RESET_VERSION")

        return try {
            context.deleteDatabase(Constants.DATABASE_NAME)
            context.preferencesDataStoreFile(Constants.PREFERENCES_NAME).delete()
            Log.i(TAG, "Data reset completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during data reset", e)
            false
        } finally {
            // Do not repeat a destructive upgrade attempt on the next launch after a failure.
            versionPrefs.edit { putInt(KEY_LAST_RESET_VERSION, TARGET_RESET_VERSION) }
        }
    }
}
