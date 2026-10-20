package com.colombianapps.toonizapp.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.colombianapps.toonizapp.data.model.AnimationProject
import com.colombianapps.toonizapp.data.repository.ProjectRepository
import uniffi.rust_core.AnimationEngine
import uniffi.rust_core.OnionStroke
import uniffi.rust_core.Stroke
import uniffi.rust_core.ToolType

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

    init {
        updateColorHsb(0f, 0f, 0f)
        startAutoSave()
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
    }

    fun saveProject() {
        currentProject?.let {
            projectRepository.saveProject(it)
            hasUnsavedChanges = false
        }
    }

    fun selectTool(tool: ToolType) {
        selectedTool = tool
    }

    fun selectColor(color: Color) {
        currentColor = color
        hasUnsavedChanges = true
    }

    fun updateColorHsb(h: Float, s: Float, b: Float) {
        toolHue = h
        toolSaturation = s
        toolBrightness = b
        currentColor = Color.hsv(h, s, b)
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
        hasUnsavedChanges = true
    }

    fun setSaturation(sat: Float) {
        toolSaturation = sat.coerceIn(0f, 1f)
        currentColor = Color.hsv(toolHue, toolSaturation, toolBrightness)
        hasUnsavedChanges = true
    }

    fun setBrightness(bri: Float) {
        toolBrightness = bri.coerceIn(0f, 1f)
        currentColor = Color.hsv(toolHue, toolSaturation, toolBrightness)
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
        strokesVersion++
    }

    fun expandTimeline() {
        if (totalTimelineFrames < 240) {
            totalTimelineFrames = (totalTimelineFrames + 24).coerceAtMost(240)
        }
    }

    fun setActiveLayer(layerId: Long) {
        activeLayerId = layerId
        engine.setActiveLayer(layerId.toULong())
        strokesVersion++
    }

    fun addLayer() {
        val newName = "Nivel ${layers.size + 1}"
        val newId = engine.addLayer(newName)
        layers = engine.getLayers()
        setActiveLayer(newId.toLong())
        hasUnsavedChanges = true
        strokesVersion++
    }

    fun deleteLayer(layerId: Long) {
        engine.deleteLayer(layerId.toULong())
        layers = engine.getLayers()
        activeLayerId = layers.firstOrNull()?.id?.toLong() ?: 0L
        engine.setActiveLayer(activeLayerId.toULong())
        hasUnsavedChanges = true
        strokesVersion++
    }

    fun adjustExposure(delta: Int) {
        if (activeLayerId == 0L) return
        engine.shiftExposure(activeLayerId.toULong(), currentFrame.toUInt(), delta)
        hasUnsavedChanges = true
        strokesVersion++
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
        val colorInt = android.graphics.Color.argb(
            (currentColor.alpha * 255).toInt(),
            (currentColor.red * 255).toInt(),
            (currentColor.green * 255).toInt(),
            (currentColor.blue * 255).toInt()
        )
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
        val colorInt = android.graphics.Color.argb(
            (currentColor.alpha * 255).toInt(),
            (currentColor.red * 255).toInt(),
            (currentColor.green * 255).toInt(),
            (currentColor.blue * 255).toInt()
        )
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
        strokesVersion++
    }

    fun undo() {
        if (activeLayerId == 0L) return
        engine.undo()
        drawingManager.clear(activeLayerId, currentFrame)
        hasUnsavedChanges = true
        strokesVersion++
    }

    fun clearCurrentFrame() {
        if (activeLayerId == 0L) return
        engine.clearCurrentFrame()
        drawingManager.clear(activeLayerId, currentFrame)
        hasUnsavedChanges = true
        strokesVersion++
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
