package com.colombianapps.toonizapp.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.colombianapps.toonizapp.data.model.AnimationProject
import com.colombianapps.toonizapp.data.repository.ProjectRepository
import org.json.JSONArray
import org.json.JSONObject
import uniffi.rust_core.AnimationEngine
import uniffi.rust_core.Layer
import uniffi.rust_core.OnionStroke
import uniffi.rust_core.Stroke
import uniffi.rust_core.ToolType
import java.io.File
import java.util.ArrayDeque
import kotlin.math.hypot

enum class SelectionType {
    RECTANGLE,
    POLYGON,
    FREEHAND
}

enum class CanvasTool {
    SELECTION,
    PENCIL,
    HARD_ERASER,
    SOFT_ERASER
}

enum class SelectionHandle {
    NONE,
    MOVE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

data class FrameCellSummary(
    val hasStrokes: Boolean,
    val exposure: Int
)

data class ClipboardEntry(
    val relativeFrameIndex: Int,
    val bitmap: Bitmap?,
    val exposure: Int
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    val projectRepository = ProjectRepository(application)
    val engine = AnimationEngine()
    val drawingManager = DrawingManager()

    var currentProject by mutableStateOf<AnimationProject?>(null)
        private set

    var hasUnsavedChanges by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var activeTool by mutableStateOf(CanvasTool.PENCIL)
        private set

    var activeSelectionType by mutableStateOf(SelectionType.RECTANGLE)
        private set

    // Selection Transformation State
    val selectionPoints = mutableStateListOf<Offset>()
    var isSelectionActive by mutableStateOf(false)
    var selectionBounds by mutableStateOf<Rect?>(null)
    var selectionBitmap by mutableStateOf<Bitmap?>(null)

    // Selection Transform values
    var selectionScaleX by mutableStateOf(1f)
    var selectionScaleY by mutableStateOf(1f)
    var selectionOffsetX by mutableStateOf(0f)
    var selectionOffsetY by mutableStateOf(0f)
    var isSelectionFlippedHorizontally by mutableStateOf(false)
    var isSelectionFlippedVertically by mutableStateOf(false)
    var activeHandle by mutableStateOf(SelectionHandle.NONE)

    var selectedTool by mutableStateOf(ToolType.PENCIL)
        private set

    var currentColor by mutableStateOf(Color.Black)
        private set

    var currentColorArgb by mutableStateOf(android.graphics.Color.BLACK)
        private set

    var toolHue by mutableStateOf(0f)
        private set

    var toolSaturation by mutableStateOf(0f)
        private set

    var toolBrightness by mutableStateOf(0f)
        private set

    var strokeWidth by mutableStateOf(8f)
        private set

    var strokeHardness by mutableStateOf(0.5f)
        private set

    var isColorPickerOpen by mutableStateOf(false)

    var isAdjustingThickness by mutableStateOf(false)

    var scale by mutableStateOf(1f)
        private set

    var offsetX by mutableStateOf(0f)
        private set

    var offsetY by mutableStateOf(0f)
        private set

    var rotation by mutableStateOf(0f)
        private set

    var strokesVersion by mutableStateOf(0)
        private set

    var drawNonce by mutableStateOf(0)
        private set

    var currentFrame by mutableStateOf(1)
        private set

    var totalTimelineFrames by mutableStateOf(240)
        private set

    var activeLayerId by mutableStateOf(0L)
        private set

    var layers by mutableStateOf(engine.getLayers())
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var onionSkinPast by mutableStateOf(1)
        private set

    var onionSkinFuture by mutableStateOf(0)
        private set

    var currentScreenPosition by mutableStateOf<Offset?>(null)
        private set

    var isPointerDown by mutableStateOf(false)
        private set

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    // Multi-Selection State for Timeline
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedFrames by mutableStateOf<Set<Int>>(emptySet())
        private set

    var hasClipboardData by mutableStateOf(false)
        private set

    private var copiedEntries = listOf<ClipboardEntry>()

    private var playbackJob: Job? = null
    private var autoSaveJob: Job? = null

    // Layer Opacity Map (0.0 to 1.0) - MutableStateMap for Compose Observation!
    private val layerOpacityMap = androidx.compose.runtime.mutableStateMapOf<Long, Float>()

    // Undo & Redo History Stacks per (layerId, frameIndex) cell
    private val undoMap = mutableMapOf<Long, ArrayDeque<Bitmap>>()
    private val redoMap = mutableMapOf<Long, ArrayDeque<Bitmap>>()

    // Cache to eliminate massive JNI overhead during TimelineView rendering
    private val frameCellCache = mutableMapOf<Long, FrameCellSummary>()

    init {
        updateColorHsb(0f, 0f, 0f)
        startAutoSave()
    }

    fun getLayerOpacity(layerId: Long): Float {
        return layerOpacityMap[layerId] ?: 1.0f
    }

    fun setLayerOpacity(layerId: Long, opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        layerOpacityMap[layerId] = clamped
        hasUnsavedChanges = true
        drawNonce++
    }

    fun restoreUnmodifiedSelection() {
        val bounds = selectionBounds ?: return
        val bmp = selectionBitmap ?: return
        if (activeLayerId != 0L) {
            val srcBmp = drawingManager.getBitmap(activeLayerId, currentFrame)
            val canvas = android.graphics.Canvas(srcBmp)
            canvas.drawBitmap(bmp, bounds.left, bounds.top, null)
        }
    }

    fun clearSelectionBounds() {
        if (isSelectionActive && selectionBitmap != null) {
            restoreUnmodifiedSelection()
        }
        selectionPoints.clear()
        isSelectionActive = false
        selectionBounds = null
        selectionBitmap?.recycle()
        selectionBitmap = null
        selectionScaleX = 1f
        selectionScaleY = 1f
        selectionOffsetX = 0f
        selectionOffsetY = 0f
        isSelectionFlippedHorizontally = false
        isSelectionFlippedVertically = false
        activeHandle = SelectionHandle.NONE
        drawNonce++
    }

    fun getPastDrawingFrames(layerId: ULong, currentFrame: Int, count: Int): List<Int> {
        if (count <= 0 || currentFrame <= 1) return emptyList()
        val result = mutableListOf<Int>()
        var f = currentFrame - 1

        while (f >= 1 && result.size < count) {
            val summary = getCellSummary(layerId, f.toUInt())
            if (summary.hasStrokes) {
                val strokes = engine.getStrokesForLayerAndFrame(layerId, f.toUInt())
                val startFrame = if (strokes.isNotEmpty()) strokes.first().frameIndex.toInt() else f
                if (!result.contains(startFrame) && startFrame < currentFrame) {
                    result.add(startFrame)
                }
                f = startFrame - 1
            } else {
                f--
            }
        }
        return result
    }

    fun getFutureDrawingFrames(layerId: ULong, currentFrame: Int, count: Int): List<Int> {
        if (count <= 0) return emptyList()
        val result = mutableListOf<Int>()
        var f = currentFrame + 1

        while (f <= totalTimelineFrames && result.size < count) {
            val summary = getCellSummary(layerId, f.toUInt())
            if (summary.hasStrokes) {
                val strokes = engine.getStrokesForLayerAndFrame(layerId, f.toUInt())
                val startFrame = if (strokes.isNotEmpty()) strokes.first().frameIndex.toInt() else f
                if (!result.contains(startFrame) && startFrame > currentFrame) {
                    result.add(startFrame)
                }
                f = startFrame + summary.exposure
            } else {
                f++
            }
        }
        return result
    }

    fun selectCanvasTool(tool: CanvasTool) {
        if (isSelectionActive && selectionBitmap != null && tool != CanvasTool.SELECTION) {
            applySelectionTransform()
        }
        activeTool = tool
        when (tool) {
            CanvasTool.PENCIL -> selectTool(ToolType.PENCIL)
            CanvasTool.HARD_ERASER -> selectTool(ToolType.HARD_ERASER)
            CanvasTool.SOFT_ERASER -> selectTool(ToolType.SOFT_ERASER)
            CanvasTool.SELECTION -> {}
        }
    }

    fun selectSelectionType(type: SelectionType) {
        if (isSelectionActive && selectionBitmap != null) {
            applySelectionTransform()
        }
        activeSelectionType = type
        selectCanvasTool(CanvasTool.SELECTION)
        clearSelectionBounds()
    }

    fun flipSelectionHorizontal() {
        isSelectionFlippedHorizontally = !isSelectionFlippedHorizontally
        drawNonce++
    }

    fun flipSelectionVertical() {
        isSelectionFlippedVertically = !isSelectionFlippedVertically
        drawNonce++
    }

    fun startHandleDrag(sheetX: Float, sheetY: Float) {
        val bounds = selectionBounds ?: return
        val transLeft = bounds.left + selectionOffsetX
        val transTop = bounds.top + selectionOffsetY
        val currentWidth = bounds.width * selectionScaleX
        val currentHeight = bounds.height * selectionScaleY
        val transRight = transLeft + currentWidth
        val transBottom = transTop + currentHeight

        val hitRadius = 35f

        val isTL = hypot(sheetX - transLeft, sheetY - transTop) < hitRadius
        val isTR = hypot(sheetX - transRight, sheetY - transTop) < hitRadius
        val isBL = hypot(sheetX - transLeft, sheetY - transBottom) < hitRadius
        val isBR = hypot(sheetX - transRight, sheetY - transBottom) < hitRadius

        activeHandle = when {
            isTL -> SelectionHandle.TOP_LEFT
            isTR -> SelectionHandle.TOP_RIGHT
            isBL -> SelectionHandle.BOTTOM_LEFT
            isBR -> SelectionHandle.BOTTOM_RIGHT
            sheetX in transLeft..transRight && sheetY in transTop..transBottom -> SelectionHandle.MOVE
            else -> SelectionHandle.NONE
        }
    }

    fun handleSelectionDrag(deltaX: Float, deltaY: Float) {
        val bounds = selectionBounds ?: return
        val origW = bounds.width.coerceAtLeast(1f)
        val origH = bounds.height.coerceAtLeast(1f)

        when (activeHandle) {
            SelectionHandle.MOVE -> {
                selectionOffsetX += deltaX
                selectionOffsetY += deltaY
            }
            SelectionHandle.BOTTOM_RIGHT -> {
                val newW = (origW * selectionScaleX + deltaX).coerceAtLeast(10f)
                val newH = (origH * selectionScaleY + deltaY).coerceAtLeast(10f)
                selectionScaleX = newW / origW
                selectionScaleY = newH / origH
            }
            SelectionHandle.BOTTOM_LEFT -> {
                val newW = (origW * selectionScaleX - deltaX).coerceAtLeast(10f)
                val newH = (origH * selectionScaleY + deltaY).coerceAtLeast(10f)
                selectionOffsetX += (origW * selectionScaleX - newW)
                selectionScaleX = newW / origW
                selectionScaleY = newH / origH
            }
            SelectionHandle.TOP_RIGHT -> {
                val newW = (origW * selectionScaleX + deltaX).coerceAtLeast(10f)
                val newH = (origH * selectionScaleY - deltaY).coerceAtLeast(10f)
                selectionOffsetY += (origH * selectionScaleY - newH)
                selectionScaleX = newW / origW
                selectionScaleY = newH / origH
            }
            SelectionHandle.TOP_LEFT -> {
                val newW = (origW * selectionScaleX - deltaX).coerceAtLeast(10f)
                val newH = (origH * selectionScaleY - deltaY).coerceAtLeast(10f)
                selectionOffsetX += (origW * selectionScaleX - newW)
                selectionOffsetY += (origH * selectionScaleY - newH)
                selectionScaleX = newW / origW
                selectionScaleY = newH / origH
            }
            else -> {}
        }
        drawNonce++
    }

    fun applySelectionTransform() {
        val bounds = selectionBounds ?: return
        val bmp = selectionBitmap ?: return

        if (activeLayerId == 0L) return

        val targetBmp = drawingManager.getBitmap(activeLayerId, currentFrame)
        val canvas = android.graphics.Canvas(targetBmp)

        val matrix = android.graphics.Matrix().apply {
            val scaleXVal = if (isSelectionFlippedHorizontally) -selectionScaleX else selectionScaleX
            val scaleYVal = if (isSelectionFlippedVertically) -selectionScaleY else selectionScaleY
            postScale(scaleXVal, scaleYVal, bmp.width / 2f, bmp.height / 2f)
            postTranslate(bounds.left + selectionOffsetX, bounds.top + selectionOffsetY)
        }

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bmp, matrix, paint)

        selectionPoints.clear()
        isSelectionActive = false
        selectionBounds = null
        selectionBitmap?.recycle()
        selectionBitmap = null
        selectionScaleX = 1f
        selectionScaleY = 1f
        selectionOffsetX = 0f
        selectionOffsetY = 0f
        isSelectionFlippedHorizontally = false
        isSelectionFlippedVertically = false
        activeHandle = SelectionHandle.NONE
        hasUnsavedChanges = true
        drawNonce++
        invalidateCellCache()
    }

