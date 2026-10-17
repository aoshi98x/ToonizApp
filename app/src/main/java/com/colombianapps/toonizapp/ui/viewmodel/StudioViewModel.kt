package com.colombianapps.toonizapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uniffi.rust_core.AnimationEngine
import uniffi.rust_core.Stroke
import uniffi.rust_core.ToolType

class StudioViewModel : ViewModel() {
    val engine = AnimationEngine()

    var selectedTool by mutableStateOf(ToolType.PENCIL)
        private set

    var currentColor by mutableStateOf(Color.Black)
        private set

    var strokeWidth by mutableStateOf(8f)
        private set

    var scale by mutableStateOf(1f)
        private set

    var offsetX by mutableStateOf(0f)
        private set

    var offsetY by mutableStateOf(0f)
        private set

    var strokesVersion by mutableStateOf(0)
        private set

    var currentFrame by mutableStateOf(1)
        private set

    var activeLayerId by mutableStateOf(1L)
        private set

    var layers by mutableStateOf(engine.getLayers())
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentScreenPosition by mutableStateOf<Offset?>(null)
        private set

    var isPointerDown by mutableStateOf(false)
        private set

    private var playbackJob: Job? = null

    fun selectTool(tool: ToolType) {
        selectedTool = tool
    }

    fun selectColor(color: Color) {
        currentColor = color
    }

    fun updateStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun updateTransform(newScale: Float, newPanX: Float, newPanY: Float) {
        scale = newScale.coerceIn(0.2f, 10f)
        offsetX += newPanX
        offsetY += newPanY
    }

    fun updateCurrentFrame(frame: Int) {
        currentFrame = frame
        engine.setCurrentFrame(frame.toUInt())
        strokesVersion++
    }

    fun setActiveLayer(layerId: Long) {
        activeLayerId = layerId
        engine.setActiveLayer(layerId.toULong())
    }

    fun addLayer() {
        val newName = "Capa ${layers.size + 1}"
        val newId = engine.addLayer(newName)
        layers = engine.getLayers()
        setActiveLayer(newId.toLong())
        strokesVersion++
    }

    fun togglePlayback() {
        isPlaying = !isPlaying
        if (isPlaying) {
            playbackJob = viewModelScope.launch {
                while (isPlaying) {
                    delay(120L)
                    val next = if (currentFrame >= 24) 1 else currentFrame + 1
                    updateCurrentFrame(next)
                }
            }
        } else {
            playbackJob?.cancel()
        }
    }

    fun startStroke(worldX: Float, worldY: Float, pressure: Float) {
        if (isPlaying) return
        val colorInt = android.graphics.Color.argb(
            (currentColor.alpha * 255).toInt(),
            (currentColor.red * 255).toInt(),
            (currentColor.green * 255).toInt(),
            (currentColor.blue * 255).toInt()
        )
        if (selectedTool == ToolType.PENCIL) {
            engine.startStroke(worldX, worldY, pressure, selectedTool, colorInt.toUInt(), strokeWidth)
        } else {
            engine.eraseAt(worldX, worldY, strokeWidth * 2.5f)
        }
        strokesVersion++
    }

    fun addPoint(worldX: Float, worldY: Float, pressure: Float) {
        if (isPlaying) return
        if (selectedTool == ToolType.PENCIL) {
            engine.addPoint(worldX, worldY, pressure)
        } else {
            engine.eraseAt(worldX, worldY, strokeWidth * 2.5f)
        }
        strokesVersion++
    }

    fun endStroke() {
        if (isPlaying) return
        if (selectedTool == ToolType.PENCIL) {
            engine.endStroke()
        }
        strokesVersion++
    }

    fun undo() {
        engine.undo()
        strokesVersion++
    }

    fun clearCurrentFrame() {
        engine.clearCurrentFrame()
        strokesVersion++
    }

    fun getStrokes(): List<Stroke> {
        return engine.getStrokesForFrame(currentFrame.toUInt())
    }

    fun setPointerState(down: Boolean, pos: Offset?) {
        isPointerDown = down
        currentScreenPosition = pos
    }
}
