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
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anthonyla.paperize.R
import com.anthonyla.paperize.core.ScalingConstants
import com.anthonyla.paperize.core.SettingsConstants
import com.anthonyla.paperize.core.Type
import com.anthonyla.paperize.core.blurBitmap
import com.anthonyla.paperize.core.calculateInSampleSize
import com.anthonyla.paperize.core.darkenBitmap
import com.anthonyla.paperize.core.fillBitmap
import com.anthonyla.paperize.core.fitBitmap
import com.anthonyla.paperize.core.getImageDimensions
import com.anthonyla.paperize.core.getWallpaperFromFolder
import com.anthonyla.paperize.core.stretchBitmap
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.math.min

/**
 * Service to change lock screen
 */
@AndroidEntryPoint
class LockWallpaperService: Service() {
    private val handleThread = HandlerThread("LockThread")
    private lateinit var workerHandler: Handler
    @Inject lateinit var selectedRepository: SelectedAlbumRepository
    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var settingsDataStoreImpl: SettingsDataStore
    private var scheduleSeparately: Boolean = false
    private var homeInterval: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
    private var lockInterval: Int = SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT
    private var type = Type.SINGLE.ordinal

    enum class Actions {
        START,
        REQUEUE,
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
                Actions.REQUEUE.toString() -> {
                    homeInterval = intent.getIntExtra("homeInterval", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                    lockInterval = intent.getIntExtra("lockInterval", SettingsConstants.WALLPAPER_CHANGE_INTERVAL_DEFAULT)
                    scheduleSeparately = intent.getBooleanExtra("scheduleSeparately", false)
                    workerTaskRequeue()
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
        workerHandler.post {
            CoroutineScope(Dispatchers.IO).launch {
                changeWallpaper(this@LockWallpaperService)
            }
            stopSelf()
        }
    }

    private fun workerTaskRequeue() {
        workerHandler.post {
            CoroutineScope(Dispatchers.IO).launch {
                val nextSetTime1 = LocalDateTime.parse(settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME))
                val nextSetTime2 = LocalDateTime.parse(settingsDataStoreImpl.getString(SettingsConstants.LOCK_NEXT_SET_TIME))
                val nextSetTime = (if (nextSetTime1!!.isBefore(nextSetTime2)) nextSetTime1 else nextSetTime2)
                val notification = createNotification(nextSetTime)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notification?.let { notificationManager.notify(1, it) }
            }
            stopSelf()
        }
    }

    private fun workerTaskUpdate() {
        workerHandler.post {
            CoroutineScope(Dispatchers.IO).launch {
                updateCurrentWallpaper(this@LockWallpaperService)
            }
            stopSelf()
        }
    }

    private fun workerTaskRefresh() {
        workerHandler.post {
            CoroutineScope(Dispatchers.IO).launch {
                refreshAlbum(this@LockWallpaperService)
            }
            stopSelf()
        }
    }

