package com.anthonyla.paperize.feature.wallpaper.wallpaper_service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.core.decompress
import com.anthonyla.paperize.core.findFirstValidUri
import com.anthonyla.paperize.core.getDeviceScreenSize
import com.anthonyla.paperize.core.getFolderMetadata
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.core.isDirectory
import com.anthonyla.paperize.core.isValidUri
import com.anthonyla.paperize.core.processBitmap
import com.anthonyla.paperize.core.retrieveBitmap
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import com.anthonyla.paperize.feature.wallpaper.tasker_shortcut.triggerWallpaperTaskerEvent
import com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperBootAndChangeReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
                    homeInterval = intent.getIntExtra("homeInterval", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                    lockInterval = intent.getIntExtra("lockInterval", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                    scheduleSeparately = intent.getBooleanExtra("scheduleSeparately", false)
                    type = intent.getIntExtra("type", Type.SINGLE.ordinal)
                    workerTaskStart()
                }
                Actions.UPDATE.toString() -> {
                    workerTaskUpdate()
                }
                Actions.REFRESH.toString() -> {
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
            delay(50)
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

    // Creates a notification for the wallpaper service
    private fun createNotification(nextSetTime: LocalDateTime?): android.app.Notification? {
        nextSetTime?.let {
            val changeWallpaperIntent = Intent(this, WallpaperBootAndChangeReceiver::class.java).apply {
                action = "com.anthonyla.paperize.SHORTCUT"
            }
            val pendingChangeWallpaperIntent = PendingIntent.getBroadcast(
                this,
                0,
                changeWallpaperIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            val pendingMainActivityIntent = PendingIntent.getActivity(
                this,
                3,
                mainActivityIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            val formattedNextSetTime = nextSetTime.format(formatter)
            return NotificationCompat.Builder(this, "wallpaper_service_channel").apply {
                setContentTitle(getString(R.string.app_name))
                setContentText(getString(R.string.next_wallpaper_change, formattedNextSetTime))
                setSmallIcon(R.drawable.notification_icon)
                setContentIntent(pendingMainActivityIntent)
                addAction(R.drawable.notification_icon, getString(R.string.change_wallpaper), pendingChangeWallpaperIntent)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
            }.build()
        }
        return null
    }

    private suspend fun getWallpaperSettings(): SettingsState.ServiceSettings {
        return SettingsState.ServiceSettings(
            enableChanger = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false,
            setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_HOME_WALLPAPER) ?: false,
            setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_LOCK_WALLPAPER) ?: false,
            scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL,
            darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false,
            homeDarkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_DARKEN_PERCENTAGE) ?: 100,
            lockDarkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_DARKEN_PERCENTAGE) ?: 100,
            blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false,
            homeBlurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_BLUR_PERCENTAGE) ?: 0,
            lockBlurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_BLUR_PERCENTAGE) ?: 0,
            vignette = settingsDataStoreImpl.getBoolean(SettingsConstants.VIGNETTE) ?: false,
            homeVignettePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_VIGNETTE_PERCENTAGE) ?: 0,
            lockVignettePercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_VIGNETTE_PERCENTAGE) ?: 0,
            grayscale = settingsDataStoreImpl.getBoolean(SettingsConstants.GRAYSCALE) ?: false,
            homeGrayscalePercentage = settingsDataStoreImpl.getInt(SettingsConstants.HOME_GRAYSCALE_PERCENTAGE) ?: 0,
            lockGrayscalePercentage = settingsDataStoreImpl.getInt(SettingsConstants.LOCK_GRAYSCALE_PERCENTAGE) ?: 0,
            lockAlbumName = settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME) ?: "",
            homeAlbumName = settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME) ?: "",
            shuffle = settingsDataStoreImpl.getBoolean(SettingsConstants.SHUFFLE) ?: true
        )
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        try {
            var selectedAlbum = albumRepository.getSelectedAlbums().first()
            if (selectedAlbum.isEmpty()) {
                onDestroy()
                return
            }
            val settings = getWallpaperSettings()
            if (!settings.setHome && !settings.setLock) {
                onDestroy()
                return
            }
            var homeAlbum = selectedAlbum.find { it.album.initialAlbumName == settings.homeAlbumName }
            if (homeAlbum == null) {
                onDestroy()
                return
            }
            when {
                // Case: Set home and lock screen wallpapers using separate albums (home screen and lock screen album)
                settings.setHome && settings.setLock && scheduleSeparately -> {
                    var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                        else homeAlbum.wallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            albumRepository.cascadeDeleteAlbum(homeAlbum.album)
                            onDestroy()
                            return
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                albumRepository.upsertAlbum(homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers.drop(1)))
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                    darken = settings.darken,
                                    darkenPercent = settings.homeDarkenPercentage,
                                    scaling = settings.scaling,
                                    blur = settings.blur,
                                    blurPercent = settings.homeBlurPercentage,
                                    vignette = settings.vignette,
                                    vignettePercent = settings.homeVignettePercentage,
                                    grayscale = settings.grayscale,
                                    grayscalePercent = settings.homeGrayscalePercentage
                                )
                            }
                            else {
                                val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                                        homeAlbum.copy(
                                            wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                            album = homeAlbum.album.copy(
                                                homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper },
                                                lockWallpapersInQueue = homeAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                    else {
                        val success = isValidUri(context, wallpaper)
                        if ((homeInterval % lockInterval == 0) || (lockInterval % homeInterval == 0) && (settings.homeAlbumName == settings.lockAlbumName)) {
                            delay(1000)
                            selectedAlbum = albumRepository.getSelectedAlbums().first()
                            homeAlbum = selectedAlbum.find { it.album.initialAlbumName == settings.homeAlbumName }
                        }
                        if (homeAlbum != null) {
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                albumRepository.upsertAlbum(homeAlbum.album.copy(homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.drop(1)))
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                    darken = settings.darken,
                                    darkenPercent = settings.homeDarkenPercentage,
                                    scaling = settings.scaling,
                                    blur = settings.blur,
                                    blurPercent = settings.homeBlurPercentage,
                                    vignette = settings.vignette,
                                    vignettePercent = settings.homeVignettePercentage,
                                    grayscale = settings.grayscale,
                                    grayscalePercent = settings.homeGrayscalePercentage
                                )
                            }
                            else {
                                val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                                        homeAlbum.copy(
                                            wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                            album = homeAlbum.album.copy(
                                                homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper },
                                                lockWallpapersInQueue = homeAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                // Case: Set home and lock screen wallpapers using the same album (home screen album)
                settings.setHome && settings.setLock && !scheduleSeparately -> {
                    var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                        else homeAlbum.wallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            albumRepository.cascadeDeleteAlbum(homeAlbum.album)
                            onDestroy()
                            return
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                albumRepository.upsertAlbum(homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers.drop(1)))
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                    darken = settings.darken,
                                    darkenPercent = settings.homeDarkenPercentage,
                                    scaling = settings.scaling,
                                    blur = settings.blur,
                                    blurPercent = settings.homeBlurPercentage,
                                    vignette = settings.vignette,
                                    vignettePercent = settings.homeVignettePercentage,
                                    grayscale = settings.grayscale,
                                    grayscalePercent = settings.homeGrayscalePercentage,
                                    both = true
                                )
                            }
                            else {
                                val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                                        homeAlbum.copy(
                                            wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                            album = homeAlbum.album.copy(
                                                homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper },
                                                lockWallpapersInQueue = homeAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                    else {
                        val success = isValidUri(context, wallpaper)
                        if (success) {
                            settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                            settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                            albumRepository.upsertAlbum(homeAlbum.album.copy(homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.drop(1)))
                            setWallpaper(
                                context = context,
                                wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                darken = settings.darken,
                                darkenPercent = settings.homeDarkenPercentage,
                                scaling = settings.scaling,
                                blur = settings.blur,
                                blurPercent = settings.homeBlurPercentage,
                                vignette = settings.vignette,
                                vignettePercent = settings.homeVignettePercentage,
                                grayscale = settings.grayscale,
                                grayscalePercent = settings.homeGrayscalePercentage,
                                both = true
                            )

                        }
                        else {
                            val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                            if (wallpaperToDelete != null) {
                                albumRepository.deleteWallpaper(wallpaperToDelete)
                                albumRepository.upsertAlbumWithWallpaperAndFolder(
                                    homeAlbum.copy(
                                        wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                        album = homeAlbum.album.copy(
                                            homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper },
                                            lockWallpapersInQueue = homeAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
                // Case: Set home screen wallpaper (home screen album)
                settings.setHome -> {
                    var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) homeAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                        else homeAlbum.wallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            albumRepository.cascadeDeleteAlbum(homeAlbum.album)
                            onDestroy()
                            return
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                albumRepository.upsertAlbum(homeAlbum.album.copy(homeWallpapersInQueue = newWallpapers.drop(1)))
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                    darken = settings.darken,
                                    darkenPercent = settings.homeDarkenPercentage,
                                    scaling = settings.scaling,
                                    blur = settings.blur,
                                    blurPercent = settings.homeBlurPercentage,
                                    vignette = settings.vignette,
                                    vignettePercent = settings.homeVignettePercentage,
                                    grayscale = settings.grayscale,
                                    grayscalePercent = settings.homeGrayscalePercentage
                                )

                            }
                            else {
                                val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                                        homeAlbum.copy(
                                            wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                            album = homeAlbum.album.copy(
                                                homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper },
                                                lockWallpapersInQueue = homeAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                    else {
                        val success = isValidUri(context, wallpaper)
                        if (success) {
                            settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                            settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                            albumRepository.upsertAlbum(homeAlbum.album.copy(homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.drop(1)))
                            setWallpaper(
                                context = context,
                                wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                darken = settings.darken,
                                darkenPercent = settings.homeDarkenPercentage,
                                scaling = settings.scaling,
                                blur = settings.blur,
                                blurPercent = settings.homeBlurPercentage,
                                vignette = settings.vignette,
                                vignettePercent = settings.homeVignettePercentage,
                                grayscale = settings.grayscale,
                                grayscalePercent = settings.homeGrayscalePercentage
                            )
                        }
                        else {
                            val wallpaperToDelete = homeAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                            if (wallpaperToDelete != null) {
                                albumRepository.deleteWallpaper(wallpaperToDelete)
                                albumRepository.upsertAlbumWithWallpaperAndFolder(
                                    homeAlbum.copy(
                                        wallpapers = homeAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                        album = homeAlbum.album.copy(
                                            homeWallpapersInQueue = homeAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper },
                                            lockWallpapersInQueue = homeAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Run notification
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            val currentTime = LocalDateTime.now()
            val homeNextSetTime: LocalDateTime = currentTime.plusMinutes(homeInterval.toLong())
            val lockNextSetTime: LocalDateTime = try {
                LocalDateTime.parse(settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME))
            } catch (e: Exception) {
                currentTime
            }
            val nextSetTime: LocalDateTime = when {
                homeInterval == lockInterval -> homeNextSetTime
                settings.setHome && settings.setLock && scheduleSeparately -> {
                    if (homeNextSetTime.isBefore(lockNextSetTime) && homeNextSetTime.isAfter(currentTime)) {
                        homeNextSetTime
                    } else lockNextSetTime
                }
                else -> homeNextSetTime
            }

            nextSetTime.let {
                settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, it.format(formatter))
                settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, homeNextSetTime.toString())
                settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, lockNextSetTime.toString())
            }

            val notification = createNotification(nextSetTime)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notification?.let { notificationManager.notify(1, it) }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper", e)
        }
    }

    /**
     * Updates the current wallpaper with current settings
     */
    private suspend fun updateCurrentWallpaper(context: Context) {
        try {
            val selectedAlbum = albumRepository.getSelectedAlbums().first()
            if (selectedAlbum.isEmpty()) {
                onDestroy()
                return
            }
            val settings = getWallpaperSettings()
            if (!settings.enableChanger || (!settings.setHome && !settings.setLock)) {
                onDestroy()
                return
            }

            val currentHomeWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_HOME_WALLPAPER) ?: ""
            setWallpaper(
                context = context,
                wallpaper = currentHomeWallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                darken = settings.darken,
                darkenPercent = settings.homeDarkenPercentage,
                scaling = settings.scaling,
                blur = settings.blur,
                blurPercent = settings.homeBlurPercentage,
                vignette = settings.vignette,
                vignettePercent = settings.homeVignettePercentage,
                grayscale = settings.grayscale,
                grayscalePercent = settings.homeGrayscalePercentage
            )
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in updating", e)
        }
    }

    /**
     * Sets the wallpaper to the given uri
     */
    private fun setWallpaper(
        context: Context,
        wallpaper: Uri?,
        darken: Boolean,
        darkenPercent: Int,
        scaling: ScalingConstants,
        blur: Boolean,
        blurPercent: Int,
        vignette: Boolean,
        vignettePercent: Int,
        grayscale: Boolean,
        grayscalePercent: Int,
        both: Boolean = false
    ): Boolean {
        wallpaper?.let {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val size = getDeviceScreenSize(context)
                val bitmap = retrieveBitmap(context, wallpaper, size.width, size.height)
                if (bitmap == null) return false
                else if (wallpaperManager.isSetWallpaperAllowed) {
                    processBitmap(size.width, size.height, bitmap, darken, darkenPercent, scaling, blur, blurPercent, vignette, vignettePercent, grayscale, grayscalePercent)?.let { image ->
                        if (both) setWallpaperSafely(image, WallpaperManager.FLAG_LOCK, wallpaperManager)
                        setWallpaperSafely(image, WallpaperManager.FLAG_SYSTEM, wallpaperManager)
                        image.recycle()
                    }
                    bitmap.recycle()
                    context.triggerWallpaperTaskerEvent()
                    return true
                }
                else return false
            } catch (e: Exception) {
                Log.e("PaperizeWallpaperChanger", "Error setting wallpaper", e)
                return false
            }
        }
        return false
    }

    /**
     * Refreshes the album by deleting invalid wallpapers and updating folder cover uri and wallpapers uri-
     */
    private fun refreshAlbum(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PaperizeWallpaperChanger", "Refreshing album")
            try {
                val albumWithWallpapers = albumRepository.getAlbumsWithWallpaperAndFolder().first()
                albumWithWallpapers.forEach { album ->
                    // Remove invalid wallpapers
                    val validWallpapers = async {
                        album.wallpapers
                            .asSequence()
                            .filter { isValidUri(context, it.wallpaperUri) }
                            .mapIndexed { index, wallpaper -> wallpaper.copy(order = index) }
                            .toList()
                    }

                    // Remove invalid folders and inner wallpapers
                    val validFolders = async {
                        album.folders
                            .asSequence()
                            .filterNot { isDirectory(context, it.folderUri) }
                            .map { folder ->
                                async {
                                    val metadata = getFolderMetadata(folder.folderUri, context)
                                    if (metadata.lastModified != folder.dateModified) {
                                        val existingWallpapers = folder.wallpapers
                                            .asSequence()
                                            .filter { isValidUri(context, it.wallpaperUri) }
                                            .mapIndexed { index, wallpaper -> wallpaper.copy(order = index) }
                                            .toList()
                                        val newWallpapers =
                                            getWallpaperFromFolder(folder.folderUri, context)
                                                .asSequence()
                                                .filterNot { new -> existingWallpapers.any { it.wallpaperUri == new.wallpaperUri } }
                                                .mapIndexed { index, wallpaper ->
                                                    wallpaper.copy(
                                                        initialAlbumName = album.album.initialAlbumName,
                                                        order = existingWallpapers.size + 1 + index,
                                                        key = album.album.initialAlbumName.hashCode() +
                                                                folder.folderUri.hashCode() +
                                                                wallpaper.wallpaperUri.hashCode()
                                                    )
                                                }.toList()
                                        val combinedWallpapers = existingWallpapers + newWallpapers
                                        folder.copy(
                                            coverUri = combinedWallpapers.firstOrNull()?.wallpaperUri ?: "",
                                            wallpapers = combinedWallpapers,
                                            dateModified = metadata.lastModified,
                                            folderName = metadata.filename
                                        )
                                    }
                                    else { folder }
                                }
                            }
                            .toList()
                            .awaitAll()
                    }

                    val folders = validFolders.await()
                    val wallpapers = validWallpapers.await()
                    if (folders.isNotEmpty() || wallpapers.isNotEmpty()) {
                        val settings = getWallpaperSettings()
                        val coverUri = findFirstValidUri(context, folders, wallpapers)
                        val allValidWallpaperUri = (folders.flatMap { it.wallpapers } + wallpapers).sortedBy { it.order }.map{ it.wallpaperUri }.toSet()
                        val validatedHomeQueue = album.album.homeWallpapersInQueue.filter { allValidWallpaperUri.contains(it) }
                        val validatedLockQueue = album.album.lockWallpapersInQueue.filter { allValidWallpaperUri.contains(it) }
                        val newUris = allValidWallpaperUri - (validatedHomeQueue + validatedLockQueue).toSet()
                        val finalHomeQueue = if (settings.shuffle) {
                            validatedHomeQueue + newUris.shuffled()
                        } else {
                            allValidWallpaperUri.filter { it in validatedHomeQueue || it in newUris }
                        }
                        val finalLockQueue = if (settings.shuffle) {
                            validatedLockQueue + newUris.shuffled()
                        } else {
                            allValidWallpaperUri.filter { it in validatedLockQueue || it in newUris }
                        }

                        albumRepository.upsertAlbumWithWallpaperAndFolder(
                            album.copy(
                                album = album.album.copy(
                                    coverUri = coverUri,
                                    homeWallpapersInQueue = finalHomeQueue,
                                    lockWallpapersInQueue = finalLockQueue
                                ),
                                wallpapers = wallpapers,
                                folders = folders
                            )
                        )
                    }
                    else {
                        albumRepository.cascadeDeleteAlbum(album.album)
                    }
                }
            } catch (e: Exception) {
                Log.e("PaperizeWallpaperChanger", "Error refreshing album", e)
            }
        }
    }

    private fun setWallpaperSafely(bitmap_s: Bitmap?, flag: Int, wallpaperManager: WallpaperManager) {
        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                wallpaperManager.setBitmap(bitmap_s, null, true, flag)
                return
            } catch (e: IOException) {
                if (attempt == maxRetries) {
                    Log.e("Wallpaper", "Final attempt failed: \${e.message}")
                    return
                }
                Thread.sleep(1000L * attempt)
            }
        }
    }
}
