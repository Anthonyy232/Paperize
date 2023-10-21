package com.anthonyla.paperize.feature.wallpaper.presentation.album.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.anthonyla.paperize.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.AccessController.getContext


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperItem(
    wallpaperUri: String,
    itemSelected: Boolean,
    selectionMode: Boolean,
    onActivateSelectionMode: (Boolean) -> Unit,
    onItemSelection: () -> Unit,
    onWallpaperViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val transition = updateTransition(itemSelected, label = "")

    val paddingTransition by transition.animateDp(label = "") { selected ->
        if (selected) 5.dp else 0.dp
    }
    val roundedCornerShapeTransition by transition.animateDp(label = "") { selected ->
        if (selected) 24.dp else 16.dp
    }


    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(
        LocalContext.current.contentResolver.openInputStream(wallpaperUri.toUri()),
        null,
        options
    )
    val imageHeight = options.outHeight
    val imageWidth = options.outWidth
    val aspectRatio = (imageWidth.toFloat()/imageHeight.toFloat())

    Box(
        modifier = modifier
            .padding(paddingTransition)
            .clip(RoundedCornerShape(roundedCornerShapeTransition))
    ) {
        AsyncImage (
            model = wallpaperUri.toUri(),
            contentDescription = wallpaperUri,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .combinedClickable(
                    /*
                    If in selection mode, a click will select the item.
                    If not, a click will show an image view.
                     */
                    onClick = {
                        if (!selectionMode) { onWallpaperViewClick() }
                        else { onItemSelection() }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onActivateSelectionMode(true)
                            onItemSelection()
                        }
                    }
                ),
            )
        /*
        If in selection mode and item is selected, show a checkmark circle.
        If not selected, show an empty circle.
         */
        if (selectionMode) {
            val bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
            if (itemSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.image_is_selected),
                    modifier = Modifier
                        .padding(4.dp)
                        .border(2.dp, bgColor, CircleShape)
                        .clip(CircleShape)
                        .background(bgColor)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.image_is_not_selected),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

fun fileFromContentUri(context: Context, contentUri: Uri): File {
    // Preparing Temp file name
    val fileExtension = getFileExtension(context, contentUri)
    val fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""

    // Creating Temp file
    val tempFile = File(context.cacheDir, fileName)
    tempFile.createNewFile()

    try {
        val oStream = FileOutputStream(tempFile)
        val inputStream = context.contentResolver.openInputStream(contentUri)

        inputStream?.let {
            copy(inputStream, oStream)
        }

        oStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return tempFile
}

private fun getFileExtension(context: Context, uri: Uri): String? {
    val fileType: String? = context.contentResolver.getType(uri)
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
}

@Throws(IOException::class)
private fun copy(source: InputStream, target: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (source.read(buf).also { length = it } > 0) {
        target.write(buf, 0, length)
    }
}