    /**
     * Creates a notification for the wallpaper service
     */
    private fun createNotification(nextSetTime: LocalDateTime?): Notification? {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        if (nextSetTime != null) {
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Builder(this, "wallpaper_service_channel")
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.next_wallpaper_change, nextSetTime.format(formatter)))
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .build()
        }
        return null
    }

    /**
     * Changes the wallpaper to the next wallpaper in the queue of the selected album
     * If none left, reshuffle the wallpapers and pick the first one
     */
    private suspend fun changeWallpaper(context: Context) {
        try {
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
                val darkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100
                val blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false
                val blurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.BLUR_PERCENTAGE) ?: 0
                val lockAlbumName = settingsDataStoreImpl.getString(SettingsConstants.LOCK_ALBUM_NAME) ?: ""
                val homeAlbumName = settingsDataStoreImpl.getString(SettingsConstants.HOME_ALBUM_NAME) ?: ""
                val homeAlbum = selectedAlbum.find { it.album.initialAlbumName == homeAlbumName }
                val lockAlbum = selectedAlbum.find { it.album.initialAlbumName == lockAlbumName }
                if (lockAlbum == null) {
                    onDestroy()
                    return
                }

                when {
                    // Case: Set home and lock screen wallpapers using separate albums (home screen and lock screen album)
                    setHome && setLock && scheduleSeparately -> {
                        var wallpaper = lockAlbum.album.lockWallpapersInQueue.firstOrNull()
                        if (wallpaper == null) {
                            // Reshuffle the wallpapers and pick the first one
                            val newWallpapers = lockAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpapers.firstOrNull()
                            if (wallpaper == null) {
                                // Delete invalid album if no valid wallpaper
                                selectedRepository.cascadeDeleteAlbum(lockAlbum.album.initialAlbumName)
                                onDestroy()
                                return
                            }
                            else {
                                val success = setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = darkenPercentage,
                                    scaling = scaling,
                                    blur = blur,
                                    blurPercent = blurPercentage
                                )
                                if (success) {
                                    selectedRepository.upsertSelectedAlbum(lockAlbum.copy(album = lockAlbum.album.copy(lockWallpapersInQueue = newWallpapers.drop(1))))
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                }
                                else {
                                    val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                    if (wallpaperToDelete != null) {
                                        albumRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.upsertSelectedAlbum(
                                            lockAlbum.copy(
                                                album = lockAlbum.album.copy(
                                                    lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                                ),
                                                wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        else {
                            val success = setWallpaper(
                                context = context,
                                wallpaper = wallpaper.toUri(),
                                darken = darken,
                                darkenPercent = darkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercent = blurPercentage
                            )
                            if (success) {
                                selectedRepository.upsertSelectedAlbum(lockAlbum.copy(album = lockAlbum.album.copy(lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.drop(1))))
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                            }
                            else {
                                val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.upsertSelectedAlbum(
                                        lockAlbum.copy(
                                            album = lockAlbum.album.copy(
                                                lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                            ),
                                            wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                        )
                                    )
                                }
                            }
                        }
                        if (homeInterval != lockInterval) {
                            wallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_HOME_WALLPAPER)
                            if (!wallpaper.isNullOrEmpty()) {
                                setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = darkenPercentage,
                                    scaling = scaling,
                                    blur = blur,
                                    blurPercent = blurPercentage
                                )
                            }
                        }
                    }
                    // Case: Set home and lock screen wallpapers using the same album (home screen album)
                    setHome && setLock && !scheduleSeparately -> {
                        if (homeAlbum == null) {
                            onDestroy()
                            return
                        }
                        val wallpaper = homeAlbum.album.homeWallpapersInQueue.firstOrNull()
                        if (wallpaper != null) {
                            val success = setWallpaper(
                                context = context,
                                wallpaper = wallpaper.toUri(),
                                darken = darken,
                                darkenPercent = darkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercent = blurPercentage
                            )
                            if (success) {
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                            }
                        }
                    }
                    // Case: Set lock screen wallpaper (lock screen album)
                    setLock -> {
                        var wallpaper = lockAlbum.album.lockWallpapersInQueue.firstOrNull()
                        if (wallpaper == null) {
                            // Reshuffle the wallpapers and pick the first one
                            val newWallpapers = lockAlbum.wallpapers.map { it.wallpaperUri }.shuffled()
                            wallpaper = newWallpapers.firstOrNull()
                            if (wallpaper == null) {
                                // Delete invalid album if no valid wallpaper
                                selectedRepository.cascadeDeleteAlbum(lockAlbum.album.initialAlbumName)
                                onDestroy()
                                return
                            }
                            else {
                                val success = setWallpaper(
                                    context = context,
                                    wallpaper = wallpaper.toUri(),
                                    darken = darken,
                                    darkenPercent = darkenPercentage,
                                    scaling = scaling,
                                    blur = blur,
                                    blurPercent = blurPercentage
                                )
                                if (success) {
                                    selectedRepository.upsertSelectedAlbum(lockAlbum.copy(album = lockAlbum.album.copy(lockWallpapersInQueue = newWallpapers.drop(1))))
                                    settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                                }
                                else {
                                    val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                    if (wallpaperToDelete != null) {
                                        albumRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.deleteWallpaper(wallpaperToDelete)
                                        selectedRepository.upsertSelectedAlbum(
                                            lockAlbum.copy(
                                                album = lockAlbum.album.copy(
                                                    lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                                ),
                                                wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        else {
                            val success = setWallpaper(
                                context = context,
                                wallpaper = wallpaper.toUri(),
                                darken = darken,
                                darkenPercent = darkenPercentage,
                                scaling = scaling,
                                blur = blur,
                                blurPercent = blurPercentage
                            )
                            if (success) {
                                selectedRepository.upsertSelectedAlbum(lockAlbum.copy(album = lockAlbum.album.copy(lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.drop(1))))
                                settingsDataStoreImpl.putString(SettingsConstants.CURRENT_LOCK_WALLPAPER, wallpaper.toString())
                            }
                            else {
                                val wallpaperToDelete = lockAlbum.wallpapers.find { it.wallpaperUri == wallpaper }
                                if (wallpaperToDelete != null) {
                                    albumRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.deleteWallpaper(wallpaperToDelete)
                                    selectedRepository.upsertSelectedAlbum(
                                        lockAlbum.copy(
                                            album = lockAlbum.album.copy(
                                                lockWallpapersInQueue = lockAlbum.album.lockWallpapersInQueue.filterNot { it == wallpaper }
                                            ),
                                            wallpapers = lockAlbum.wallpapers.filterNot { it.wallpaperUri == wallpaper }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                // Run notification
                if (homeInterval != lockInterval) {
                    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    val homeNextSetTime = LocalDateTime.parse(settingsDataStoreImpl.getString(SettingsConstants.HOME_NEXT_SET_TIME))
                    val lockNextSetTime: LocalDateTime?
                    val nextSetTime: LocalDateTime?
                    val currentTime = LocalDateTime.now()
                    lockNextSetTime = currentTime.plusMinutes(lockInterval.toLong())
                    nextSetTime = if (lockNextSetTime.isBefore(homeNextSetTime) && lockNextSetTime.isAfter(currentTime)) lockNextSetTime
                    else if (homeNextSetTime.isAfter(currentTime)) homeNextSetTime
                    else currentTime.plusMinutes(lockInterval.toLong())
                    nextSetTime?.let {
                        settingsDataStoreImpl.putString(SettingsConstants.LAST_SET_TIME, currentTime.format(formatter))
                        settingsDataStoreImpl.putString(SettingsConstants.NEXT_SET_TIME, it.format(formatter))
                        settingsDataStoreImpl.putString(SettingsConstants.HOME_NEXT_SET_TIME, homeNextSetTime.toString())
                        settingsDataStoreImpl.putString(SettingsConstants.LOCK_NEXT_SET_TIME, lockNextSetTime.toString())
                    }
                    val notification = createNotification(nextSetTime)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notification?.let { notificationManager.notify(1, it) }
                }
            }
        } catch (e: Exception) {
            Log.e("PaperizeWallpaperChanger", "Error in changing wallpaper", e)
        }
    }

    /**
     * Updates the current wallpaper with current settings
     */
    private suspend fun updateCurrentWallpaper(context: Context) {
        try {
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
                val darkenPercentage = settingsDataStoreImpl.getInt(SettingsConstants.DARKEN_PERCENTAGE) ?: 100
                val blur = settingsDataStoreImpl.getBoolean(SettingsConstants.BLUR) ?: false
                val blurPercentage = settingsDataStoreImpl.getInt(SettingsConstants.BLUR_PERCENTAGE) ?: 0
                val currentLockWallpaper = settingsDataStoreImpl.getString(SettingsConstants.CURRENT_LOCK_WALLPAPER) ?: ""
                setWallpaper(
                    context = context,
                    wallpaper = currentLockWallpaper.toUri(),
                    darken = darken,
                    darkenPercent = darkenPercentage,
                    scaling = scaling,
                    blur = blur,
                    blurPercent = blurPercentage
                )
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
        blur: Boolean = false,
        blurPercent: Int,
    ): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        try {
            val imageSize = wallpaper.getImageDimensions(context) ?: return false
            val aspectRatio = imageSize.height.toFloat() / imageSize.width.toFloat()
            val device = context.resources.displayMetrics
            val targetWidth = min(2 * device.widthPixels, imageSize.width)
            val targetHeight = (targetWidth * aspectRatio).toInt()

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val source = ImageDecoder.createSource(context.contentResolver, wallpaper)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.setTargetSize(targetWidth, targetHeight)
                        decoder.isMutableRequired = true
                    }
                } catch (e: Exception) {
                    context.contentResolver.openInputStream(wallpaper)?.use { inputStream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = calculateInSampleSize(imageSize, targetWidth, targetHeight)
                            inMutable = true
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
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
                    wallpaperManager.setBitmap(image, null, true, WallpaperManager.FLAG_LOCK)
                    wallpaperManager.forgetLoadedWallpaper()
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

    /**
     * Refreshes the album by deleting invalid wallpapers and updating folder cover uri and wallpapers uri-
     */
    private fun refreshAlbum(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
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
                        } catch (e: Exception) {
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
        }
    }
}