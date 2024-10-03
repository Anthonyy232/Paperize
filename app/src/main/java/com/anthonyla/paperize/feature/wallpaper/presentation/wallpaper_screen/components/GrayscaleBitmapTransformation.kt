package com.anthonyla.paperize.feature.wallpaper.presentation.wallpaper_screen.components

import android.graphics.Bitmap
import com.anthonyla.paperize.core.grayBitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.util.Util
import java.security.MessageDigest

/**
 * A BitmapTransformation that applies a grayscale effect to a bitmap to be used with Glide
 */
class GrayscaleBitmapTransformation(private val percent: Int): BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return grayBitmap(toTransform, percent)
    }

    override fun equals(other: Any?): Boolean {
        return other is GrayscaleBitmapTransformation && other.percent == percent
    }

    override fun hashCode(): Int {
        return Util.hashCode(percent)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + percent).toByteArray(CHARSET))
    }

    companion object {
        private const val ID = "com.bumptech.glide.transformations.GrayscaleBitmapTransformation"
    }
}