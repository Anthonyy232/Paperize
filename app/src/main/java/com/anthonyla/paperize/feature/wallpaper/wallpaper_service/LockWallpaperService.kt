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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class LockWallpaperService: Service() {
    private val handleThread = HandlerThread("LockThread")
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
        UPDATE
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
            changeWallpaper(this@LockWallpaperService)
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    private fun workerTaskUpdate() {
        CoroutineScope(Dispatchers.Default).launch {
            updateCurrentWallpaper(this@LockWallpaperService)
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
            skipLandscape = settingsDataStoreImpl.getBoolean(SettingsConstants.SKIP_LANDSCAPE) ?: false
        )
    }

    private suspend fun changeWallpaper(context: Context) {
        try {
            val selectedAlbum = albumRepository.getSelectedAlbums().first()
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

            val lockAlbum = selectedAlbum.find { it.album.initialAlbumName == settings.lockAlbumName }
            val homeAlbum = selectedAlbum.find { it.album.initialAlbumName == settings.homeAlbumName }
            if (lockAlbum == null || homeAlbum == null) {
                onDestroy()
                return
            }
            when {
                settings.setHome && settings.setLock && scheduleSeparately -> {
                    var wallpaper = lockAlbum.album.lockWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) lockAlbum.totalWallpapers.map { it.wallpaperUri }.shuffled()
                        else lockAlbum.totalWallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            Log.d("PaperizeWallpaperChanger", "No wallpaper found")
                            albumRepository.cascadeDeleteAlbum(lockAlbum.album)
                            onDestroy()
                            return
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                albumRepository.upsertAlbum(lockAlbum.album.copy(lockWallpapersInQueue = newWallpapers.drop(1)))
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                    darken = settings.darken,
                                    darkenPercent = settings.lockDarkenPercentage,
                                    scaling = settings.scaling,
                                    blur = settings.blur,
                                    blurPercent = settings.lockBlurPercentage,
                                    vignette = settings.vignette,
                                    vignettePercent = settings.lockVignettePercentage,
                                    grayscale = settings.grayscale,
                                    grayscalePercent = settings.lockGrayscalePercentage
                                )
                            }
                            else {
                                val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                                        lockAlbum.copy(
                                            wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper },
                                            album = lockAlbum.album.copy(
                                                lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper },
                                                homeWallpapersInQueue = lockAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
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
                            albumRepository.upsertAlbum(lockAlbum.album.copy(lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.drop(1)))
                            setWallpaper(
                                context = context,
                                wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                darken = settings.darken,
                                darkenPercent = settings.lockDarkenPercentage,
                                scaling = settings.scaling,
                                blur = settings.blur,
                                blurPercent = settings.lockBlurPercentage,
                                vignette = settings.vignette,
                                vignettePercent = settings.lockVignettePercentage,
                                grayscale = settings.grayscale,
                                grayscalePercent = settings.lockGrayscalePercentage
                            )
                        }
                        else {
                            val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                            if (wallpaperToDelete != null) {
                                albumRepository.deleteWallpaper(wallpaperToDelete)
                                albumRepository.upsertAlbumWithWallpaperAndFolder(
                                    lockAlbum.copy(
                                        album = lockAlbum.album.copy(
                                            lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper },
                                            homeWallpapersInQueue = lockAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                        ),
                                        wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                    )
                                )
                            }
                        }
                    }
                }
                settings.setHome && settings.setLock && !scheduleSeparately -> { /* handled by home wallpaper service */ return }
                settings.setLock -> {
                    var wallpaper = lockAlbum.album.lockWallpapersInQueue.firstOrNull()
                    if (wallpaper == null) {
                        val newWallpapers = if (settings.shuffle) lockAlbum.totalWallpapers.map { it.wallpaperUri }.shuffled()
                        else lockAlbum.totalWallpapers.map { it.wallpaperUri }
                        wallpaper = newWallpapers.firstOrNull()
                        if (wallpaper == null) {
                            Log.d("PaperizeWallpaperChanger", "No wallpaper found")
                            albumRepository.cascadeDeleteAlbum(lockAlbum.album)
                            onDestroy()
                            return
                        }
                        else {
                            val success = isValidUri(context, wallpaper)
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_HOME_WALLPAPER, wallpaper.toString())
                                albumRepository.upsertAlbum(lockAlbum.album.copy(lockWallpapersInQueue = newWallpapers.drop(1)))
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                    darken = settings.darken,
                                    darkenPercent = settings.lockDarkenPercentage,
                                    scaling = settings.scaling,
                                    blur = settings.blur,
                                    blurPercent = settings.lockBlurPercentage,
                                    vignette = settings.vignette,
                                    vignettePercent = settings.lockVignettePercentage,
                                    grayscale = settings.grayscale,
                                    grayscalePercent = settings.lockGrayscalePercentage
                                )
                            }
                            else {
                                val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    albumRepository.upsertAlbumWithWallpaperAndFolder(
                                        lockAlbum.copy(
                                            album = lockAlbum.album.copy(
                                                lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper },
                                                homeWallpapersInQueue = lockAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                            ),
                                            wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
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
                            albumRepository.upsertAlbum(lockAlbum.album.copy(lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.drop(1)))
                            setWallpaper(
                                context = context,
                                wallpaper = wallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                                darken = settings.darken,
                                darkenPercent = if (settings.setHome) settings.homeDarkenPercentage else settings.lockDarkenPercentage,
                                scaling = settings.scaling,
                                blur = settings.blur,
                                blurPercent = if (settings.setHome) settings.homeBlurPercentage else settings.lockBlurPercentage,
                                vignette = settings.vignette,
                                vignettePercent = if (settings.setHome) settings.homeVignettePercentage else settings.lockVignettePercentage,
                                grayscale = settings.grayscale,
                                grayscalePercent = if (settings.setHome) settings.homeGrayscalePercentage else settings.lockGrayscalePercentage
                            )
                        } else {
                            val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                            if (wallpaperToDelete != null) {
                                albumRepository.deleteWallpaper(wallpaperToDelete)
                                albumRepository.upsertAlbumWithWallpaperAndFolder(
                                    lockAlbum.copy(
                                        album = lockAlbum.album.copy(
                                            lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper },
                                            homeWallpapersInQueue = lockAlbum.album.homeWallpapersInQueue.filterNot { it == wallpaper }
                                        ),
                                        wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                    )
                                )

                            }
                        }
                    }
                }
            }
            Log.d("LockWallpaperService", "Wallpaper change task completed.")
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper (Lock)", e)
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

            val currentLockWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_LOCK_WALLPAPER) ?: ""
            setWallpaper(
                context = context,
                wallpaper = currentLockWallpaper.decompress("content://com.android.externalstorage.documents/").toUri(),
                darken = settings.darken,
                darkenPercent = if (settings.setHome) settings.homeDarkenPercentage else settings.lockDarkenPercentage,
                scaling = settings.scaling,
                blur = settings.blur,
                blurPercent = if (settings.setHome) settings.homeBlurPercentage else settings.lockBlurPercentage,
                vignette = settings.vignette,
                vignettePercent = if (settings.setHome) settings.homeVignettePercentage else settings.lockVignettePercentage,
                grayscale = settings.grayscale,
                grayscalePercent = if (settings.setHome) settings.homeGrayscalePercentage else settings.lockGrayscalePercentage
            )
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
                val wallpaperManager = WallpaperManager.getInstance(this.applicationContext)
                val size = getDeviceScreenSize(context)
                val bitmap = retrieveBitmap(context, wallpaper, size.width, size.height, scaling)
                if (bitmap == null) return false
                else if (wallpaperManager.isSetWallpaperAllowed) {
                    processBitmap(size.width, size.height, bitmap, darken, darkenPercent, scaling, blur, blurPercent, vignette, vignettePercent, grayscale, grayscalePercent)?.let { image ->
                        setWallpaperSafely(image, WallpaperManager.FLAG_LOCK, wallpaperManager)
                    }
                    context.triggerWallpaperTaskerEvent()
                    return true
                }
                else return false
            } catch (e: IOException) {
                Log.e("PaperizeWallpaperChanger", "Error setting wallpaper", e)
                return false
            }
        }
        return false
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