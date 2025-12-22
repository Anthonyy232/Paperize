package com.anthonyla.paperize.core.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Manages one-time data reset for major version upgrades.
 *
 * Uses a separate preferences file (version_prefs) to track reset status,
 * ensuring the flag survives the data wipe itself.
 */
object DataResetManager {
    private const val TAG = "DataResetManager"

    /**
     * Version tracking preferences - stored separately from main app prefs
     * so it survives the data wipe.
     */
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
     * Database and DataStore names to clear (must match Constants)
     */
    private const val DATABASE_NAME = "paperize_database"
    private const val DATASTORE_NAME = "paperize_preferences.preferences_pb"

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

        try {
            // 1. Delete Room database files
            deleteDatabase(context)

            // 2. Delete DataStore file
            deleteDataStore(context)

            // 3. Update version after successful wipe
            versionPrefs.edit()
                .putInt(KEY_LAST_RESET_VERSION, TARGET_RESET_VERSION)
                .apply()

            Log.i(TAG, "Data reset completed successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error during data reset", e)
            // Still mark as reset to avoid infinite reset loops on corrupted data
            versionPrefs.edit()
                .putInt(KEY_LAST_RESET_VERSION, TARGET_RESET_VERSION)
                .apply()
            return false
        }
    }

    /**
     * Deletes Room database and all related files (WAL, SHM, journal)
     */
    private fun deleteDatabase(context: Context) {
        val dbPath = context.getDatabasePath(DATABASE_NAME)
        val dbDir = dbPath.parentFile

        if (dbDir != null && dbDir.exists()) {
            // Delete main database file and all related files
            val filesToDelete = listOf(
                DATABASE_NAME,
                "$DATABASE_NAME-wal",
                "$DATABASE_NAME-shm",
                "$DATABASE_NAME-journal"
            )

            for (fileName in filesToDelete) {
                val file = File(dbDir, fileName)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Deleted $fileName: $deleted")
                }
            }
        }

        // Also use Context.deleteDatabase for good measure
        val deleted = context.deleteDatabase(DATABASE_NAME)
        Log.d(TAG, "Context.deleteDatabase result: $deleted")
    }

    /**
     * Deletes DataStore preferences file
     */
    private fun deleteDataStore(context: Context) {
        val dataStoreFile = File(context.filesDir, "datastore/$DATASTORE_NAME")
        if (dataStoreFile.exists()) {
            val deleted = dataStoreFile.delete()
            Log.d(TAG, "Deleted DataStore file: $deleted")
        }

        // Also delete the datastore directory if empty
        val dataStoreDir = File(context.filesDir, "datastore")
        if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
            val files = dataStoreDir.listFiles()
            if (files.isNullOrEmpty()) {
                dataStoreDir.delete()
            }
        }
    }
}
