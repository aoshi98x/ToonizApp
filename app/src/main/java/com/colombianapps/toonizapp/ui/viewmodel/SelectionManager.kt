package com.colombianapps.toonizapp.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ClipboardEntry(
    val relativeFrameIndex: Int,
    val bitmap: Bitmap?,
    val exposure: Int
)

class SelectionManager(
    private val drawingManager: DrawingManager,
    private val getActiveLayerId: () -> Long,
    private val getCurrentFrame: () -> Int,
    private val onDeleteDrawingAtFrame: (Long, Int) -> Unit,
    private val onInvalidateCellCache: () -> Unit,
    private val onSetUnsavedChanges: (Boolean) -> Unit,
    private val onIncrementDrawNonce: () -> Unit,
    private val getCellSummary: (ULong, UInt) -> FrameCellSummary
) {
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedFrames by mutableStateOf<Set<Int>>(emptySet())
        private set

    var hasClipboardData by mutableStateOf(false)
        private set

    var copiedEntries by mutableStateOf<List<ClipboardEntry>>(emptyList())
        private set

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
        val layerId = getActiveLayerId()
        if (layerId == 0L || selectedFrames.isEmpty()) return
        val sorted = selectedFrames.sorted()
        val firstFrame = sorted.first()

        copiedEntries.forEach { it.bitmap?.recycle() }

        val entries = mutableListOf<ClipboardEntry>()
        for (f in sorted) {
            val bmp = drawingManager.copyBitmap(layerId, f)
            val summary = getCellSummary(layerId.toULong(), f.toUInt())
            entries.add(ClipboardEntry(f - firstFrame, bmp, summary.exposure))
        }
        copiedEntries = entries
        hasClipboardData = true
        clearSelection()
    }

    fun deleteSelectedFrames() {
        val layerId = getActiveLayerId()
        if (layerId == 0L || selectedFrames.isEmpty()) return
        val sortedFrames = selectedFrames.sortedDescending()
        for (f in sortedFrames) {
            onDeleteDrawingAtFrame(layerId, f)
        }
        onSetUnsavedChanges(true)
        onIncrementDrawNonce()
        clearSelection()
        onInvalidateCellCache()
    }
}
