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
    private val bitmaps = mutableMapOf<Long, Bitmap>()
    private var canvasWidth = 1920
    private var canvasHeight = 1080
    private val random = Random()

    private var lastX = 0f
    private var lastY = 0f

    // Reusable Paint and Xfermode objects to eliminate GC pressure
    private val pencilPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    private val hardEraserPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = clearXfermode
    }

    private val softEraserPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = clearXfermode
    }

    private var cachedBlurRadius = -1f
    private var cachedBlurMaskFilter: BlurMaskFilter? = null

    private fun makeKey(layerId: Long, frameIndex: Int): Long {
        return (layerId shl 32) or (frameIndex.toLong() and 0xFFFFFFFFL)
    }

    fun setSize(width: Int, height: Int) {
        if (width > 0 && height > 0 && (width != canvasWidth || height != canvasHeight)) {
            canvasWidth = width
            canvasHeight = height
        }
    }

    fun getBitmap(layerId: Long, frameIndex: Int): Bitmap {
        val key = makeKey(layerId, frameIndex)
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

        when (toolType) {
            ToolType.PENCIL -> {
                pencilPaint.color = color

                val spacing = (effectiveWidth * 0.25f).coerceAtLeast(1f)
                val baseAlpha = (255f * hardness.coerceIn(0.1f, 1f) * pressure.coerceIn(0.2f, 1f)).toInt().coerceIn(20, 220)

                val dx = x - lastX
                val dy = y - lastY
                val dist = hypot(dx, dy)
                val steps = (dist / spacing).toInt().coerceIn(1, 100)
                val twoPi = 6.283185307179586f

                for (i in 0..steps) {
                    val t = if (steps > 0) i / steps.toFloat() else 0f
                    val cx = lastX + dx * t
                    val cy = lastY + dy * t

                    // Undertaker's Pencil / Sketch brush micro-particle scatter texture
                    val particleCount = (effectiveWidth * 0.4f).toInt().coerceIn(2, 10)
                    for (p in 0 until particleCount) {
                        val angle = random.nextFloat() * twoPi + p
                        val r = random.nextFloat() * (effectiveWidth * 0.45f)
                        val jx = cx + r * cos(angle)
                        val jy = cy + r * sin(angle)

                        pencilPaint.alpha = (baseAlpha * (0.4f + random.nextFloat() * 0.6f)).toInt().coerceIn(10, 200)
                        val dotRadius = (effectiveWidth * 0.12f * (0.5f + random.nextFloat() * 0.5f)).coerceAtLeast(0.8f)
                        canvas.drawCircle(jx, jy, dotRadius, pencilPaint)
                    }
                }
            }
            ToolType.HARD_ERASER -> {
                hardEraserPaint.strokeWidth = effectiveWidth
                canvas.drawLine(lastX, lastY, x, y, hardEraserPaint)
            }
            ToolType.SOFT_ERASER -> {
                softEraserPaint.strokeWidth = effectiveWidth
                val blurRadius = (effectiveWidth * (1.0f - hardness) * 1.5f).coerceAtLeast(2f)
                if (blurRadius != cachedBlurRadius) {
                    cachedBlurRadius = blurRadius
                    cachedBlurMaskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                }
                softEraserPaint.maskFilter = cachedBlurMaskFilter
                canvas.drawLine(lastX, lastY, x, y, softEraserPaint)
            }
        }

        lastX = x
        lastY = y
    }

    fun clear(layerId: Long, frameIndex: Int) {
        val key = makeKey(layerId, frameIndex)
        bitmaps[key]?.eraseColor(android.graphics.Color.TRANSPARENT)
    }
}
