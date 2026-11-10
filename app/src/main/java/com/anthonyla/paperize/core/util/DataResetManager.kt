package com.anthonyla.paperize.core.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.anthonyla.paperize.core.constants.Constants

/**
 * Wipes all data once when upgrading across a major version whose data is incompatible.
 * The flag lives in its own preferences file so it survives the wipe.
 */
object DataResetManager {
    private const val TAG = "DataResetManager"

    private const val VERSION_PREFS_NAME = "version_prefs"
    private const val KEY_LAST_RESET_VERSION = "last_reset_version"

    /** Increment for a release that needs a wipe. 4 = the v4.0.0 rewrite. */
    private const val TARGET_RESET_VERSION = 4

    /** Must run before anything opens the database or preferences. */
    fun performResetIfNeeded(context: Context) {
        val versionPrefs = context.getSharedPreferences(VERSION_PREFS_NAME, Context.MODE_PRIVATE)
        val lastResetVersion = versionPrefs.getInt(KEY_LAST_RESET_VERSION, 0)
        if (lastResetVersion >= TARGET_RESET_VERSION) return

        Log.w(TAG, "Performing data reset: upgrading from version $lastResetVersion to $TARGET_RESET_VERSION")
        try {
            context.deleteDatabase(Constants.DATABASE_NAME)
            context.preferencesDataStoreFile(Constants.PREFERENCES_NAME).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error during data reset", e)
        } finally {
            // Do not repeat a destructive upgrade attempt on the next launch after a failure.
            versionPrefs.edit { putInt(KEY_LAST_RESET_VERSION, TARGET_RESET_VERSION) }
        }
    }
}
