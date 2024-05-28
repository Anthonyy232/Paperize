package com.anthonyla.paperize.feature.wallpaper.wallpaper_service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.blurBitmap
import com.anthonyla.paperize.core.calculateInSampleSize
import com.anthonyla.paperize.core.darkenBitmap
import com.anthonyla.paperize.core.fillBitmap
import com.anthonyla.paperize.core.fitBitmap
import com.anthonyla.paperize.core.getImageDimensions
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.core.stretchBitmap
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.model.SelectedAlbum
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.math.min


@AndroidEntryPoint
class WallpaperService: Service() {
    @Inject lateinit var selectedRepository: SelectedAlbumRepository
    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    private val handler1 = Handler(Looper.getMainLooper())
    private val handler2 = Handler(Looper.getMainLooper())
    private lateinit var runnableCode1: Runnable
    private lateinit var runnableCode2: Runnable
    private var timeInMinutes1: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
    private var timeInMinutes2: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
    private var nextSetTime1: LocalDateTime? = null
    private var nextSetTime2: LocalDateTime? = null
    private var scheduleSeparately: Boolean = false
    private var lastRan1: LocalDateTime? = null
    private var lastRan2: LocalDateTime? = null
    private var refresherTimer = LocalDateTime.now()

    enum class Actions {
        START,
        REQUEUE,
        STOP,
        UPDATE
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Set up the runnable code to change the wallpaper to prevent runnableCode from being null
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        runnableCode1 = object : Runnable {
            override fun run() {
                val self = this
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                            refreshAlbum(this@WallpaperService)
                            refresherTimer = LocalDateTime.now()
                            delay(5000)
                        }
                        changeWallpaper(this@WallpaperService, true)
                    } catch (e: Exception) {
                        Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                    } finally {
                        handler1.postDelayed(self, timeInMinutes1 * 60 * 1000L)
                    }
                }
            }
        }
        runnableCode2 = object : Runnable {
            override fun run() {
                val self = this
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                            refreshAlbum(this@WallpaperService)
                            refresherTimer = LocalDateTime.now()
                            delay(5000)
                        }
                        changeWallpaper(this@WallpaperService, false)
                    } catch (e: Exception) {
                        Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                    } finally {
                        handler2.postDelayed(self, timeInMinutes2 * 60 * 1000L + 10000)
                    }
                }
            }
        }
    }

    /**
     * Start the service and schedule the wallpaper change
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            Actions.START.toString() -> {
                handler1.removeCallbacks(runnableCode1)
                handler2.removeCallbacks(runnableCode2)
                timeInMinutes1 = intent.getIntExtra("timeInMinutes1", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                timeInMinutes2 = intent.getIntExtra("timeInMinutes2", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                scheduleSeparately = intent.getBooleanExtra("scheduleSeparately", false)
                nextSetTime1 = null
                nextSetTime2 = null
                lastRan1 = null
                lastRan2 = null
                refresherTimer = LocalDateTime.now()

                if (!scheduleSeparately) {
                    runnableCode1 = object : Runnable {
                        override fun run() {
                            val self = this
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                                        refreshAlbum(this@WallpaperService)
                                        refresherTimer = LocalDateTime.now()
                                        delay(5000)
                                    }
                                    changeWallpaper(this@WallpaperService, null)
                                } catch (e: Exception) {
                                    Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                                } finally {
                                    handler1.postDelayed(self, timeInMinutes1 * 60 * 1000L)
                                }
                            }
                        }
                    }
                    handler1.postDelayed(runnableCode1, 1000)
                }
                else {
                    runnableCode1 = object : Runnable {
                        override fun run() {
                            val self = this
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                                        refreshAlbum(this@WallpaperService)
                                        refresherTimer = LocalDateTime.now()
                                        delay(5000)
                                    }
                                    changeWallpaper(this@WallpaperService, true)
                                } catch (e: Exception) {
                                    Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                                } finally {
                                    handler1.postDelayed(self, timeInMinutes1 * 60 * 1000L)
                                }
                            }
                        }
                    }
                    handler1.postDelayed(runnableCode1, 1000)
                    runnableCode2 = object : Runnable {
                        override fun run() {
                            val self = this
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                                        refreshAlbum(this@WallpaperService)
                                        refresherTimer = LocalDateTime.now()
                                        delay(5000)
                                    }
                                    changeWallpaper(this@WallpaperService, false)
                                } catch (e: Exception) {
                                    Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                                } finally {
                                    handler2.postDelayed(self, timeInMinutes2 * 60 * 1000L  + 10000)
                                }
                            }
                        }
                    }
                    handler2.postDelayed(runnableCode2, 5000)
                }
            }
            Actions.REQUEUE.toString() -> {
                handler1.removeCallbacks(runnableCode1)
                handler2.removeCallbacks(runnableCode2)
                timeInMinutes1 = intent.getIntExtra("timeInMinutes1", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                timeInMinutes2 = intent.getIntExtra("timeInMinutes2", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                scheduleSeparately = intent.getBooleanExtra("scheduleSeparately", false)
                nextSetTime1 = null
                nextSetTime2 = null
                lastRan1 = null
                lastRan2 = null
                refresherTimer = LocalDateTime.now()

                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                val currentTime = LocalDateTime.now()
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                    if (scheduleSeparately) {
                        nextSetTime1 = currentTime.plusMinutes(timeInMinutes1.toLong())
                        nextSetTime2 = currentTime.plusMinutes(timeInMinutes2.toLong())
                        val earliestTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, earliestTime)
                    }
                    else {
                        nextSetTime1 = currentTime.plusMinutes(timeInMinutes1.toLong())
                        nextSetTime2 = nextSetTime1
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, currentTime.plusMinutes(timeInMinutes1.toLong()).format(formatter))
                    }
                }

                val notification = createNotification()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, notification)
                if (!scheduleSeparately) {
                    runnableCode1 = object : Runnable {
                        override fun run() {
                            val self = this
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                                        refreshAlbum(this@WallpaperService)
                                        refresherTimer = LocalDateTime.now()
                                        delay(5000)
                                    }
                                    changeWallpaper(this@WallpaperService, null)
                                } catch (e: Exception) {
                                    Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                                } finally {
                                    handler1.postDelayed(self, timeInMinutes1 * 60 * 1000L)
                                }
                            }
                        }
                    }
                    handler1.postDelayed(runnableCode1, timeInMinutes1 * 60 * 1000L)
                }
                else {
                    runnableCode1 = object : Runnable {
                        override fun run() {
                            val self = this
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                                        refreshAlbum(this@WallpaperService)
                                        refresherTimer = LocalDateTime.now()
                                        delay(5000)
                                    }
                                    changeWallpaper(this@WallpaperService, true)
                                } catch (e: Exception) {
                                    Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                                } finally {
                                    handler1.postDelayed(self, timeInMinutes1 * 60 * 1000L)
                                }
                            }
                        }
                    }
                    handler1.postDelayed(runnableCode1, timeInMinutes1 * 60 * 1000L)
                    runnableCode2 = object : Runnable {
                        override fun run() {
                            val self = this
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (LocalDateTime.now().minusDays(1).isAfter(refresherTimer)) {
                                        refreshAlbum(this@WallpaperService)
                                        refresherTimer = LocalDateTime.now()
                                        delay(5000)
                                    }
                                    changeWallpaper(this@WallpaperService, false)
                                } catch (e: Exception) {
                                    Log.e("PaperizeWallpaperChanger", "Error in runnableCode", e)
                                } finally {
                                    handler2.postDelayed(self, timeInMinutes2 * 60 * 1000L + 10000)
                                }
                            }
                        }
                    }
                    handler2.postDelayed(runnableCode2, timeInMinutes2 * 60 * 1000L + 10000)
                }
            }
            Actions.UPDATE.toString() -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateCurrentWallpaper(this@WallpaperService)
                    } catch (e: Exception) {
                        Log.e("PaperizeWallpaperChanger", "Error in changing brightness", e)
                    }
                }
            }
            Actions.STOP.toString() -> {
                handler1.removeCallbacks(runnableCode1)
                handler2.removeCallbacks(runnableCode2)
                CoroutineScope(Dispatchers.IO).launch {
                    settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, "")
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, "")
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    /**
     * Clean up the service when it is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        handler1.removeCallbacks(runnableCode1)
        handler2.removeCallbacks(runnableCode2)
        CoroutineScope(Dispatchers.IO).launch {
            settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, "")
            settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, "")
        }
    }

    /**
     * Creates a notification for the wallpaper service
     */
    private fun createNotification(): Notification {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        var earliestTime = when {
            nextSetTime1 != null && nextSetTime2 != null -> (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)!!.format(formatter)
            nextSetTime1 != null -> nextSetTime1!!.format(formatter)
            nextSetTime2 != null -> nextSetTime2!!.format(formatter)
            else -> LocalDateTime.now().format(formatter)
        }
        if (earliestTime != null) { // Edge case where numbers like 2131689515 pass through
            if (earliestTime.length <= 10) {
                earliestTime = LocalDateTime.now().format(formatter)
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, "wallpaper_service_channel")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.next_wallpaper_change, earliestTime))
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context, setHomeOrLock: Boolean? = null) {
        try {
            val selectedAlbum = selectedRepository.getSelectedAlbum().first().firstOrNull()
            if (selectedAlbum == null) {
                onDestroy()
                return
            }
            else {
                val toggled = settingsDataStoreImpl.getBoolean(SettingsConstants.ENABLE_CHANGER) ?: false
                val setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.HOME_WALLPAPER) ?: false
                val setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.LOCK_WALLPAPER) ?: false
                if (!toggled || (!setHome && !setLock)) {
                    onDestroy()
                    return
                }
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                val currentTime = LocalDateTime.now()
                settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))

                if (setHomeOrLock == null) {
                    nextSetTime1 = currentTime.plusMinutes(timeInMinutes1.toLong())
                    nextSetTime2 = nextSetTime1
                    settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, currentTime.plusMinutes(timeInMinutes1.toLong()).format(formatter))
                }
                else {
                    if (setHomeOrLock) { nextSetTime1 = currentTime.plusMinutes(timeInMinutes1.toLong()) }
                    else { nextSetTime2 = currentTime.plusMinutes(timeInMinutes2.toLong()) }
                    if (nextSetTime1 == null && nextSetTime2 != null) {
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime2!!.format(formatter))
                    }
                    else if (nextSetTime1 != null && nextSetTime2 == null) {
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, nextSetTime1!!.format(formatter))
                    }
                    else {
                        val earliestTime = if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, earliestTime!!.format(formatter))
                    }
                }

                val notification = createNotification()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, notification)

                val scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL
                val darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false
                val darkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100
                val blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false
                val blurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.BLUR_PERCENTAGE) ?: 0

                selectedAlbum.let { it ->
                    if (setHomeOrLock != null) {
                        var wallpaper = if (setHomeOrLock) it.album.homeWallpapersInQueue.firstOrNull() else it.album.lockWallpapersInQueue.firstOrNull()
                        if (wallpaper != null) {
                            if (!setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = darkenPercentage,
                                    scaling = scaling,
                                    setHome = setHome,
                                    setLock = setLock,
                                    setLockOrHome = setHomeOrLock,
                                    blur = blur,
                                    blurPercent = blurPercentage
                                )) {
                                selectedAlbum.wallpapers.firstOrNull { it.wallpaperUri == wallpaper }
                                    ?.let { it1 ->
                                        selectedRepository.deleteWallpaper(it1)
                                        selectedRepository.upsertSelectedAlbum(
                                            it.copy(
                                                album = it.album.copy(
                                                    homeWallpapersInQueue = if (setHomeOrLock) it.album.homeWallpapersInQueue.drop(1) else it.album.homeWallpapersInQueue,
                                                    lockWallpapersInQueue = if (!setHomeOrLock) it.album.lockWallpapersInQueue.drop(1) else it.album.lockWallpapersInQueue,
                                                    currentHomeWallpaper = if (setHomeOrLock) wallpaper else it.album.currentHomeWallpaper,
                                                    currentLockWallpaper = if (!setHomeOrLock) wallpaper else it.album.currentLockWallpaper
                                                ),
                                                wallpapers = it.wallpapers.filter { it == it1 },
                                            )
                                        )
                                        albumRepository.deleteWallpaper(it1)
                                    }
                            } else {
                                selectedRepository.upsertSelectedAlbum(
                                    it.copy(
                                        album = it.album.copy(
                                            homeWallpapersInQueue = if (setHomeOrLock) it.album.homeWallpapersInQueue.drop(1) else it.album.homeWallpapersInQueue,
                                            lockWallpapersInQueue = if (!setHomeOrLock) it.album.lockWallpapersInQueue.drop(1) else it.album.lockWallpapersInQueue,
                                            currentHomeWallpaper = if (setHomeOrLock) wallpaper else it.album.currentHomeWallpaper,
                                            currentLockWallpaper = if (!setHomeOrLock) wallpaper else it.album.currentLockWallpaper
                                        )
                                    )
                                )
                            }
                        }
                        // No more wallpapers in the queue -- reshuffle the wallpapers
                        else {
                            val newWallpaperInQueue = it.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpaperInQueue.firstOrNull()
                            if (wallpaper != null) {
                                if (!setWallpaper(
                                        context = context,
                                        wallpaper = wallpaper.toUri(),
                                        darken = darken,
                                        darkenPercent = darkenPercentage,
                                        scaling = scaling,
                                        setHome = setHome,
                                        setLock = setLock,
                                        setLockOrHome = setHomeOrLock,
                                        blur = blur,
                                        blurPercent = blurPercentage
                                    )
                                ) {
                                    selectedAlbum.wallpapers.firstOrNull { it.wallpaperUri == wallpaper }
                                        ?.let { it1 ->
                                            selectedRepository.deleteWallpaper(it1)
                                            selectedRepository.upsertSelectedAlbum(
                                                it.copy(
                                                    album = it.album.copy(
                                                        homeWallpapersInQueue = if (setHomeOrLock) newWallpaperInQueue.drop(1) else it.album.homeWallpapersInQueue,
                                                        lockWallpapersInQueue = if (!setHomeOrLock) newWallpaperInQueue.drop(1) else it.album.lockWallpapersInQueue,
                                                        currentHomeWallpaper = if (setHomeOrLock) wallpaper else it.album.currentHomeWallpaper,
                                                        currentLockWallpaper = if (!setHomeOrLock) wallpaper else it.album.currentLockWallpaper
                                                    ),
                                                    wallpapers = it.wallpapers.filter { it == it1 }
                                                )
                                            )
                                            albumRepository.deleteWallpaper(it1)
                                        }
                                } else {
                                    selectedRepository.upsertSelectedAlbum(
                                        it.copy(
                                            album = it.album.copy(
                                                homeWallpapersInQueue = if (setHomeOrLock) newWallpaperInQueue.drop(1) else it.album.homeWallpapersInQueue,
                                                lockWallpapersInQueue = if (!setHomeOrLock) newWallpaperInQueue.drop(1) else it.album.lockWallpapersInQueue,
                                                currentHomeWallpaper = if (setHomeOrLock) wallpaper else it.album.currentHomeWallpaper,
                                                currentLockWallpaper = if (!setHomeOrLock) wallpaper else it.album.currentLockWallpaper
                                            )
                                        )
                                    )
                                }
                            } else { onDestroy() }
                        }
                    }
                    else {
                        var wallpaper = it.album.homeWallpapersInQueue.firstOrNull()
                        if (wallpaper != null) {
                            if (!setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = darkenPercentage,
                                    scaling = scaling,
                                    setHome = setHome,
                                    setLock = setLock,
                                    blur = blur,
                                    blurPercent = blurPercentage
                                )) {
                                selectedAlbum.wallpapers.firstOrNull { it.wallpaperUri == wallpaper }
                                    ?.let { it1 ->
                                        selectedRepository.deleteWallpaper(it1)
                                        selectedRepository.upsertSelectedAlbum(
                                            it.copy(
                                                album = it.album.copy(
                                                    homeWallpapersInQueue = it.album.homeWallpapersInQueue.drop(1),
                                                    lockWallpapersInQueue = it.album.lockWallpapersInQueue.filter { it != wallpaper },
                                                    currentHomeWallpaper = null,
                                                    currentLockWallpaper = null
                                                ),
                                                wallpapers = it.wallpapers.filter { it == it1 },
                                            )
                                        )
                                        albumRepository.deleteWallpaper(it1)
                                    }
                            } else {
                                selectedRepository.upsertSelectedAlbum(
                                    it.copy(
                                        album = it.album.copy(
                                            homeWallpapersInQueue = it.album.homeWallpapersInQueue.drop(1),
                                            lockWallpapersInQueue = it.album.lockWallpapersInQueue.filter { it != wallpaper },
                                            currentHomeWallpaper = wallpaper,
                                            currentLockWallpaper = wallpaper
                                        )
                                    )
                                )
                            }
                        }
                        // No more wallpapers in the queue -- reshuffle the wallpapers
                        else {
                            val newWallpaperInQueue = it.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpaperInQueue.firstOrNull()
                            if (wallpaper != null) {
                                if (!setWallpaper(
                                        context = context,
                                        wallpaper = wallpaper.toUri(),
                                        darken = darken,
                                        darkenPercent = darkenPercentage,
                                        scaling = scaling,
                                        setHome = setHome,
                                        setLock = setLock,
                                        setLockOrHome = setHomeOrLock,
                                        blur = blur,
                                        blurPercent = blurPercentage
                                    )
                                ) {
                                    selectedAlbum.wallpapers.firstOrNull { it.wallpaperUri == wallpaper }
                                        ?.let { it1 ->
                                            selectedRepository.deleteWallpaper(it1)
                                            selectedRepository.upsertSelectedAlbum(
                                                it.copy(
                                                    album = it.album.copy(
                                                        homeWallpapersInQueue = it.album.homeWallpapersInQueue.drop(1),
                                                        lockWallpapersInQueue = it.album.lockWallpapersInQueue.filter { it != wallpaper },
                                                        currentHomeWallpaper = null,
                                                        currentLockWallpaper = null
                                                    ),
                                                    wallpapers = it.wallpapers.filter { it == it1 }
                                                )
                                            )
                                            albumRepository.deleteWallpaper(it1)
                                        }
                                } else {
                                    selectedRepository.upsertSelectedAlbum(
                                        it.copy(
                                            album = it.album.copy(
                                                homeWallpapersInQueue = newWallpaperInQueue.drop(1),
                                                currentHomeWallpaper = wallpaper,
                                                currentLockWallpaper = wallpaper
                                            )
                                        )
                                    )
                                }
                            } else { onDestroy() }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper", e)
        }
    }

    private suspend fun updateCurrentWallpaper(context: Context) {
        try {
            val selectedAlbum = selectedRepository.getSelectedAlbum().first().firstOrNull()
            if (selectedAlbum == null) {
                onDestroy()
                return
            }
            else {
                val setHome = settingsDataStoreImpl.getBoolean(SettingsConstants.HOME_WALLPAPER) ?: false
                val setLock = settingsDataStoreImpl.getBoolean(SettingsConstants.LOCK_WALLPAPER) ?: false
                if (!setHome && !setLock) {
                    onDestroy()
                    return
                }
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                val time = LocalDateTime.now()
                settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, time.format(formatter))
                settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, time.plusMinutes(timeInMinutes1.toLong()).format(formatter))
                val scaling = settingsDataStoreImpl.getString(SettingsConstants.WALLPAPER_SCALING)?.let { ScalingConstants.valueOf(it) } ?: ScalingConstants.FILL
                val darken = settingsDataStoreImpl.getBoolean(SettingsConstants.DARKEN) ?: false
                val darkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100
                val blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false
                val blurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.BLUR_PERCENTAGE) ?: 0

                selectedAlbum.let { it ->
                    val wallpaper1 = it.album.currentHomeWallpaper
                    val wallpaper2 = it.album.currentLockWallpaper
                    if (wallpaper1 != null) {
                        if (!setWallpaper(
                                context = context,
                                wallpaper = wallpaper1.toUri(),
                                darken = darken,
                                darkenPercent = darkenPercentage,
                                scaling = scaling,
                                setHome = setHome,
                                setLock = setLock,
                                setLockOrHome = true,
                                blur = blur,
                                blurPercent = blurPercentage
                        )) {
                            selectedAlbum.wallpapers.firstOrNull{ it.wallpaperUri == wallpaper1 }
                                ?.let { it1 ->
                                    selectedRepository.deleteWallpaper(it1)
                                    selectedRepository.upsertSelectedAlbum(
                                        it.copy(
                                            album = it.album.copy(
                                                homeWallpapersInQueue = it.album.homeWallpapersInQueue.filter { it != wallpaper1 },
                                                lockWallpapersInQueue = it.album.lockWallpapersInQueue.filter { it != wallpaper1 },
                                                currentHomeWallpaper = null
                                            ),
                                            wallpapers = it.wallpapers.filter { it == it1 },
                                        )
                                    )
                                    albumRepository.deleteWallpaper(it1)
                                }
                        }
                    }
                    if (wallpaper2 != null) {
                        if (!setWallpaper(
                                context = context,
                                wallpaper = wallpaper2.toUri(),
                                darken = darken,
                                darkenPercent = darkenPercentage,
                                scaling = scaling,
                                setHome = setHome,
                                setLock = setLock,
                                setLockOrHome = false,
                                blur = blur,
                                blurPercent = blurPercentage
                            )) {
                            selectedAlbum.wallpapers.firstOrNull{ it.wallpaperUri == wallpaper2 }
                                ?.let { it1 ->
                                    selectedRepository.deleteWallpaper(it1)
                                    selectedRepository.upsertSelectedAlbum(
                                        it.copy(
                                            album = it.album.copy(
                                                homeWallpapersInQueue = it.album.homeWallpapersInQueue.filter { it != wallpaper1 },
                                                lockWallpapersInQueue = it.album.lockWallpapersInQueue.filter { it != wallpaper1 },
                                                currentLockWallpaper = null
                                            ),
                                            wallpapers = it.wallpapers.filter { it == it1 },
                                        )
                                    )
                                    albumRepository.deleteWallpaper(it1)
                                }
                        }
                    }
                    if (wallpaper1 != null || wallpaper2 != null) {
                        val notification = createNotification()
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, notification)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in updating", e)
        }
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
        setHome: Boolean, setLock: Boolean,
        setLockOrHome: Boolean? = null,
        blur: Boolean = false,
        blurPercent: Int
    ): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val imageSize = wallpaper.getImageDimensions(context) ?: return false
            val aspectRatio = imageSize.height.toFloat() / imageSize.width.toFloat()
            val device = context.resources.displayMetrics
            val targetWidth = min(4 * device.widthPixels, imageSize.width)
            val targetHeight = (targetWidth * aspectRatio).toInt()

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSize(targetWidth, targetHeight)
                    decoder.isMutableRequired = true
                }
            }
            else {
                context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(imageSize, targetWidth, targetHeight)
                        inMutable = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }

            if (bitmap == null) return false
            else {
                processBitmap(device, bitmap, darken, darkenPercent, scaling, blur, blurPercent)?.let { image ->
                    when {
                        setLockOrHome == true -> wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_SYSTEM)
                        setLockOrHome == false -> wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_LOCK)
                        setHome && setLock -> wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM)
                        setHome -> wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_SYSTEM)
                        setLock -> wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_LOCK)
                        else -> {}
                    }
                    image.recycle()
                }
                bitmap.recycle()
                return true
            }
        } catch (e: IOException) {
            Log.e("PaperizeWallpaperChanger", "Error setting wallpaper", e)
            return false
        }
    }

    /**
     * Darkens the bitmap by the given percentage and returns it
     * 0 - lightest, 100 - darkest
     */
    private fun processBitmap(
        device: DisplayMetrics,
        source: Bitmap, darken: Boolean,
        darkenPercent: Int,
        scaling: ScalingConstants,
        blur: Boolean,
        blurPercent: Int
    ): Bitmap? {
        try {
            var processedBitmap = source

            // Apply wallpaper scaling effects
            processedBitmap = when (scaling) {
                ScalingConstants.FILL -> fillBitmap(processedBitmap, device.widthPixels, device.heightPixels)
                ScalingConstants.FIT -> fitBitmap(processedBitmap, device.widthPixels, device.heightPixels)
                ScalingConstants.STRETCH -> stretchBitmap(processedBitmap, device.widthPixels, device.heightPixels)
            }

            // Apply brightness effect
            if (darken && darkenPercent < 100) {
                processedBitmap = darkenBitmap(processedBitmap, darkenPercent)
            }

            // Apply blur effect
            if (blur && blurPercent > 0) {
                processedBitmap = blurBitmap(processedBitmap, blurPercent)
            }
            return processedBitmap
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error darkening bitmap", e)
            return null
        }
    }

    private fun refreshAlbum(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var albumWithWallpapers = albumRepository.getAlbumsWithWallpaperAndFolder().first()
                albumWithWallpapers.forEach { albumWithWallpaper ->
                    // Delete wallpaper if the URI is invalid
                    val invalidWallpapers = albumWithWallpaper.wallpapers.filterNot { wallpaper ->
                        val file = DocumentFile.fromSingleUri(context, wallpaper.wallpaperUri.toUri())
                        file?.exists() == true
                    }
                    if (invalidWallpapers.isNotEmpty()) {
                        albumRepository.deleteWallpaperList(invalidWallpapers)
                    }

                    // Update folder wallpapers
                    albumWithWallpaper.folders.forEach { folder ->
                        DocumentFileCompat.fromTreeUri(context, folder.folderUri.toUri())?.let { folderDirectory ->
                            if (!folderDirectory.isDirectory()) {
                                albumRepository.deleteFolder(folder)
                            } else {
                                val wallpapers = getWallpaperFromFolder(folder.folderUri, context)
                                albumRepository.updateFolder(folder.copy(wallpapers = wallpapers))
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
                            else {
                                val newSelectedAlbum = SelectedAlbum(
                                    album = foundAlbum.album.copy(
                                        homeWallpapersInQueue = wallpapersUri.shuffled(),
                                        lockWallpapersInQueue = wallpapersUri.shuffled(),
                                        currentHomeWallpaper = selectedAlbum.album.currentHomeWallpaper,
                                        currentLockWallpaper = selectedAlbum.album.currentLockWallpaper,
                                    ),
                                    wallpapers = wallpapers
                                )
                                selectedRepository.upsertSelectedAlbum(newSelectedAlbum)
                            }
                        } ?: run { onDestroy() }
                }
            } catch (e: Exception) {
                Log.e("PaperizeWallpaperChanger", "Error refreshing album", e)
            }
        }
    }
}