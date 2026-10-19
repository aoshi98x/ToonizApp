package com.colombianapps.toonizapp.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BlurMaskFilter
import uniffi.rust_core.ToolType
import java.util.Random

class DrawingManager {
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private var canvasWidth = 1920
    private var canvasHeight = 1080
    private val random = Random()

    fun setSize(width: Int, height: Int) {
        if (width > 0 && height > 0 && (width != canvasWidth || height != canvasHeight)) {
            canvasWidth = width
            canvasHeight = height
        }
    }

    fun getBitmap(layerId: Long, frameIndex: Int): Bitmap {
        val key = "${layerId}_$frameIndex"
        return bitmaps.getOrPut(key) {
            Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.TRANSPARENT)
            }
        }
    }

    fun drawLine(layerId: Long, frameIndex: Int, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float, toolType: ToolType, hardness: Float) {
        val bmp = getBitmap(layerId, frameIndex)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
        }

        when (toolType) {
            ToolType.PENCIL -> {
                // Graphite pencil texture effect (sketchy/grainy texture)
                paint.color = color
                val dx = x2 - x1
                val dy = y2 - y1
                val dist = kotlin.math.hypot(dx, dy)
                val steps = (dist / 1.5f).coerceAtLeast(1f).toInt()

                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val cx = x1 + dx * t
                    val cy = y1 + dy * t
                    // Jitter scatter for graphite texture
                    val scatter = width * 0.3f
                    val jx = cx + (random.nextFloat() - 0.5f) * scatter
                    val jy = cy + (random.nextFloat() - 0.5f) * scatter
                    paint.alpha = (40 + random.nextInt(120)).coerceIn(20, 180)
                    val dotRadius = (width * 0.18f * (0.4f + random.nextFloat() * 0.6f)).coerceAtLeast(1f)
                    canvas.drawCircle(jx, jy, dotRadius, paint)
                }
            }
            ToolType.HARD_ERASER -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = width
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                canvas.drawLine(x1, y1, x2, y2, paint)
            }
            ToolType.SOFT_ERASER -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = width
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                val blurRadius = (width * (1.0f - hardness) * 1.5f).coerceAtLeast(2f)
                paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                canvas.drawLine(x1, y1, x2, y2, paint)
            }
        }
    }

    fun clear(layerId: Long, frameIndex: Int) {
        val key = "${layerId}_$frameIndex"
        bitmaps[key]?.eraseColor(android.graphics.Color.TRANSPARENT)
    }
}
