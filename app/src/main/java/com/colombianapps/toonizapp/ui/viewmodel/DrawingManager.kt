package com.colombianapps.toonizapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BlurMaskFilter
import uniffi.rust_core.ToolType
import java.io.File
import java.io.FileOutputStream
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

    fun findBitmap(layerId: Long, frameIndex: Int): Bitmap? {
        val key = makeKey(layerId, frameIndex)
        val bmp = bitmaps[key] ?: return null
        return if (!bmp.isRecycled) bmp else null
    }

    fun getMaxFrame(layerId: Long): Int {
        var maxF = 1
        for (key in bitmaps.keys) {
            val lId = key shr 32
            if (lId == layerId) {
                val f = (key and 0xFFFFFFFFL).toInt()
                if (f > maxF) {
                    maxF = f
                }
            }
        }
        return maxF
    }

    fun hasBitmap(layerId: Long, frameIndex: Int): Boolean {
        val key = makeKey(layerId, frameIndex)
        val bmp = bitmaps[key] ?: return false
        return !bmp.isRecycled
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
        val bmp = bitmaps.remove(key)
        bmp?.recycle()
    }

    fun setBitmap(layerId: Long, frameIndex: Int, sourceBmp: Bitmap?) {
        val targetBmp = getBitmap(layerId, frameIndex)
        val canvas = Canvas(targetBmp)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        if (sourceBmp != null) {
            canvas.drawBitmap(sourceBmp, 0f, 0f, null)
        }
    }

    fun copyBitmap(layerId: Long, frameIndex: Int): Bitmap? {
        val key = makeKey(layerId, frameIndex)
        val bmp = bitmaps[key] ?: return null
        return bmp.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun shiftExposureBitmaps(layerId: Long, startFrame: Int, delta: Int, currentExposure: Int) {
        if (delta == 0) return
        val boundary = startFrame + currentExposure

        if (delta > 0) {
            val targetKeys = bitmaps.keys.filter { key ->
                val lId = key shr 32
                val fIndex = (key and 0xFFFFFFFFL).toInt()
                lId == layerId && fIndex >= boundary
            }.sortedByDescending { (it and 0xFFFFFFFFL).toInt() }

            for (oldKey in targetKeys) {
                val fIndex = (oldKey and 0xFFFFFFFFL).toInt()
                val newFIndex = fIndex + delta
                val newKey = makeKey(layerId, newFIndex)

                val bmp = bitmaps.remove(oldKey)
                if (bmp != null) {
                    bitmaps[newKey] = bmp
                }
            }
        } else {
            val shrinkAmount = -delta
            val targetKeys = bitmaps.keys.filter { key ->
                val lId = key shr 32
                val fIndex = (key and 0xFFFFFFFFL).toInt()
                lId == layerId && fIndex >= boundary
            }.sortedBy { (it and 0xFFFFFFFFL).toInt() }

            for (oldKey in targetKeys) {
                val fIndex = (oldKey and 0xFFFFFFFFL).toInt()
                val newFIndex = (fIndex - shrinkAmount).coerceAtLeast(boundary + delta)
                val newKey = makeKey(layerId, newFIndex)

                val bmp = bitmaps.remove(oldKey)
                if (bmp != null) {
                    bitmaps[newKey] = bmp
                }
            }
        }
    }

    fun clearAll() {
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
    }

    fun saveBitmaps(context: Context, projectId: String) {
        val projectDir = File(context.filesDir, "project_drawings_$projectId")
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }

        val snapshotList = synchronized(bitmaps) {
            bitmaps.entries.mapNotNull { (key, bmp) ->
                if (!bmp.isRecycled) {
                    try {
                        Pair(key, bmp.copy(Bitmap.Config.ARGB_8888, false))
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
        }

        for ((key, copyBmp) in snapshotList) {
            val layerId = key shr 32
            val frameIndex = (key and 0xFFFFFFFFL).toInt()
            val file = File(projectDir, "${layerId}_$frameIndex.png")

            try {
                FileOutputStream(file).use { out ->
                    copyBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                copyBmp.recycle()
            }
        }
    }

    fun loadBitmapsWithLayerMapping(context: Context, projectId: String, layerIdMap: Map<Long, Long>): Set<Pair<Long, Int>> {
        clearAll()
        val loadedCells = mutableSetOf<Pair<Long, Int>>()
        val projectDir = File(context.filesDir, "project_drawings_$projectId")
        if (!projectDir.exists()) return loadedCells

        val files = projectDir.listFiles() ?: return loadedCells
        for (file in files) {
            if (file.name.endsWith(".png")) {
                val parts = file.name.removeSuffix(".png").split("_")
                if (parts.size == 2) {
                    val savedLayerId = parts[0].toLongOrNull() ?: continue
                    val frameIndex = parts[1].toIntOrNull() ?: continue
                    val remappedLayerId = layerIdMap[savedLayerId] ?: savedLayerId
                    try {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            val mutableBmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
                            bmp.recycle()
                            val key = makeKey(remappedLayerId, frameIndex)
                            bitmaps[key] = mutableBmp
                            loadedCells.add(Pair(remappedLayerId, frameIndex))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return loadedCells
    }

    fun loadBitmaps(context: Context, projectId: String): Set<Pair<Long, Int>> {
        return loadBitmapsWithLayerMapping(context, projectId, emptyMap())
    }

    fun deleteProjectDrawings(context: Context, projectId: String) {
        val projectDir = File(context.filesDir, "project_drawings_$projectId")
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
    }
}
