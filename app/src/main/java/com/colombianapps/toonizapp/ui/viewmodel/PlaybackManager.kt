package com.colombianapps.toonizapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackManager(
    private val scope: CoroutineScope,
    private val getCurrentFrame: () -> Int,
    private val updateCurrentFrame: (Int) -> Unit,
    private val getFps: () -> Int,
    private val getMaxFrame: () -> Int,
    private val hasLayers: () -> Boolean
) {
    var isPlaying by mutableStateOf(false)
        private set

    private var playbackJob: Job? = null

    fun togglePlayback() {
        if (!hasLayers()) return
        if (isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        if (isPlaying || !hasLayers()) return
        isPlaying = true
        val fps = getFps().coerceAtLeast(1)
        val maxFrame = getMaxFrame()

        playbackJob = scope.launch {
            val startSystemTime = System.currentTimeMillis()
            var frameIndex = getCurrentFrame()
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

    fun stopPlayback() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
    }
}