    fun finalizeSelectionArea(bounds: Rect) {
        if (activeLayerId == 0L || bounds.width <= 2f || bounds.height <= 2f) {
            clearSelectionBounds()
            return
        }

        selectionBounds = bounds
        isSelectionActive = true

        val srcBmp = drawingManager.findBitmap(activeLayerId, currentFrame)
        if (srcBmp != null && !srcBmp.isRecycled) {
            val left = bounds.left.toInt().coerceIn(0, srcBmp.width - 1)
            val top = bounds.top.toInt().coerceIn(0, srcBmp.height - 1)
            val width = bounds.width.toInt().coerceIn(1, srcBmp.width - left)
            val height = bounds.height.toInt().coerceIn(1, srcBmp.height - top)

            if (width > 0 && height > 0) {
                val extracted = Bitmap.createBitmap(srcBmp, left, top, width, height)
                selectionBitmap = extracted

                // Cut/Clear extracted pixels from source bitmap
                val canvas = android.graphics.Canvas(srcBmp)
                val clearPaint = android.graphics.Paint().apply {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }
                canvas.drawRect(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    clearPaint
                )
            }
        }
        drawNonce++
    }

    private fun updateColorArgb() {
        currentColorArgb = currentColor.toArgb()
    }

    private fun invalidateCellCache() {
        frameCellCache.clear()
        strokesVersion++
    }

