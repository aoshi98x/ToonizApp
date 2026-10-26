package com.colombianapps.toonizapp.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UndoRedoManager(
    private val drawingManager: DrawingManager,
    private val getActiveLayerId: () -> Long,
    private val getCurrentFrame: () -> Int,
    private val onInvalidateCellCache: () -> Unit,
    private val onSetUnsavedChanges: (Boolean) -> Unit,
    private val onIncrementDrawNonce: () -> Unit
) {
    private val undoMap = mutableMapOf<Long, ArrayDeque<Bitmap>>()
    private val redoMap = mutableMapOf<Long, ArrayDeque<Bitmap>>()

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    private fun makeKey(layerId: Long, frameIndex: Int): Long {
        return (layerId shl 32) or (frameIndex.toLong() and 0xFFFFFFFFL)
    }

    fun updateUndoRedoStates() {
        val layerId = getActiveLayerId()
        val frame = getCurrentFrame()
        val key = makeKey(layerId, frame)
        canUndo = (undoMap[key]?.isNotEmpty() == true)
        canRedo = (redoMap[key]?.isNotEmpty() == true)
    }

    fun recordStrokeStart() {
        val layerId = getActiveLayerId()
        val frame = getCurrentFrame()
        if (layerId == 0L) return
        val key = makeKey(layerId, frame)
        val undoStack = undoMap.getOrPut(key) { ArrayDeque() }
        val snapshot = drawingManager.copyBitmap(layerId, frame)
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
    }

    fun clear() {
        undoMap.values.forEach { stack -> stack.forEach { it.recycle() }; stack.clear() }
        undoMap.clear()
        redoMap.values.forEach { stack -> stack.forEach { it.recycle() }; stack.clear() }
        redoMap.clear()
        updateUndoRedoStates()
    }
}
