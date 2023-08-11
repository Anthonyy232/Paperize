package com.anthonyla.paperize.data.configuration

import android.graphics.Color
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = WallpaperEngine()
    inner class WallpaperEngine: WallpaperService.Engine() {
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            val canvas = holder.lockCanvas()
            val paint = Paint().apply {
                color = Color.GRAY
                style = Paint.Style.FILL
            }
            canvas.drawPaint(paint)
            holder.unlockCanvasAndPost(canvas)
        }

    }

}