    private fun updateUndoRedoStates() {
        val key = (activeLayerId shl 32) or (currentFrame.toLong() and 0xFFFFFFFFL)
        canUndo = undoMap[key]?.isNotEmpty() == true
        canRedo = redoMap[key]?.isNotEmpty() == true
    }

    fun stopPlayback() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        currentScreenPosition = null
        isPointerDown = false
    }

    fun getMaxFrameWithStrokes(): Int {
        var maxF = 1
        for (layer in layers) {
            val layerMax = drawingManager.getMaxFrame(layer.id.toLong())
            if (layerMax > maxF) {
                maxF = layerMax
            }
        }
        val engineMax = engine.getMaxFrameWithStrokes().toInt()
        return maxOf(maxF, engineMax, 1)
    }

    fun getCellSummary(layerId: ULong, frameIndex: UInt): FrameCellSummary {
        val key = (layerId.toLong() shl 32) or (frameIndex.toLong() and 0xFFFFFFFFL)
        return frameCellCache.getOrPut(key) {
            val hasBmp = drawingManager.hasBitmap(layerId.toLong(), frameIndex.toInt())
            val strokes = engine.getStrokesForLayerAndFrame(layerId, frameIndex)
            val hasStrokes = hasBmp || strokes.isNotEmpty()
            val exposure = if (strokes.isNotEmpty()) strokes.first().exposure.toInt() else 1
            FrameCellSummary(hasStrokes, exposure)
        }
    }

    // -------------------------------------------------------------------------
    // TIMELINE MULTI-SELECTION & CLIPBOARD ACTIONS
    // -------------------------------------------------------------------------
    fun toggleFrameSelection(frame: Int) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedFrames = setOf(frame)
            return
        }
        selectedFrames = if (selectedFrames.contains(frame)) {
            val updated = selectedFrames - frame
            if (updated.isEmpty()) isSelectionMode = false
            updated
        } else {
            selectedFrames + frame
        }
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedFrames = emptySet()
    }

    fun copySelectedFrames() {
        if (activeLayerId == 0L || selectedFrames.isEmpty()) return
        val sorted = selectedFrames.sorted()
        val firstFrame = sorted.first()

        copiedEntries.forEach { it.bitmap?.recycle() }

        val entries = mutableListOf<ClipboardEntry>()
        for (f in sorted) {
            val bmp = drawingManager.copyBitmap(activeLayerId, f)
            val summary = getCellSummary(activeLayerId.toULong(), f.toUInt())
            entries.add(ClipboardEntry(f - firstFrame, bmp, summary.exposure))
        }
        copiedEntries = entries
        hasClipboardData = true
        clearSelection()
    }

    fun deleteSelectedFrames() {
        if (activeLayerId == 0L || selectedFrames.isEmpty()) return
        for (f in selectedFrames) {
            engine.setActiveLayer(activeLayerId.toULong())
            engine.setCurrentFrame(f.toUInt())
            engine.clearCurrentFrame()
            drawingManager.clear(activeLayerId, f)
        }
        hasUnsavedChanges = true
        drawNonce++
        clearSelection()
        invalidateCellCache()
    }

    fun pasteToSelectedFrames() {
        if (activeLayerId == 0L || copiedEntries.isEmpty()) return

        val targetFrames = if (selectedFrames.isNotEmpty()) selectedFrames.sorted() else listOf(currentFrame)
        val startFrame = targetFrames.first()
        val totalTargetFrames = targetFrames.size

        // Smart Paste logic: 1 drawing into multiple frames -> set initial drawing & extend exposure
        if (copiedEntries.size == 1 && totalTargetFrames > 1) {
            val entry = copiedEntries.first()

            val currentSummary = getCellSummary(activeLayerId.toULong(), startFrame.toUInt())
            if (currentSummary.hasStrokes) {
                drawingManager.shiftExposureBitmaps(activeLayerId, startFrame, totalTargetFrames, 1)
                engine.shiftExposure(activeLayerId.toULong(), startFrame.toUInt(), totalTargetFrames)
            }

            if (entry.bitmap != null) {
                val copyBmp = entry.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                drawingManager.setBitmap(activeLayerId, startFrame, copyBmp)
                copyBmp.recycle()
            }

            engine.setActiveLayer(activeLayerId.toULong())
            engine.setCurrentFrame(startFrame.toUInt())
            engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
            engine.endStroke()

            if (totalTargetFrames > 1) {
                engine.shiftExposure(activeLayerId.toULong(), startFrame.toUInt(), totalTargetFrames - 1)
            }

            updateCurrentFrame(startFrame)
        } else {
            for (entry in copiedEntries) {
                val destFrame = startFrame + entry.relativeFrameIndex
                if (entry.bitmap != null) {
                    val copyBmp = entry.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    drawingManager.setBitmap(activeLayerId, destFrame, copyBmp)
                    copyBmp.recycle()
                }
                engine.setActiveLayer(activeLayerId.toULong())
                engine.setCurrentFrame(destFrame.toUInt())
                engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
                engine.endStroke()
            }
            updateCurrentFrame(startFrame)
        }

        hasUnsavedChanges = true
        drawNonce++
        clearSelection()
        invalidateCellCache()
    }

    private fun startAutoSave() {
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(180_000L) // 3 minutes auto-save
                if (hasUnsavedChanges && currentProject != null && !isPlaying) {
                    saveProject()
                }
            }
        }
    }

    fun loadProject(project: AnimationProject, onFinished: (() -> Unit)? = null) {
        stopPlayback()
        isLoading = true
        currentProject = project
        hasUnsavedChanges = false

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                drawingManager.clearAll()
                undoMap.values.forEach { stack -> stack.forEach { it.recycle() }; stack.clear() }
                redoMap.values.forEach { stack -> stack.forEach { it.recycle() }; stack.clear() }
                undoMap.clear()
                redoMap.clear()

                val context = getApplication<Application>()
                val projectDir = File(context.filesDir, "project_drawings_${project.id}")
                val projectJsonFile = File(projectDir, "project.json")

                val savedLayerIds = mutableListOf<Long>()
                val savedLayerNames = mutableListOf<String>()
                val restoredExposures = mutableMapOf<String, Int>()

                if (projectJsonFile.exists()) {
                    try {
                        val projObj = JSONObject(projectJsonFile.readText())
                        if (projObj.has("totalTimelineFrames")) {
                            totalTimelineFrames = projObj.getInt("totalTimelineFrames")
                        }
                        if (projObj.has("layers")) {
                            val layersArray = projObj.getJSONArray("layers")
                            for (i in 0 until layersArray.length()) {
                                val layerObj = layersArray.getJSONObject(i)
                                savedLayerIds.add(layerObj.getLong("id"))
                                savedLayerNames.add(layerObj.getString("name"))
                            }
                        }
                        if (projObj.has("drawings")) {
                            val drawingsArray = projObj.getJSONArray("drawings")
                            for (i in 0 until drawingsArray.length()) {
                                val drawObj = drawingsArray.getJSONObject(i)
                                val lId = drawObj.getLong("layerId")
                                val fIndex = drawObj.getInt("frameIndex")
                                val exp = drawObj.getInt("exposure")
                                restoredExposures["${lId}_$fIndex"] = exp
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val layersFile = File(projectDir, "layers.json")
                    if (layersFile.exists()) {
                        try {
                            val jsonArray = JSONArray(layersFile.readText())
                            for (i in 0 until jsonArray.length()) {
                                savedLayerNames.add(jsonArray.getString(i))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val exposuresFile = File(projectDir, "exposures.json")
                    if (exposuresFile.exists()) {
                        try {
                            val jsonArray = JSONArray(exposuresFile.readText())
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val lId = obj.getLong("layerId")
                                val fIndex = obj.getInt("frameIndex")
                                val exp = obj.getInt("exposure")
                                restoredExposures["${lId}_$fIndex"] = exp
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Reset layers in Rust Engine & Build Layer ID Mapping
                val existingLayers = engine.getLayers()
                for (l in existingLayers) {
                    engine.deleteLayer(l.id)
                }

                val layerIdMap = mutableMapOf<Long, Long>()
                if (savedLayerNames.isNotEmpty()) {
                    for (i in savedLayerNames.indices) {
                        val newId = engine.addLayer(savedLayerNames[i])
                        if (i in savedLayerIds.indices) {
                            layerIdMap[savedLayerIds[i]] = newId.toLong()
                        }
                    }
                } else {
                    engine.addLayer("Nivel 1")
                }

                val layersSnapshot = engine.getLayers()
                val activeLayerSnapshot = layersSnapshot.firstOrNull()?.id?.toLong() ?: 0L

                // Load Bitmaps from disk with Layer ID Mapping!
                val loadedCells = drawingManager.loadBitmapsWithLayerMapping(context, project.id, layerIdMap)

                // Populate stroke markers and exposures in Rust Engine
                for ((layerId, frameIndex) in loadedCells) {
                    engine.setActiveLayer(layerId.toULong())
                    engine.setCurrentFrame(frameIndex.toUInt())
                    engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
                    engine.endStroke()

                    val origSavedId = layerIdMap.entries.firstOrNull { it.value == layerId }?.key ?: layerId
                    val exp = restoredExposures["${origSavedId}_$frameIndex"] ?: restoredExposures["${layerId}_$frameIndex"] ?: 1
                    if (exp > 1) {
                        engine.shiftExposure(layerId.toULong(), frameIndex.toUInt(), exp - 1)
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    layers = layersSnapshot
                    activeLayerId = activeLayerSnapshot
                    engine.setActiveLayer(activeLayerId.toULong())
                    engine.setCurrentFrame(1.toUInt())
                    currentFrame = 1

                    updateUndoRedoStates()
                    invalidateCellCache()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    onFinished?.invoke()
                }
            }
        }
    }

    fun saveProject(onFinished: (() -> Unit)? = null) {
        stopPlayback()
        if (isSelectionActive && selectionBitmap != null) {
            applySelectionTransform()
        }
        val proj = currentProject ?: return
        isSaving = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                projectRepository.saveProject(proj)
                val context = getApplication<Application>()
                val projectDir = File(context.filesDir, "project_drawings_${proj.id}")
                if (!projectDir.exists()) {
                    projectDir.mkdirs()
                }

                // Save unified project.json & track active drawing keys
                val activeDrawingKeys = mutableSetOf<String>()
                val projectJsonFile = File(projectDir, "project.json")
                val projObj = JSONObject().apply {
                    put("id", proj.id)
                    put("name", proj.name)
                    put("canvasWidth", proj.canvasWidth)
                    put("canvasHeight", proj.canvasHeight)
                    put("fps", proj.fps)
                    put("totalTimelineFrames", totalTimelineFrames)

                    val layersArray = JSONArray()
                    for (layer in layers) {
                        val layerObj = JSONObject().apply {
                            put("id", layer.id)
                            put("name", layer.name)
                        }
                        layersArray.put(layerObj)
                    }
                    put("layers", layersArray)

                    val drawingsArray = JSONArray()
                    for (layer in layers) {
                        for (f in 1..totalTimelineFrames) {
                            val strokes = engine.getStrokesForLayerAndFrame(layer.id, f.toUInt())
                            val hasBmp = drawingManager.hasBitmap(layer.id.toLong(), f)
                            if (hasBmp || strokes.isNotEmpty()) {
                                val stroke = strokes.firstOrNull()
                                if (stroke == null || stroke.frameIndex.toInt() == f) {
                                    val drawingKey = "${layer.id}_$f"
                                    activeDrawingKeys.add(drawingKey)
                                    val drawObj = JSONObject().apply {
                                        put("layerId", layer.id)
                                        put("frameIndex", f)
                                        put("exposure", stroke?.exposure?.toInt() ?: 1)
                                        put("drawingId", drawingKey)
                                    }
                                    drawingsArray.put(drawObj)
                                }
                            }
                        }
                    }
                    put("drawings", drawingsArray)
                }
                projectJsonFile.writeText(projObj.toString())

                // Save bitmaps to disk
                drawingManager.saveBitmaps(context, proj.id)

                // Orphan File Cleanup: Delete any PNG file no longer in activeDrawingKeys
                val files = projectDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.name.endsWith(".png")) {
                        val keyName = file.name.removeSuffix(".png")
                        if (!activeDrawingKeys.contains(keyName)) {
                            file.delete()
                        }
                    }
                }

                hasUnsavedChanges = false
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isSaving = false
                    onFinished?.invoke()
                }
            }
        }
    }

    private fun decodeGifFrames(context: android.content.Context, uri: android.net.Uri, canvasW: Int, canvasH: Int): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val movie = android.graphics.Movie.decodeStream(inputStream)
            inputStream.close()

            if (movie != null && movie.duration() > 0) {
                val duration = movie.duration()
                val frameInterval = 80 // ms per frame (~12.5 fps sequence)
                val movieW = movie.width().coerceAtLeast(1)
                val movieH = movie.height().coerceAtLeast(1)

                val sheetW = canvasW * 0.85f
                val sheetH = canvasH * 0.8f
                val sheetCenterX = canvasW / 2f
                val sheetCenterY = canvasH / 2f

                val maxW = (sheetW - 100f).coerceAtLeast(10f)
                val maxH = (sheetH - 100f).coerceAtLeast(10f)

                val scale = kotlin.math.min(maxW / movieW.toFloat(), maxH / movieH.toFloat())
                val scaledW = (movieW * scale).toInt().coerceAtLeast(1)
                val scaledH = (movieH * scale).toInt().coerceAtLeast(1)

                val left = sheetCenterX - (scaledW / 2f)
                val top = sheetCenterY - (scaledH / 2f)

                var time = 0
                while (time < duration) {
                    movie.setTime(time)
                    val rawMovieBmp = Bitmap.createBitmap(movieW, movieH, Bitmap.Config.ARGB_8888)
                    val rawCanvas = android.graphics.Canvas(rawMovieBmp)
                    movie.draw(rawCanvas, 0f, 0f)

                    val scaledMovieBmp = Bitmap.createScaledBitmap(rawMovieBmp, scaledW, scaledH, true)
                    rawMovieBmp.recycle()

                    val frameBmp = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
                    val finalCanvas = android.graphics.Canvas(frameBmp)
                    finalCanvas.drawBitmap(scaledMovieBmp, left, top, null)
                    scaledMovieBmp.recycle()

                    frames.add(frameBmp)
                    time += frameInterval
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return frames
    }

    fun importImageToNewLayer(context: android.content.Context, uri: android.net.Uri) {
        try {
            val canvasW = currentProject?.canvasWidth ?: 1920
            val canvasH = currentProject?.canvasHeight ?: 1080

            val sheetW = canvasW * 0.85f
            val sheetH = canvasH * 0.8f
            val sheetCenterX = canvasW / 2f
            val sheetCenterY = canvasH / 2f

            val gifFrames = decodeGifFrames(context, uri, canvasW, canvasH)

            if (gifFrames.size > 1) {
                val layerName = "GIF ${layers.size + 1}"
                val newLayerId = engine.addLayer(layerName)
                layers = engine.getLayers()
                setActiveLayer(newLayerId.toLong())

                gifFrames.forEachIndexed { i, frameBmp ->
                    val targetFrame = currentFrame + i
                    if (targetFrame <= totalTimelineFrames) {
                        drawingManager.setBitmap(newLayerId.toLong(), targetFrame, frameBmp)
                        engine.setActiveLayer(newLayerId)
                        engine.setCurrentFrame(targetFrame.toUInt())
                        engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
                        engine.endStroke()
                    }
                    frameBmp.recycle()
                }

                engine.setActiveLayer(newLayerId)
                engine.setCurrentFrame(currentFrame.toUInt())
                hasUnsavedChanges = true
                drawNonce++
                invalidateCellCache()
            } else {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return
                val originalBmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBmp == null) return

                val origW = originalBmp.width.coerceAtLeast(1)
                val origH = originalBmp.height.coerceAtLeast(1)

                val maxW = (sheetW - 100f).coerceAtLeast(10f)
                val maxH = (sheetH - 100f).coerceAtLeast(10f)

                val scale = kotlin.math.min(maxW / origW.toFloat(), maxH / origH.toFloat())
                val scaledW = (origW * scale).toInt().coerceAtLeast(1)
                val scaledH = (origH * scale).toInt().coerceAtLeast(1)

                val left = sheetCenterX - (scaledW / 2f)
                val top = sheetCenterY - (scaledH / 2f)

                val composedBmp = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(composedBmp)

                val scaledBmp = Bitmap.createScaledBitmap(originalBmp, scaledW, scaledH, true)
                originalBmp.recycle()

                canvas.drawBitmap(scaledBmp, left, top, null)
                scaledBmp.recycle()

                val layerName = "Imagen ${layers.size + 1}"
                val newLayerId = engine.addLayer(layerName)
                layers = engine.getLayers()
                setActiveLayer(newLayerId.toLong())

                drawingManager.setBitmap(newLayerId.toLong(), currentFrame, composedBmp)

                engine.setActiveLayer(newLayerId)
                engine.setCurrentFrame(currentFrame.toUInt())
                engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
                engine.endStroke()

                // Activate automatic selection transform mode
                activeTool = CanvasTool.SELECTION
                activeSelectionType = SelectionType.RECTANGLE
                selectionBounds = Rect(left, top, left + scaledW, top + scaledH)
                isSelectionActive = true

                val extracted = Bitmap.createBitmap(composedBmp, left.toInt().coerceAtLeast(0), top.toInt().coerceAtLeast(0), scaledW.coerceAtMost(canvasW - left.toInt()), scaledH.coerceAtMost(canvasH - top.toInt()))
                composedBmp.recycle()
                selectionBitmap = extracted

                // Clear layer bitmap in DrawingManager so no duplicate background ghost image remains!
                val layerBmp = drawingManager.getBitmap(newLayerId.toLong(), currentFrame)
                val layerCanvas = android.graphics.Canvas(layerBmp)
                layerCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

                hasUnsavedChanges = true
                drawNonce++
                invalidateCellCache()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectTool(tool: ToolType) {
        selectedTool = tool
    }

    fun selectColor(color: Color) {
        currentColor = color
        updateColorArgb()
        hasUnsavedChanges = true
    }

    fun updateColorHsb(h: Float, s: Float, b: Float) {
        toolHue = h
        toolSaturation = s
        toolBrightness = b
        currentColor = Color.hsv(h, s, b)
        updateColorArgb()
        hasUnsavedChanges = true
    }

    fun updateStrokeWidth(width: Float) {
        strokeWidth = width.coerceIn(1f, 500f)
    }

    fun updateStrokeHardness(hardness: Float) {
        strokeHardness = hardness.coerceIn(0f, 1f)
    }

    fun setHue(hue: Float) {
        toolHue = hue.coerceIn(0f, 360f)
        currentColor = Color.hsv(toolHue, toolSaturation, toolBrightness)
        updateColorArgb()
        hasUnsavedChanges = true
    }

    fun setSaturation(sat: Float) {
        toolSaturation = sat.coerceIn(0f, 1f)
        currentColor = Color.hsv(toolHue, toolSaturation, toolBrightness)
        updateColorArgb()
        hasUnsavedChanges = true
    }

    fun setBrightness(bri: Float) {
        toolBrightness = bri.coerceIn(0f, 1f)
        currentColor = Color.hsv(toolHue, toolSaturation, toolBrightness)
        updateColorArgb()
        hasUnsavedChanges = true
    }

    fun updateTransform(newScale: Float, newPanX: Float, newPanY: Float, newRotation: Float) {
        scale = newScale.coerceIn(0.2f, 10f)
        offsetX = newPanX
        offsetY = newPanY
        rotation = newRotation
    }

    fun resetTransform() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
    }

    fun updateCurrentFrame(frame: Int) {
        if (isSelectionActive && selectionBitmap != null) {
            applySelectionTransform()
        }
        currentFrame = frame.coerceIn(1, totalTimelineFrames)
        engine.setCurrentFrame(currentFrame.toUInt())
        updateUndoRedoStates()
        invalidateCellCache()
    }

    fun expandTimeline() {
        if (totalTimelineFrames < 240) {
            totalTimelineFrames = (totalTimelineFrames + 24).coerceAtMost(240)
        }
    }

    fun setActiveLayer(layerId: Long) {
        if (isSelectionActive && selectionBitmap != null) {
            applySelectionTransform()
        }
        activeLayerId = layerId
        engine.setActiveLayer(layerId.toULong())
        updateUndoRedoStates()
        invalidateCellCache()
    }

    fun addLayer() {
        val newName = "Nivel ${layers.size + 1}"
        val newId = engine.addLayer(newName)
        layers = engine.getLayers()
        setActiveLayer(newId.toLong())
        hasUnsavedChanges = true
        invalidateCellCache()
    }

    private val layerVisibilityMap = mutableMapOf<Long, Boolean>()

    fun isLayerVisible(layerId: Long): Boolean {
        return layerVisibilityMap[layerId] ?: true
    }

    fun toggleLayerVisibility(layerId: Long) {
        val current = isLayerVisible(layerId)
        layerVisibilityMap[layerId] = !current
        strokesVersion++
        drawNonce++
    }

    fun moveLayerToIndex(layerId: Long, newIndex: Int) {
        val mutableLayers = layers.toMutableList()
        val currentIndex = mutableLayers.indexOfFirst { it.id.toLong() == layerId }
        if (currentIndex < 0) return
        val item = mutableLayers.removeAt(currentIndex)
        val clampedIndex = newIndex.coerceIn(0, mutableLayers.size)
        mutableLayers.add(clampedIndex, item)
        layers = mutableLayers

        for (l in engine.getLayers()) {
            engine.deleteLayer(l.id)
        }
        for (l in mutableLayers) {
            engine.addLayer(l.name)
        }
        engine.setActiveLayer(activeLayerId.toULong())

        hasUnsavedChanges = true
        strokesVersion++
        drawNonce++
        invalidateCellCache()
    }

    fun renameLayer(layerId: Long, newName: String) {
        val cleanName = if (newName.isBlank()) "Nivel Sin Nombre" else newName
        layers = layers.map {
            if (it.id.toLong() == layerId) Layer(it.id, cleanName) else it
        }
        hasUnsavedChanges = true
    }

    fun deleteLayer(layerId: Long) {
        engine.deleteLayer(layerId.toULong())
        layers = engine.getLayers()
        activeLayerId = layers.firstOrNull()?.id?.toLong() ?: 0L
        engine.setActiveLayer(activeLayerId.toULong())
        hasUnsavedChanges = true
        invalidateCellCache()
    }

    fun adjustExposure(delta: Int) {
        if (activeLayerId == 0L) return
        val strokes = engine.getStrokesForLayerAndFrame(activeLayerId.toULong(), currentFrame.toUInt())
        val actualStartFrame = if (strokes.isNotEmpty()) strokes.first().frameIndex.toInt() else currentFrame
        val currentExp = if (strokes.isNotEmpty()) strokes.first().exposure.toInt() else 1

        if (delta < 0 && currentExp <= 1) {
            // Delete 1-frame drawing and shift subsequent frames left
            drawingManager.clear(activeLayerId, actualStartFrame)
            engine.setActiveLayer(activeLayerId.toULong())
            engine.setCurrentFrame(actualStartFrame.toUInt())
            engine.clearCurrentFrame()
            drawingManager.shiftExposureBitmaps(activeLayerId, actualStartFrame + 1, -1, 1)
            engine.shiftExposure(activeLayerId.toULong(), actualStartFrame.toUInt(), -1)
        } else {
            drawingManager.shiftExposureBitmaps(activeLayerId, actualStartFrame, delta, currentExp)
            engine.shiftExposure(activeLayerId.toULong(), actualStartFrame.toUInt(), delta)
        }

        hasUnsavedChanges = true
        drawNonce++
        invalidateCellCache()
    }

    fun createNewDrawingAtCurrentFrame() {
        if (activeLayerId == 0L) return
        val summary = getCellSummary(activeLayerId.toULong(), currentFrame.toUInt())
        if (!summary.hasStrokes) {
            drawingManager.getBitmap(activeLayerId, currentFrame)
            engine.setActiveLayer(activeLayerId.toULong())
            engine.setCurrentFrame(currentFrame.toUInt())
            engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
            engine.endStroke()
            hasUnsavedChanges = true
            drawNonce++
            invalidateCellCache()
        }
    }

    fun duplicateCurrentFrame() {
        if (activeLayerId == 0L) return
        val sourceFrame = currentFrame
        val targetFrame = currentFrame + 1

        val summaryNext = getCellSummary(activeLayerId.toULong(), targetFrame.toUInt())
        if (summaryNext.hasStrokes) {
            drawingManager.shiftExposureBitmaps(activeLayerId, targetFrame, 1, 1)
            engine.shiftExposure(activeLayerId.toULong(), targetFrame.toUInt(), 1)
        }

        val sourceBmp = drawingManager.getBitmap(activeLayerId, sourceFrame)
        val copyBmp = sourceBmp.copy(Bitmap.Config.ARGB_8888, true)
        drawingManager.setBitmap(activeLayerId, targetFrame, copyBmp)
        copyBmp.recycle()

        engine.setActiveLayer(activeLayerId.toULong())
        engine.setCurrentFrame(targetFrame.toUInt())
        engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
        engine.endStroke()

        updateCurrentFrame(targetFrame)
        hasUnsavedChanges = true
        drawNonce++
        invalidateCellCache()
    }

    fun updateOnionSkinPast(count: Int) {
        onionSkinPast = count.coerceIn(0, 3)
        strokesVersion++
    }

    fun updateOnionSkinFuture(count: Int) {
        onionSkinFuture = count.coerceIn(0, 3)
        strokesVersion++
    }

    fun togglePlayback() {
        if (layers.isEmpty()) return
        if (isPlaying) {
            stopPlayback()
        } else {
            isPlaying = true
            val fps = currentProject?.fps ?: 24
            val maxFrame = getMaxFrameWithStrokes()

            playbackJob = viewModelScope.launch {
                val startSystemTime = System.currentTimeMillis()
                var frameIndex = currentFrame
                var stepCount = 0L

                while (isPlaying) {
                    stepCount++
                    val targetTime = startSystemTime + (stepCount * 1000L / fps)
                    val delayMs = (targetTime - System.currentTimeMillis()).coerceAtLeast(1L)
                    delay(delayMs)

                    frameIndex = if (frameIndex >= maxFrame) 1 else frameIndex + 1
                    updateCurrentFrame(frameIndex)
                }
            }
        }
    }

    fun startStroke(worldX: Float, worldY: Float, pressure: Float) {
        if (isPlaying || activeLayerId == 0L) return

        val key = (activeLayerId shl 32) or (currentFrame.toLong() and 0xFFFFFFFFL)
        val undoStack = undoMap.getOrPut(key) { ArrayDeque() }
        val snapshot = drawingManager.copyBitmap(activeLayerId, currentFrame)
        if (snapshot != null) {
            if (undoStack.size >= 30) {
                undoStack.removeFirst().recycle()
            }
            undoStack.addLast(snapshot)
        }

        redoMap[key]?.let { stack ->
            stack.forEach { it.recycle() }
            stack.clear()
        }

        updateUndoRedoStates()

        val colorInt = currentColorArgb
        if (selectedTool == ToolType.PENCIL) {
            engine.startStroke(worldX, worldY, pressure, selectedTool, colorInt.toUInt(), strokeWidth)
            drawingManager.startPath(worldX, worldY, pressure)
        } else {
            val isSoft = selectedTool == ToolType.SOFT_ERASER
            engine.eraseAt(worldX, worldY, strokeWidth * 2.5f, strokeHardness, isSoft)
            drawingManager.startPath(worldX, worldY, pressure)
        }
        hasUnsavedChanges = true
        drawNonce++
    }

    fun addPoint(worldX: Float, worldY: Float, pressure: Float) {
        if (isPlaying || activeLayerId == 0L) return
        val colorInt = currentColorArgb
        if (selectedTool == ToolType.PENCIL) {
            engine.addPoint(worldX, worldY, pressure)
            drawingManager.addPathPoint(activeLayerId, currentFrame, worldX, worldY, pressure, colorInt, strokeWidth, selectedTool, strokeHardness)
        } else {
            val isSoft = selectedTool == ToolType.SOFT_ERASER
            engine.eraseAt(worldX, worldY, strokeWidth * 2.5f, strokeHardness, isSoft)
            drawingManager.addPathPoint(activeLayerId, currentFrame, worldX, worldY, pressure, 0, strokeWidth * 2.5f, selectedTool, strokeHardness)
        }
        hasUnsavedChanges = true
        drawNonce++
    }

    fun endStroke() {
        if (isPlaying || activeLayerId == 0L) return
        if (selectedTool == ToolType.PENCIL) {
            engine.endStroke()
        }
        hasUnsavedChanges = true
        updateUndoRedoStates()
        invalidateCellCache()
    }

    fun undo() {
        if (activeLayerId == 0L) return
        val key = (activeLayerId shl 32) or (currentFrame.toLong() and 0xFFFFFFFFL)
        val undoStack = undoMap[key]
        if (undoStack != null && undoStack.isNotEmpty()) {
            val currentSnapshot = drawingManager.copyBitmap(activeLayerId, currentFrame)
            if (currentSnapshot != null) {
                val redoStack = redoMap.getOrPut(key) { ArrayDeque() }
                if (redoStack.size >= 30) {
                    redoStack.removeFirst().recycle()
                }
                redoStack.addLast(currentSnapshot)
            }

            val previousBmp = undoStack.removeLast()
            drawingManager.setBitmap(activeLayerId, currentFrame, previousBmp)
            previousBmp.recycle()
            engine.undo()

            hasUnsavedChanges = true
            drawNonce++
            updateUndoRedoStates()
            invalidateCellCache()
        }
    }

    fun redo() {
        if (activeLayerId == 0L) return
        val key = (activeLayerId shl 32) or (currentFrame.toLong() and 0xFFFFFFFFL)
        val redoStack = redoMap[key]
        if (redoStack != null && redoStack.isNotEmpty()) {
            val currentSnapshot = drawingManager.copyBitmap(activeLayerId, currentFrame)
            if (currentSnapshot != null) {
                val undoStack = undoMap.getOrPut(key) { ArrayDeque() }
                if (undoStack.size >= 30) {
                    undoStack.removeFirst().recycle()
                }
                undoStack.addLast(currentSnapshot)
            }

            val redoneBmp = redoStack.removeLast()
            drawingManager.setBitmap(activeLayerId, currentFrame, redoneBmp)
            redoneBmp.recycle()

            hasUnsavedChanges = true
            drawNonce++
            updateUndoRedoStates()
            invalidateCellCache()
        }
    }

    fun clearCurrentFrame() {
        if (activeLayerId == 0L) return
        
        val key = (activeLayerId shl 32) or (currentFrame.toLong() and 0xFFFFFFFFL)
        val undoStack = undoMap.getOrPut(key) { ArrayDeque() }
        val snapshot = drawingManager.copyBitmap(activeLayerId, currentFrame)
        if (snapshot != null) {
            if (undoStack.size >= 30) {
                undoStack.removeFirst().recycle()
            }
            undoStack.addLast(snapshot)
        }

        engine.clearCurrentFrame()
        drawingManager.clear(activeLayerId, currentFrame)
        hasUnsavedChanges = true
        updateUndoRedoStates()
        invalidateCellCache()
    }

    fun getStrokes(): List<Stroke> {
        if (activeLayerId == 0L && layers.isEmpty()) return emptyList()
        return engine.getStrokesForFrame(currentFrame.toUInt())
    }

    fun getOnionStrokes(): List<OnionStroke> {
        if (activeLayerId == 0L) return emptyList()
        return engine.getOnionStrokes(onionSkinPast.toUInt(), onionSkinFuture.toUInt())
    }

    fun setPointerState(down: Boolean, pos: Offset?) {
        isPointerDown = down
        currentScreenPosition = pos
    }
}
