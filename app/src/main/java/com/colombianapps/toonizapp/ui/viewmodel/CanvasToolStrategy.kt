package com.colombianapps.toonizapp.ui.viewmodel

import uniffi.rust_core.AnimationEngine
import uniffi.rust_core.ToolType

interface DrawingToolStrategy {
    val toolType: ToolType
    fun startStroke(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int,
        x: Float,
        y: Float,
        pressure: Float,
        colorArgb: Int,
        strokeWidth: Float,
        hardness: Float
    )
    fun addPoint(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int,
        x: Float,
        y: Float,
        pressure: Float,
        colorArgb: Int,
        strokeWidth: Float,
        hardness: Float
    )
    fun endStroke(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int
    )
}

class PencilToolStrategy : DrawingToolStrategy {
    override val toolType: ToolType = ToolType.PENCIL

    override fun startStroke(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int,
        x: Float,
        y: Float,
        pressure: Float,
        colorArgb: Int,
        strokeWidth: Float,
        hardness: Float
    ) {
        engine.startStroke(x, y, pressure, toolType, colorArgb.toUInt(), strokeWidth)
        drawingManager.startPath(x, y, pressure)
    }

    override fun addPoint(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int,
        x: Float,
        y: Float,
        pressure: Float,
        colorArgb: Int,
        strokeWidth: Float,
        hardness: Float
    ) {
        engine.addPoint(x, y, pressure)
        drawingManager.addPathPoint(layerId, frameIndex, x, y, pressure, colorArgb, strokeWidth, toolType, hardness)
    }

    override fun endStroke(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int
    ) {
        engine.endStroke()
    }
}

class EraserToolStrategy(override val toolType: ToolType) : DrawingToolStrategy {
    override fun startStroke(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int,
        x: Float,
        y: Float,
        pressure: Float,
        colorArgb: Int,
        strokeWidth: Float,
        hardness: Float
    ) {
        val isSoft = (toolType == ToolType.SOFT_ERASER)
        engine.eraseAt(x, y, strokeWidth * 2.5f, hardness, isSoft)
        drawingManager.startPath(x, y, pressure)
    }

    override fun addPoint(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int,
        x: Float,
        y: Float,
        pressure: Float,
        colorArgb: Int,
        strokeWidth: Float,
        hardness: Float
    ) {
        val isSoft = (toolType == ToolType.SOFT_ERASER)
        engine.eraseAt(x, y, strokeWidth * 2.5f, hardness, isSoft)
        drawingManager.addPathPoint(layerId, frameIndex, x, y, pressure, 0, strokeWidth * 2.5f, toolType, hardness)
    }

    override fun endStroke(
        engine: AnimationEngine,
        drawingManager: DrawingManager,
        layerId: Long,
        frameIndex: Int
    ) {
        // Eraser doesn't require engine.endStroke()
    }
}

class DrawingToolContext {
    private val strategies = mapOf<ToolType, DrawingToolStrategy>(
        ToolType.PENCIL to PencilToolStrategy(),
        ToolType.HARD_ERASER to EraserToolStrategy(ToolType.HARD_ERASER),
        ToolType.SOFT_ERASER to EraserToolStrategy(ToolType.SOFT_ERASER)
    )

    fun getStrategy(toolType: ToolType): DrawingToolStrategy {
        return strategies[toolType] ?: strategies[ToolType.PENCIL]!!
    }
}
