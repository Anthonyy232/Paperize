package com.anthonyla.paperize.feature.wallpaper.wallpaper_service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.core.getDeviceScreenSize
import com.anthonyla.paperize.core.processBitmap
import com.anthonyla.paperize.core.retrieveBitmap
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import com.anthonyla.paperize.feature.wallpaper.tasker_shortcut.triggerWallpaperTaskerEvent
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject


/**
 * Service for changing home screen
 */
@AndroidEntryPoint
class HomeWallpaperService: Service() {
    private val handleThread = HandlerThread("HomeThread")
    private lateinit var workerHandler: Handler
    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    private var scheduleSeparately: Boolean = false
    private var homeInterval: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
    private var lockInterval: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
    private var type = Type.SINGLE.ordinal

    enum class Actions {
        START,
        UPDATE,
        REFRESH
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        handleThread.start()
        workerHandler = Handler(handleThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                Actions.START.toString() -> {
                    Log.d("PaperizeWallpaperChanger", "Starting home start wallpaper service")
                    homeInterval = intent.getIntExtra("homeInterval", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                    lockInterval = intent.getIntExtra("lockInterval", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                    scheduleSeparately = intent.getBooleanExtra("scheduleSeparately", false)
                    type = intent.getIntExtra("type", Type.SINGLE.ordinal)
                    workerTaskStart()
                }
                Actions.UPDATE.toString() -> {
                    Log.d("PaperizeWallpaperChanger", "Starting home update wallpaper service")
                    workerTaskUpdate()
                }
                Actions.REFRESH.toString() -> {
                    Log.d("PaperizeWallpaperChanger", "Starting home refresh wallpaper service")
                    workerTaskRefresh()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        workerHandler.removeCallbacksAndMessages(null)
        handleThread.quitSafely()
    }

    private fun workerTaskStart() {
        CoroutineScope(Dispatchers.Default).launch {
            delay(20)
            changeWallpaper(this@HomeWallpaperService)
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    private fun workerTaskUpdate() {
        CoroutineScope(Dispatchers.Default).launch {
            updateCurrentWallpaper(this@HomeWallpaperService)
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    private fun workerTaskRefresh() {
        CoroutineScope(Dispatchers.Default).launch {
            refreshAlbum(this@HomeWallpaperService)
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    /**
     * Creates a notification for the wallpaper service
     */
    private fun createNotification(nextSetTime: LocalDateTime?): Notification? {
        val changeWallpaperIntent = Intent(this, WallpaperBootAndChangeReceiver::class.java)
        val pendingChangeWallpaperIntent = PendingIntent.getBroadcast(this, 0, changeWallpaperIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        if (nextSetTime != null) {
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Builder(this, "wallpaper_service_channel")
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.next_wallpaper_change, nextSetTime.format(formatter)))
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.notification_icon, getString(R.string.change_wallpaper), pendingChangeWallpaperIntent)
                .build()
        }
        return null
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        /*try {
            var selectedAlbum = selectedRepository.getSelectedAlbum().first()
            if (selectedAlbum.isEmpty()) {
                onDestroy()
                return
            }
            else {
                val setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false
                val setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false
                if (!setHome && !setLock) {
                    onDestroy()
                    return
                }
                val scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL
                val darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false
                val homeDarkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_DARKEN_PERCENTAGE) ?: 100
                val blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false
                val homeBlurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_BLUR_PERCENTAGE) ?: 0
                val vignette = settingsDataStoreImpl.getBoolean(SettingsConstants.VIGNETTE) ?: false
                val homeVignettePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_VIGNETTE_PERCENTAGE) ?: 0
                val grayscale = settingsDataStoreImpl.getBoolean(SettingsConstants.GRAYSCALE) ?: false
                val homeGrayscalePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE) ?: 0
                val homeAlbumName = settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME) ?: ""
                val lockAlbumName = settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME) ?: ""
                var homeAlbum = selectedAlbum.find { it.album.initialAlbumName == homeAlbumName }
                if (homeAlbum == null) {
                    onDestroy()
                    return
                }
                when {
                    // Case: Set home and lock screen wallpapers using separate albums (home screen and lock screen album)
                    setHome && setLock && scheduleSeparately -> {
                        var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                        if (wallpaper == null) {
                            val newWallpapers = homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpapers.firstOrNull()
                            if (wallpaper == null) {
                                selectedRepository.cascadeDeleteAlbum(homeAlbum.album.initialAlbumName)
                                onDestroy()
                                return
                            }
                            else {
                                val success = isValidUri(context, wallpaper)
                                settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, if (newWallpapers.size > 1) newWallpapers[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                                if (success) {
                                    selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers.drop(1))))
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                    setWallpaper(
                                        context = context,
                                        wallpaper = wallpaper.toUri(),
                                        darken = darken,
                                        darkenPercent = homeDarkenPercentage,
                                        scaling = scaling,
                                        blur = blur,
                                        blurPercent = homeBlurPercentage,
                                        vignette = vignette,
                                        vignettePercent = homeVignettePercentage,
                                        grayscale = grayscale,
                                        grayscalePercent = homeGrayscalePercentage
                                    )
                                }
                                else {
                                    val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                    if (wallpaperToDelete != null) {
                                        albumRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.upsertSelectedAlbum(
                                            homeAlbum.copy(
                                                album = homeAlbum.album.copy(
                                                    homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                                ),
                                                wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                            if ((homeInterval % lockInterval == 0) || (lockInterval % homeInterval == 0) && (homeAlbumName == lockAlbumName)) {
                                delay(1000)
                                selectedAlbum = selectedRepository.getSelectedAlbum().first()
                                homeAlbum = selectedAlbum.find { it.album.initialAlbumName == homeAlbumName }
                            }
                            if (homeAlbum != null) {
                                if (success) {
                                    selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.drop(1))))
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                    setWallpaper(
                                        context = context,
                                        wallpaper = wallpaper.toUri(),
                                        darken = darken,
                                        darkenPercent = homeDarkenPercentage,
                                        scaling = scaling,
                                        blur = blur,
                                        blurPercent = homeBlurPercentage,
                                        vignette = vignette,
                                        vignettePercent = homeVignettePercentage,
                                        grayscale = grayscale,
                                        grayscalePercent = homeGrayscalePercentage
                                    )
                                }
                                else {
                                    val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                    if (wallpaperToDelete != null) {
                                        albumRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.upsertSelectedAlbum(
                                            homeAlbum.copy(
                                                album = homeAlbum.album.copy(
                                                    homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                                ),
                                                wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Case: Set home and lock screen wallpapers using the same album (home screen album)
                    setHome && setLock && !scheduleSeparately -> {
                        var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                        if (wallpaper == null) {
                            val newWallpapers = homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpapers.firstOrNull()
                            if (wallpaper == null) {
                                selectedRepository.cascadeDeleteAlbum(homeAlbum.album.initialAlbumName)
                                onDestroy()
                                return
                            }
                            else {
                                val success = isValidUri(context, wallpaper)
                                settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                                if (success) {
                                    if (newWallpapers.size > 1) {
                                        selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers.drop(1))))
                                    }
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                    setWallpaper(
                                        context = context,
                                        wallpaper = wallpaper.toUri(),
                                        darken = darken,
                                        darkenPercent = homeDarkenPercentage,
                                        scaling = scaling,
                                        blur = blur,
                                        blurPercent = homeBlurPercentage,
                                        vignette = vignette,
                                        vignettePercent = homeVignettePercentage,
                                        grayscale = grayscale,
                                        grayscalePercent = homeGrayscalePercentage
                                    )
                                }
                                else {
                                    val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                    if (wallpaperToDelete != null) {
                                        albumRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.upsertSelectedAlbum(
                                            homeAlbum.copy(
                                                album = homeAlbum.album.copy(
                                                    homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                                ),
                                                wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                            if (success) {
                                if (homeAlbum.album.homeWallpapersInQueue.size > 1) {
                                    selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.drop(1))))
                                }
                                else {
                                    val newWallpapers = homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                                    if (newWallpapers.isNotEmpty()) {
                                        selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers)))
                                    }
                                    else {
                                        selectedRepository.cascadeDeleteAlbum(homeAlbum.album.initialAlbumName)
                                        onDestroy()
                                        return
                                    }
                                }
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = homeDarkenPercentage,
                                    scaling = scaling,
                                    blur = blur,
                                    blurPercent = homeBlurPercentage,
                                    vignette = vignette,
                                    vignettePercent = homeVignettePercentage,
                                    grayscale = grayscale,
                                    grayscalePercent = homeGrayscalePercentage
                                )
                            }
                            else {
                                val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.upsertSelectedAlbum(
                                        homeAlbum.copy(
                                            album = homeAlbum.album.copy(
                                                homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                            ),
                                            wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                        )
                                    )
                                }
                            }
                        }
                    }
                    // Case: Set home screen wallpaper (home screen album)
                    setHome -> {
                        var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                        if (wallpaper == null) {
                            val newWallpapers = homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpapers.firstOrNull()
                            if (wallpaper == null) {
                                selectedRepository.cascadeDeleteAlbum(homeAlbum.album.initialAlbumName)
                                onDestroy()
                                return
                            }
                            else {
                                val success = isValidUri(context, wallpaper)
                                settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                                settingsDataStoreImpl.putString(SettingsConstants.NEXT_LOCK_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                                if (success) {
                                    selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers.drop(1))))
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                    setWallpaper(
                                        context = context,
                                        wallpaper = wallpaper.toUri(),
                                        darken = darken,
                                        darkenPercent = homeDarkenPercentage,
                                        scaling = scaling,
                                        blur = blur,
                                        blurPercent = homeBlurPercentage,
                                        vignette = vignette,
                                        vignettePercent = homeVignettePercentage,
                                        grayscale = grayscale,
                                        grayscalePercent = homeGrayscalePercentage
                                    )

                                }
                                else {
                                    val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                    if (wallpaperToDelete != null) {
                                        albumRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.upsertSelectedAlbum(
                                            homeAlbum.copy(
                                                album = homeAlbum.album.copy(
                                                    homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                                ),
                                                wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            settingsDataStoreImpl.putString(SettingsConstants.NEXT_HOME_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                            settingsDataStoreImpl.putString(SettingsConstants.NEXT_LOCK_WALLPAPER, if (homeAlbum.album.homeWallpapersInQueue.size > 1) homeAlbum.album.homeWallpapersInQueue[1] else homeAlbum.wallpapers.firstOrNull()?.wallpaperUri ?: "")
                            if (success) {
                                selectedRepository.upsertSelectedAlbum(homeAlbum.copy(album = homeAlbum.album.copy(homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.drop(1))))
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = homeDarkenPercentage,
                                    scaling = scaling,
                                    blur = blur,
                                    blurPercent = homeBlurPercentage,
                                    vignette = vignette,
                                    vignettePercent = homeVignettePercentage,
                                    grayscale = grayscale,
                                    grayscalePercent = homeGrayscalePercentage
                                )
                            }
                            else {
                                val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.upsertSelectedAlbum(
                                        homeAlbum.copy(
                                            album = homeAlbum.album.copy(
                                                homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                            ),
                                            wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                // Run notification
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                val homeNextSetTime: LocalDateTime?
                var lockNextSetTime = try {
                    LocalDateTime.parse(settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME))
                } catch (_: Exception) {
                    LocalDateTime.now()
                }
                val nextSetTime: LocalDateTime?
                val currentTime = LocalDateTime.now()
                if (homeInterval == lockInterval) {
                    homeNextSetTime = currentTime.plusMinutes(homeInterval.toLong())
                    lockNextSetTime = homeNextSetTime
                    nextSetTime = homeNextSetTime
                    nextSetTime?.let {
                        settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, it.format(formatter))
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, it.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, it.toString())
                    }
                }
                else {
                    homeNextSetTime = currentTime.plusMinutes(homeInterval.toLong())
                    nextSetTime = if (homeNextSetTime.isBefore(lockNextSetTime) && homeNextSetTime.isAfter(currentTime)) homeNextSetTime
                    else if (lockNextSetTime.isAfter(currentTime)) lockNextSetTime
                    else currentTime.plusMinutes(homeInterval.toLong())
                    nextSetTime?.let {
                        settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, it.format(formatter))
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, homeNextSetTime.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, lockNextSetTime.toString())
                    }
                }
                val notification = createNotification(nextSetTime)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notification?.let { notificationManager.notify(1, it) }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper", e)
        }*/
    }

    /**
     * Updates the current wallpaper with current settings
     */
    private suspend fun updateCurrentWallpaper(context: Context) {
        /*try {
            val selectedAlbum = selectedRepository.getSelectedAlbum().first()
            if (selectedAlbum.isEmpty()) {
                onDestroy()
                return
            }
            else {
                val enableChanger = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false
                val setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false
                val setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false
                if (!enableChanger || (!setHome && !setLock)) {
                    onDestroy()
                    return
                }

                val scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL
                val darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false
                val homeDarkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_DARKEN_PERCENTAGE) ?: 100
                val blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false
                val homeBlurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_BLUR_PERCENTAGE) ?: 0
                val currentHomeWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_HOME_WALLPAPER) ?: ""
                val vignette = settingsDataStoreImpl.getBoolean(SettingsConstants.VIGNETTE) ?: false
                val homeVignettePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_VIGNETTE_PERCENTAGE) ?: 0
                val grayscale = settingsDataStoreImpl.getBoolean(SettingsConstants.GRAYSCALE) ?: false
                val homeGrayscalePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE) ?: 0

                setWallpaper(
                    context = context,
                    wallpaper = currentHomeWallpaper.toUri(),
                    darken = darken,
                    darkenPercent = homeDarkenPercentage,
                    scaling = scaling,
                    blur = blur,
                    blurPercent = homeBlurPercentage,
                    vignette = vignette,
                    vignettePercent = homeVignettePercentage,
                    grayscale = grayscale,
                    grayscalePercent = homeGrayscalePercentage
                )
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in updating", e)
        }*/
    }

    /**
     * Sets the wallpaper to the given uri
     */
    private fun setWallpaper(
        context: Context,
        wallpaper: Uri,
        darken: Boolean,
        darkenPercent: Int,
        scaling: ScalingConstants,
        blur: Boolean,
        blurPercent: Int,
        vignette: Boolean,
        vignettePercent: Int,
        grayscale: Boolean,
        grayscalePercent: Int
    ): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val size = getDeviceScreenSize(context)
            val bitmap = retrieveBitmap(context, wallpaper, size.width, size.height)
            if (bitmap == null) return false
            else if (wallpaperManager.isSetWallpaperAllowed) {
                processBitmap(size.width, size.height, bitmap, darken, darkenPercent, scaling, blur, blurPercent, vignette, vignettePercent, grayscale, grayscalePercent)?.let { image ->
                    wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_SYSTEM)
                    wallpaperManager.forgetLoadedWallpaper()
                    image.recycle()
                }
                bitmap.recycle()
                context.triggerWallpaperTaskerEvent()
                return true
            }
            else return false
        } catch (e: IOException) {
            Log.e("PaperizeWallpaperChanger", "Error setting wallpaper", e)
            return false
        }
    }

    /**
     * Refreshes the album by deleting invalid wallpapers and updating folder cover uri and wallpapers uri-
     */
    private fun refreshAlbum(context: Context) {
        /*CoroutineScope(Dispatchers.IO).launch {
            try {
                var albumWithWallpapers = albumRepository.getAlbumsWithWallpaperAndFolder().first()
                albumWithWallpapers.forEach { albumWithWallpaper ->
                    // Delete wallpaper if the URI is invalid
                    val invalidWallpapers = albumWithWallpaper.wallpapers.filterNot { wallpaper ->
                        val file = DocumentFileCompat.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                        file?.exists() == true
                    }
                    if (invalidWallpapers.isNotEmpty()) {
                        albumRepository.deleteWallpaperList(invalidWallpapers)
                    }

                    // Update folder cover uri and wallpapers uri
                    albumWithWallpaper.folders.forEach { folder ->
                        try {
                            DocumentFileCompat.fromTreeUri(context, folder.folderUri.toUri())?.let { folderDirectory ->
                                if (!folderDirectory.isDirectory()) {
                                    albumRepository.deleteFolder(folder)
                                } else {
                                    val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                                    val folderCoverFile = folder.coverUri?.let { DocumentFileCompat.fromSingleUri(context, it.toUri()) }
                                    val folderCover = folderCoverFile?.takeIf { it.exists() }?.uri?.toString() ?: wallpapers.randomOrNull()
                                    albumRepository.updateFolder(folder.copy(coverUri = folderCover, wallpapers = wallpapers))
                                }
                            }
                        } catch (_: Exception) {
                            DocumentFile.fromTreeUri(context, folder.folderUri.toUri())?.let { folderDirectory ->
                                if (!folderDirectory.isDirectory) {
                                    albumRepository.deleteFolder(folder)
                                } else {
                                    val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                                    val folderCoverFile = folder.coverUri?.let { DocumentFileCompat.fromSingleUri(context, it.toUri()) }
                                    val folderCover = folderCoverFile?.takeIf { it.exists() }?.uri?.toString() ?: wallpapers.randomOrNull()
                                    albumRepository.updateFolder(folder.copy(coverUri = folderCover, wallpapers = wallpapers))
                                }
                            }
                        }

                    }

                    // Delete empty albums
                    if (albumWithWallpaper.wallpapers.isEmpty() && albumWithWallpaper.folders.all { it.wallpapers.isEmpty() }) {
                        albumRepository.deleteAlbum(albumWithWallpaper.album)
                    }
                }

                // Update selected album
                albumWithWallpapers = albumRepository.getAlbumsWithWallpaperAndFolder().first()
                val selectedAlbum = selectedRepository.getSelectedAlbum().first().firstOrNull()
                if (selectedAlbum != null) {
                    albumWithWallpapers.find { it.album.initialAlbumName == selectedAlbum.album.initialAlbumName }
                        ?.let { foundAlbum ->
                            val albumNameHashCode = foundAlbum.album.initialAlbumName.hashCode()
                            val wallpapers: List<Wallpaper> =
                                foundAlbum.wallpapers + foundAlbum.folders.flatMap { folder ->
                                    folder.wallpapers.map { wallpaper ->
                                        Wallpaper(
                                            initialAlbumName = foundAlbum.album.initialAlbumName,
                                            wallpaperUri = wallpaper,
                                            key = wallpaper.hashCode() + albumNameHashCode,
                                        )
                                    }
                                }
                            val wallpapersUri = wallpapers.map { it.wallpaperUri }.toSet()
                            if (wallpapersUri.isEmpty()) {
                                selectedRepository.deleteAll()
                                onDestroy()
                            }
                        } ?: run { onDestroy() }
                }
            } catch (e: Exception) {
                Log.e("PaperizeWallpaperChanger", "Error refreshing album", e)
            }
        }*/
    }
}