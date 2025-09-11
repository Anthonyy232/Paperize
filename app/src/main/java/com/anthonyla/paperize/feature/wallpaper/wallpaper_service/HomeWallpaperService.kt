package com.anthonyla.paperize.feature.wallpaper.wallpaper_service

import android.Manifest
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
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.*
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import com.anthonyla.paperize.feature.wallpaper.presentation.settings_screen.SettingsState
import com.anthonyla.paperize.feature.wallpaper.tasker_shortcut.triggerWallpaperTaskerEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
    private var isForeground = false

    enum class Actions {
        START,
        UPDATE,
        REFRESH
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handleThread.start()
        workerHandler = Handler(handleThread.looper)
        if (!isForeground) {
            val notification = createInitialNotification()
            startForeground(1, notification)
            isForeground = true
        }
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
                Actions.UPDATE.toString() -> workerTaskUpdate()
                Actions.REFRESH.toString() -> workerTaskRefresh()
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

    private fun createInitialNotification(): android.app.Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val pendingMainActivityIntent = PendingIntent.getActivity(
            this, 3, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, "wallpaper_service_channel").apply {
            setContentTitle(getString(R.string.app_name))
            setContentText(getString(R.string.changing_wallpaper))
            setSmallIcon(R.drawable.notification_icon)
            setContentIntent(pendingMainActivityIntent)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }.build()
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
            shuffle = settingsDataStoreImpl.getBoolean(SettingsConstants.SHUFFLE) ?: true,
            skipLandscape = settingsDataStoreImpl.getBoolean(SettingsConstants.SKIP_LANDSCAPE) ?: false,
            skipNonInteractive = settingsDataStoreImpl.getBoolean(SettingsConstants.SKIP_NON_INTERACTIVE) ?: false
        )
    }

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

            if (settings.skipLandscape && context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                Log.d("PaperizeWallpaperChanger", "Skipping wallpaper change - device is in landscape mode")
                onDestroy()
                return
            }

            if (settings.skipNonInteractive){
                val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
                if (!powerManager.isInteractive){
                    Log.d(
                        "PaperizeWallpaperChanger", "Skipping wallpaper change - device is in non-interactive state")
                    onDestroy()
                    return
                }
            }

            var homeAlbum = selectedAlbum.find { it.album.initialAlbumName == settings.homeAlbumName }
            if (homeAlbum == null) {
                onDestroy()
                return
            }
            when {
                settings.setHome && settings.setLock && scheduleSeparately -> {
                    var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) homeAlbum.totalWallpapers.map { it.wallpaperUri }.shuffled()
                        else homeAlbum.totalWallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            Log.d("PaperizeWallpaperChanger", "No wallpaper found")
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
                settings.setHome && settings.setLock && !scheduleSeparately -> {
                    var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) homeAlbum.totalWallpapers.map { it.wallpaperUri }.shuffled()
                        else homeAlbum.totalWallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            Log.d("PaperizeWallpaperChanger", "No wallpaper found")
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
                settings.setHome -> {
                    var wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) homeAlbum.totalWallpapers.map { it.wallpaperUri }.shuffled()
                        else homeAlbum.totalWallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            Log.d("PaperizeWallpaperChanger", "No wallpaper found")
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
            Log.d("HomeWallpaperService", "Wallpaper change task completed.")
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper (Home)", e)
        }
    }

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
            val currentLockWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_LOCK_WALLPAPER) ?: ""

            val bothEnabled = settings.setHome && settings.setLock && currentHomeWallpaper == currentLockWallpaper

            if (bothEnabled) {
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
                    grayscalePercent = settings.homeGrayscalePercentage,
                    both = true
                )
            } else {
                if (settings.setHome) {
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
                }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in updating", e)
        }
    }

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
                val bitmap = retrieveBitmap(context, wallpaper, size.width, size.height, scaling)
                if (bitmap == null) return false
                else if (wallpaperManager.isSetWallpaperAllowed) {
                    if (both) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val settings = getWallpaperSettings()

                            processBitmap(
                                size.width, size.height, bitmap,
                                settings.darken, settings.homeDarkenPercentage,
                                scaling,
                                settings.blur, settings.homeBlurPercentage,
                                settings.vignette, settings.homeVignettePercentage,
                                settings.grayscale, settings.homeGrayscalePercentage
                            )?.let { homeImage ->
                                setWallpaperSafely(homeImage, WallpaperManager.FLAG_SYSTEM, wallpaperManager)
                            }

                            processBitmap(
                                size.width, size.height, bitmap,
                                settings.darken, settings.lockDarkenPercentage,
                                scaling,
                                settings.blur, settings.lockBlurPercentage,
                                settings.vignette, settings.lockVignettePercentage,
                                settings.grayscale, settings.lockGrayscalePercentage
                            )?.let { lockImage ->
                                setWallpaperSafely(lockImage, WallpaperManager.FLAG_LOCK, wallpaperManager)
                            }
                        }
                    } else {
                        processBitmap(size.width, size.height, bitmap, darken, darkenPercent, scaling, blur, blurPercent, vignette, vignettePercent, grayscale, grayscalePercent)?.let { image ->
                            setWallpaperSafely(image, WallpaperManager.FLAG_SYSTEM, wallpaperManager)
                        }
                    }
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

    private fun refreshAlbum(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("PaperizeWallpaperChanger", "Refreshing albums")
            try {
                val albumsToRefresh = albumRepository.getAlbumsWithWallpaperAndFolder().first()
                val settings = getWallpaperSettings()

                albumsToRefresh.forEach { albumWithDetails ->
                    val validStandaloneWallpapers = albumWithDetails.wallpapers
                        .filter { isValidUri(context, it.wallpaperUri) }

                    val updatedFolders = albumWithDetails.folders
                        .mapNotNull { folder ->
                            try {
                                val metadata = getFolderMetadata(folder.folderUri, context)
                                if (metadata.lastModified != folder.dateModified) {
                                    val wallpapersInDb = albumWithDetails.wallpapers
                                        .filter { folder.wallpaperUris.contains(it.wallpaperUri) }
                                        .filter { isValidUri(context, it.wallpaperUri) }
                                        .associateBy { it.wallpaperUri }

                                    val wallpapersOnDisk = getWallpaperFromFolder(folder.folderUri, context)

                                    val newWallpapers = wallpapersOnDisk
                                        .filterNot { wallpapersInDb.containsKey(it.wallpaperUri) }
                                        .mapIndexed { index, wallpaper ->
                                            wallpaper.copy(
                                                initialAlbumName = albumWithDetails.album.initialAlbumName,
                                                order = (wallpapersInDb.size) + 1 + index,
                                                key = albumWithDetails.album.initialAlbumName.hashCode() +
                                                        folder.folderUri.hashCode() +
                                                        wallpaper.wallpaperUri.hashCode()
                                            )
                                        }

                                    val combinedWallpapers = wallpapersInDb.values.toList() + newWallpapers
                                    val combinedWallpaperUris = combinedWallpapers.map { it.wallpaperUri }.sorted()

                                    folder.copy(
                                        coverUri = combinedWallpapers.firstOrNull()?.wallpaperUri ?: "",
                                        wallpaperUris = combinedWallpaperUris,
                                        dateModified = metadata.lastModified,
                                        folderName = metadata.filename
                                    )
                                } else {
                                    folder
                                }
                            } catch (e: Exception) {
                                Log.w("PaperizeWallpaperChanger", "Failed to process folder ${folder.folderUri}: ${e.message}")
                                null
                            }
                        }

                    val folderWallpapers = updatedFolders.flatMap { folder ->
                        albumWithDetails.wallpapers.filter { folder.wallpaperUris.contains(it.wallpaperUri) }
                    }
                    val allValidWallpapers = (folderWallpapers + validStandaloneWallpapers)
                        .sortedBy { it.order }

                    if (allValidWallpapers.isEmpty()) {
                        Log.d("PaperizeWallpaperChanger", "Album '${albumWithDetails.album.initialAlbumName}' is now empty. Deleting.")
                        albumRepository.cascadeDeleteAlbum(albumWithDetails.album)
                        return@forEach
                    }

                    val allValidUris = allValidWallpapers.map { it.wallpaperUri }.toSet()

                    fun rebuildQueue(currentQueue: List<String>, allWallpapers: List<String>, isShuffle: Boolean): List<String> {
                        val validQueueItems = currentQueue.filter { allValidUris.contains(it) }
                        val newItems = allWallpapers.filterNot { validQueueItems.contains(it) }
                        return if (isShuffle) {
                            validQueueItems + newItems.shuffled()
                        } else {
                            validQueueItems + newItems
                        }
                    }

                    val allValidUrisOrdered = allValidWallpapers.map { it.wallpaperUri }
                    val finalHomeQueue = rebuildQueue(albumWithDetails.album.homeWallpapersInQueue, allValidUrisOrdered, settings.shuffle)
                    val finalLockQueue = rebuildQueue(albumWithDetails.album.lockWallpapersInQueue, allValidUrisOrdered, settings.shuffle)

                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                        albumWithDetails.copy(
                            album = albumWithDetails.album.copy(
                                coverUri = findFirstValidUri(context, updatedFolders, validStandaloneWallpapers),
                                homeWallpapersInQueue = finalHomeQueue,
                                lockWallpapersInQueue = finalLockQueue
                            ),
                            wallpapers = validStandaloneWallpapers,
                            folders = updatedFolders
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("PaperizeWallpaperChanger", "Error refreshing album", e)
            }
        }
    }

    @RequiresPermission(Manifest.permission.SET_WALLPAPER)
    private fun setWallpaperSafely(bitmap_s: Bitmap?, flag: Int, wallpaperManager: WallpaperManager) {
        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                wallpaperManager.setBitmap(bitmap_s, null, true, flag)
                return
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    Log.e("Wallpaper", "Final attempt failed: ${e.message}")
                    return
                }
                Thread.sleep(1000L * attempt)
            }
        }
    }
}