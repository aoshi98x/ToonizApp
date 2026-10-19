package com.colombianapps.toonizapp.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BlurMaskFilter
import uniffi.rust_core.ToolType
import java.util.Random
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class DrawingManager {
    private val bitmaps = mutableMapOf<String, Bitmap>()
    private var canvasWidth = 1920
    private var canvasHeight = 1080
    private val random = Random()

    private var lastX = 0f
    private var lastY = 0f

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

    fun startPath(x: Float, y: Float, pressure: Float) {
        lastX = x
        lastY = y
    }

    fun addPathPoint(layerId: Long, frameIndex: Int, x: Float, y: Float, pressure: Float, color: Int, baseWidth: Float, toolType: ToolType, hardness: Float) {
        val bmp = getBitmap(layerId, frameIndex)
        val canvas = Canvas(bmp)

        val effectiveWidth = (baseWidth * pressure.coerceIn(0.2f, 1f)).coerceAtLeast(1f)

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
        }

        when (toolType) {
            ToolType.PENCIL -> {
                paint.style = Paint.Style.FILL
                paint.color = color

                val spacing = (effectiveWidth * 0.25f).coerceAtLeast(1f)
                val baseAlpha = (255f * hardness.coerceIn(0.1f, 1f) * pressure.coerceIn(0.2f, 1f)).toInt().coerceIn(20, 220)

                val dx = x - lastX
                val dy = y - lastY
                val dist = hypot(dx, dy)
                val steps = (dist / spacing).toInt().coerceIn(1, 100)

                for (i in 0..steps) {
                    val t = if (steps > 0) i / steps.toFloat() else 0f
                    val cx = lastX + dx * t
                    val cy = lastY + dy * t

                    // Undertaker's Pencil / Sketch brush micro-particle scatter texture
                    val particleCount = (effectiveWidth * 0.4f).toInt().coerceIn(2, 10)
                    for (p in 0 until particleCount) {
                        val angle = random.nextFloat() * 2f * Math.PI.toFloat() + p
                        val r = random.nextFloat() * (effectiveWidth * 0.45f)
                        val jx = cx + r * cos(angle)
                        val jy = cy + r * sin(angle)

                        paint.alpha = (baseAlpha * (0.4f + random.nextFloat() * 0.6f)).toInt().coerceIn(10, 200)
                        val dotRadius = (effectiveWidth * 0.12f * (0.5f + random.nextFloat() * 0.5f)).coerceAtLeast(0.8f)
                        canvas.drawCircle(jx, jy, dotRadius, paint)
                    }
                }
            }
            ToolType.HARD_ERASER -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = effectiveWidth
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                canvas.drawLine(lastX, lastY, x, y, paint)
            }
            ToolType.SOFT_ERASER -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = effectiveWidth
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                val blurRadius = (effectiveWidth * (1.0f - hardness) * 1.5f).coerceAtLeast(2f)
                paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                canvas.drawLine(lastX, lastY, x, y, paint)
            }
        }

        lastX = x
        lastY = y
    }

    fun clear(layerId: Long, frameIndex: Int) {
        val key = "${layerId}_$frameIndex"
        bitmaps[key]?.eraseColor(android.graphics.Color.TRANSPARENT)
    }
}
