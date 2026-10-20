package com.colombianapps.toonizapp.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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
import uniffi.rust_core.AnimationEngine
import uniffi.rust_core.OnionStroke
import uniffi.rust_core.Stroke
import uniffi.rust_core.ToolType
import java.io.File

data class FrameCellSummary(
    val hasStrokes: Boolean,
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

    private var playbackJob: Job? = null
    private var autoSaveJob: Job? = null

    // Cache to eliminate massive JNI overhead during TimelineView rendering
    private val frameCellCache = mutableMapOf<Long, FrameCellSummary>()

    init {
        updateColorHsb(0f, 0f, 0f)
        startAutoSave()
    }

    private fun updateColorArgb() {
        currentColorArgb = currentColor.toArgb()
    }

    private fun invalidateCellCache() {
        frameCellCache.clear()
        strokesVersion++
    }

    fun getCellSummary(layerId: ULong, frameIndex: UInt): FrameCellSummary {
        val key = (layerId.toLong() shl 32) or (frameIndex.toLong() and 0xFFFFFFFFL)
        return frameCellCache.getOrPut(key) {
            val strokes = engine.getStrokesForLayerAndFrame(layerId, frameIndex)
            val hasStrokes = strokes.isNotEmpty()
            val exposure = if (hasStrokes) strokes.first().exposure.toInt() else 1
            FrameCellSummary(hasStrokes, exposure)
        }
    }

    private fun startAutoSave() {
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(180_000L) // 3 minutes auto-save
                if (hasUnsavedChanges && currentProject != null) {
                    saveProject()
                }
            }
        }
    }

    fun loadProject(project: AnimationProject) {
        currentProject = project
        hasUnsavedChanges = false

        drawingManager.clearAll()

        val context = getApplication<Application>()
        val projectDir = File(context.filesDir, "project_drawings_${project.id}")
        val layersFile = File(projectDir, "layers.json")

        val savedLayerNames = mutableListOf<String>()
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

        // Reset layers in Rust Engine
        val existingLayers = engine.getLayers()
        for (l in existingLayers) {
            engine.deleteLayer(l.id)
        }

        val restoredLayerIds = mutableListOf<Long>()
        if (savedLayerNames.isNotEmpty()) {
            for (name in savedLayerNames) {
                val newId = engine.addLayer(name)
                restoredLayerIds.add(newId.toLong())
            }
        } else {
            val defaultId = engine.addLayer("Nivel 1")
            restoredLayerIds.add(defaultId.toLong())
        }

        layers = engine.getLayers()
        activeLayerId = layers.firstOrNull()?.id?.toLong() ?: 0L
        engine.setActiveLayer(activeLayerId.toULong())

        // Load Bitmaps from disk into DrawingManager
        val loadedCells = drawingManager.loadBitmaps(context, project.id)

        // Populate stroke markers in Rust Engine for timeline frame cells
        for ((layerId, frameIndex) in loadedCells) {
            engine.setActiveLayer(layerId.toULong())
            engine.setCurrentFrame(frameIndex.toUInt())
            engine.startStroke(0f, 0f, 0.5f, ToolType.PENCIL, 0u, 1f)
            engine.endStroke()
        }

        engine.setActiveLayer(activeLayerId.toULong())
        engine.setCurrentFrame(1.toUInt())
        currentFrame = 1

        invalidateCellCache()
    }

    fun saveProject() {
        val proj = currentProject ?: return
        projectRepository.saveProject(proj)

        val context = getApplication<Application>()
        val projectDir = File(context.filesDir, "project_drawings_${proj.id}")
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }

        // Save layers.json
        val layersFile = File(projectDir, "layers.json")
        try {
            val jsonArray = JSONArray()
            for (layer in layers) {
                jsonArray.put(layer.name)
            }
            layersFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Save bitmaps to disk
        drawingManager.saveBitmaps(context, proj.id)

        hasUnsavedChanges = false
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
        currentFrame = frame.coerceIn(1, totalTimelineFrames)
        engine.setCurrentFrame(currentFrame.toUInt())
        invalidateCellCache()
    }

    fun expandTimeline() {
        if (totalTimelineFrames < 240) {
            totalTimelineFrames = (totalTimelineFrames + 24).coerceAtMost(240)
        }
    }

    fun setActiveLayer(layerId: Long) {
        activeLayerId = layerId
        engine.setActiveLayer(layerId.toULong())
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
        engine.shiftExposure(activeLayerId.toULong(), currentFrame.toUInt(), delta)
        hasUnsavedChanges = true
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
        isPlaying = !isPlaying
        if (isPlaying) {
            val fps = currentProject?.fps ?: 24
            val frameDelay = (1000L / fps).coerceAtLeast(10L)
            val maxFrame = engine.getMaxFrameWithStrokes().toInt().coerceAtMost(totalTimelineFrames)
            playbackJob = viewModelScope.launch {
                while (isPlaying) {
                    delay(frameDelay)
                    val next = if (currentFrame >= maxFrame) 1 else currentFrame + 1
                    updateCurrentFrame(next)
                }
            }
        } else {
            playbackJob?.cancel()
        }
    }

    fun startStroke(worldX: Float, worldY: Float, pressure: Float) {
        if (isPlaying || activeLayerId == 0L) return
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
        drawNonce++ // Instantly triggers Canvas redraw for real-time stroke preview!
    }

    fun endStroke() {
        if (isPlaying || activeLayerId == 0L) return
        if (selectedTool == ToolType.PENCIL) {
            engine.endStroke()
        }
        hasUnsavedChanges = true
        invalidateCellCache()
    }

    fun undo() {
        if (activeLayerId == 0L) return
        engine.undo()
        drawingManager.clear(activeLayerId, currentFrame)
        hasUnsavedChanges = true
        invalidateCellCache()
    }

    fun clearCurrentFrame() {
        if (activeLayerId == 0L) return
        engine.clearCurrentFrame()
        drawingManager.clear(activeLayerId, currentFrame)
        hasUnsavedChanges = true
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
