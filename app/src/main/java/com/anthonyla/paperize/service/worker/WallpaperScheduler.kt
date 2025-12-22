package com.anthonyla.paperize.service.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.constants.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-based scheduler for wallpaper changes
 *
 * Advantages over AlarmManager:
 * - Better battery efficiency (system optimizes execution timing)
 * - No SCHEDULE_EXACT_ALARM permission needed
 * - Respects Doze mode and battery optimization
 * - Built-in constraint support (network, charging, idle)
 * - More reliable across Android versions and OEMs
 */
@Singleton
class WallpaperScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    companion object {
        private const val TAG = "WallpaperScheduler"
    }

    /**
     * Schedule periodic wallpaper change
     *
     * @param screenType HOME, LOCK, or BOTH (for synchronized schedules)
     * @param intervalMinutes Interval between changes (minimum 15 minutes for WorkManager)
     * @param networkRequired Whether network is required (for future online wallpaper support)
     * @param requireCharging Whether device must be charging
     */
    fun scheduleWallpaperChange(
        screenType: ScreenType,
        intervalMinutes: Int,
        networkRequired: Boolean = false,
        requireCharging: Boolean = false
    ) {

        val adjustedInterval = intervalMinutes.toLong().coerceAtLeast(Constants.MIN_INTERVAL_MINUTES.toLong())
        val workName = getWorkName(screenType)

        // Build constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (networkRequired) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
            )
            .setRequiresCharging(requireCharging)
            .build()

        // Build input data
        val inputData = Data.Builder()
            .putString(Constants.EXTRA_SCREEN_TYPE, screenType.name)
            .build()

        // Create periodic work request
        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            adjustedInterval,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(getWorkTag(screenType))
            .build()

        // Enqueue work with UPDATE policy to update existing work without triggering immediate run
        // Only runs immediately on first setup when no existing work exists
        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Scheduled $screenType wallpaper change every $adjustedInterval minutes")
    }

    /**
     * Schedule both home and lock wallpaper changes
     *
     * @param homeIntervalMinutes Home screen interval (0 to disable)
     * @param lockIntervalMinutes Lock screen interval (0 to disable)
     * @param synchronized Whether home and lock should use the same wallpaper (when intervals match)
     * @param networkRequired Whether network is required
     * @param requireCharging Whether device must be charging
     * @param onlyIfNotScheduled If true, only schedule if work is not already scheduled (for app startup)
     */
    fun scheduleWallpaperChanges(
        homeIntervalMinutes: Int,
        lockIntervalMinutes: Int,
        synchronized: Boolean = false,
        networkRequired: Boolean = false,
        requireCharging: Boolean = false,
        onlyIfNotScheduled: Boolean = false
    ) {
        schedulerScope.launch {
            mutex.withLock {
                // Don't cancel all first - let UPDATE policy handle existing work
                // This prevents immediate wallpaper change when just updating intervals

                // Check if both should be synchronized (same interval, synchronized flag set)
                val shouldSync = synchronized &&
                                homeIntervalMinutes > 0 &&
                                lockIntervalMinutes > 0 &&
                                homeIntervalMinutes == lockIntervalMinutes

                if (shouldSync) {
                    // When synchronized, schedule a single BOTH job that sets both screens
                    // This ensures both screens always show the same wallpaper
                    if (!onlyIfNotScheduled || !isWorkScheduled(ScreenType.BOTH)) {
                        scheduleWallpaperChange(
                            ScreenType.BOTH,
                            homeIntervalMinutes,
                            networkRequired,
                            requireCharging
                        )
                        // Cancel any existing separate HOME/LOCK jobs
                        cancelWallpaperChange(ScreenType.HOME)
                        cancelWallpaperChange(ScreenType.LOCK)
                        Log.d(TAG, "Scheduled synchronized wallpaper changes (BOTH) every $homeIntervalMinutes minutes")
                    } else {
                        Log.d(TAG, "Skipped scheduling BOTH wallpaper changes - already scheduled")
                    }
                } else {
                    // Independent schedules - schedule or cancel each separately
                    // Check if BOTH was scheduled BEFORE cancelling it
                    // This is needed to force schedule HOME/LOCK when switching from sync mode
                    val wasBothScheduled = isWorkScheduled(ScreenType.BOTH)
                    
                    // Cancel BOTH job if it exists (switching from sync to separate mode)
                    cancelWallpaperChange(ScreenType.BOTH)
                    
                    // When switching away from sync mode, we must ensure HOME/LOCK are scheduled
                    // even if onlyIfNotScheduled is true, because they were previously suppressed
                    val forceSchedule = wasBothScheduled

                    if (homeIntervalMinutes > 0) {
                        if (!onlyIfNotScheduled || forceSchedule || !isWorkScheduled(ScreenType.HOME)) {
                            scheduleWallpaperChange(
                                ScreenType.HOME,
                                homeIntervalMinutes,
                                networkRequired,
                                requireCharging
                            )
                        } else {
                            Log.d(TAG, "Skipped scheduling HOME wallpaper changes - already scheduled")
                        }
                    } else {
                        cancelWallpaperChange(ScreenType.HOME)
                    }

                    if (lockIntervalMinutes > 0) {
                        if (!onlyIfNotScheduled || forceSchedule || !isWorkScheduled(ScreenType.LOCK)) {
                            scheduleWallpaperChange(
                                ScreenType.LOCK,
                                lockIntervalMinutes,
                                networkRequired,
                                requireCharging
                            )
                        } else {
                            Log.d(TAG, "Skipped scheduling LOCK wallpaper changes - already scheduled")
                        }
                    } else {
                        cancelWallpaperChange(ScreenType.LOCK)
                    }
                }

                // Schedule daily album refresh to validate all albums
                if (!onlyIfNotScheduled || !isAlbumRefreshScheduled()) {
                    scheduleAlbumRefresh()
                } else {
                    Log.d(TAG, "Skipped scheduling album refresh - already scheduled")
                }
            }
        }
    }

    /**
     * Update existing wallpaper change schedule
     * Uses UPDATE policy to modify the existing work
     */
    fun updateWallpaperChange(
        screenType: ScreenType,
        intervalMinutes: Int,
        networkRequired: Boolean = false,
        requireCharging: Boolean = false
    ) {
        scheduleWallpaperChange(screenType, intervalMinutes, networkRequired, requireCharging)
    }

    /**
     * Cancel wallpaper change schedule for specific screen
     */
    fun cancelWallpaperChange(screenType: ScreenType) {
        val workName = getWorkName(screenType)
        workManager.cancelUniqueWork(workName)
        Log.d(TAG, "Cancelled $screenType wallpaper change schedule")
    }

    /**
     * Cancel all wallpaper change schedules
     */
    fun cancelAllWallpaperChanges() {
        workManager.cancelUniqueWork(Constants.WORK_NAME_HOME)
        workManager.cancelUniqueWork(Constants.WORK_NAME_LOCK)
        workManager.cancelUniqueWork(Constants.WORK_NAME_BOTH)
        workManager.cancelUniqueWork(Constants.WORK_NAME_LIVE)
        cancelAlbumRefresh()
        Log.d(TAG, "Cancelled all wallpaper change schedules")
    }
    
    // ... scheduleAlbumRefresh and cancelAlbumRefresh omitted as they were not targeted for change ...
    // Wait, I should not delete them. I'll use the proper range.

    // I will target up to cancelAllWallpaperChanges for this call.
    // The next tool call will fix triggerImmediateChange.


    /**
     * Schedule daily album refresh worker
     *
     * Runs once per day (typically at 3 AM) to validate and refresh all albums:
     * - Validates all wallpaper and folder URIs in all albums
     * - Removes invalid entries (deleted files, permission changes, etc.)
     * - Rescans all folders for new wallpapers and adds them to albums
     */
    fun scheduleAlbumRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Calculate initial delay to target 3 AM
        val calendar = Calendar.getInstance()
        val nowMillis = calendar.timeInMillis
        
        if (calendar.get(Calendar.HOUR_OF_DAY) >= 3) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 3)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val initialDelay = calendar.timeInMillis - nowMillis

        val workRequest = PeriodicWorkRequestBuilder<AlbumRefreshWorker>(
            1,
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(Constants.WORK_TAG_REFRESH)
            .build()

        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_NAME_REFRESH,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Scheduled daily album refresh")
    }

    /**
     * Cancel daily album refresh worker
     */
    fun cancelAlbumRefresh() {
        workManager.cancelUniqueWork(Constants.WORK_NAME_REFRESH)
        Log.d(TAG, "Cancelled daily album refresh")
    }

    /**
     * Trigger immediate wallpaper change (one-time work)
     * Does not affect the periodic schedule
     *
     * @param screenType HOME, LOCK, or BOTH (for synchronized immediate change)
     */
    fun triggerImmediateChange(screenType: ScreenType) {
        val inputData = Data.Builder()
            .putString(Constants.EXTRA_SCREEN_TYPE, screenType.name)
            .build()

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<WallpaperChangeWorker>()
            .setInputData(inputData)
            .addTag("${Constants.WORK_TAG_IMMEDIATE}_${screenType.name.lowercase()}")
            .build()

        val uniqueWorkName = "${Constants.WORK_TAG_IMMEDIATE}_${screenType.name.lowercase()}"

        workManager.enqueueUniqueWork(
            uniqueWorkName,
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Log.d(TAG, "Triggered immediate $screenType wallpaper change")
    }

    private fun getWorkName(screenType: ScreenType): String {
        return when (screenType) {
            ScreenType.HOME -> Constants.WORK_NAME_HOME
            ScreenType.LOCK -> Constants.WORK_NAME_LOCK
            ScreenType.BOTH -> Constants.WORK_NAME_BOTH
            ScreenType.LIVE -> Constants.WORK_NAME_LIVE
        }
    }

    private fun getWorkTag(screenType: ScreenType): String {
        return when (screenType) {
            ScreenType.HOME -> Constants.WORK_TAG_HOME
            ScreenType.LOCK -> Constants.WORK_TAG_LOCK
            ScreenType.BOTH -> Constants.WORK_TAG_BOTH
            ScreenType.LIVE -> Constants.WORK_TAG_LIVE
        }
    }

    /**
     * Check if wallpaper change work is already scheduled
     *
     * @param screenType Screen type to check
     * @return true if work is scheduled (ENQUEUED or RUNNING), false otherwise
     */
    private suspend fun isWorkScheduled(screenType: ScreenType): Boolean {
        val workName = getWorkName(screenType)
        val workInfos = try {
            workManager.getWorkInfosForUniqueWorkFlow(workName).first()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking work status", e)
            return false
        }

        if (workInfos.isEmpty()) return false

        return workInfos.any { workInfo ->
            workInfo.state == androidx.work.WorkInfo.State.ENQUEUED ||
            workInfo.state == androidx.work.WorkInfo.State.RUNNING
        }
    }

    /**
     * Check if album refresh work is already scheduled
     *
     * @return true if work is scheduled (ENQUEUED or RUNNING), false otherwise
     */
    private suspend fun isAlbumRefreshScheduled(): Boolean {
        val workInfos = try {
            workManager.getWorkInfosForUniqueWorkFlow(Constants.WORK_NAME_REFRESH).first()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking refresh status", e)
            return false
        }

        if (workInfos.isEmpty()) return false

        return workInfos.any { workInfo ->
            workInfo.state == androidx.work.WorkInfo.State.ENQUEUED ||
            workInfo.state == androidx.work.WorkInfo.State.RUNNING
        }
    }
}
