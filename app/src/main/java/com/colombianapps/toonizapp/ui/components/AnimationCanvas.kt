package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

@Composable
fun AnimationCanvas(viewModel: StudioViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF252525)) // Dark workspace background
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewModel.selectedTool, viewModel.isPlaying, viewModel.activeLayerId) {
                    if (viewModel.isPlaying || viewModel.activeLayerId == 0L) return@pointerInput
                    val sheetWidth = size.width * 0.85f
                    val sheetHeight = size.height * 0.8f
                    val sheetLeft = (size.width - sheetWidth) / 2f
                    val sheetTop = (size.height - sheetHeight) / 2f

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.pressed }
                            if (pointer != null && event.changes.size == 1) {
                                val pos = pointer.position
                                // Direct sheet-relative coordinate mapping
                                val sheetX = pos.x - sheetLeft
                                val sheetY = pos.y - sheetTop

                                viewModel.setPointerState(true, pos)

                                if (pointer.pressed && !pointer.previousPressed) {
                                    viewModel.startStroke(sheetX, sheetY, pointer.pressure)
                                } else if (pointer.pressed && pointer.position != pointer.previousPosition) {
                                    viewModel.addPoint(sheetX, sheetY, pointer.pressure)
                                }
                                pointer.consume()
                            } else {
                                viewModel.setPointerState(false, null)
                                if (event.changes.any { !it.pressed && it.previousPressed }) {
                                    viewModel.endStroke()
                                }
                            }
                        }
                    }
                }
        ) {
            viewModel.drawingManager.setSize(size.width.toInt(), size.height.toInt())
            val ignoredVersion = viewModel.strokesVersion
            if (ignoredVersion < 0) return@Canvas

            val sheetWidth = size.width * 0.85f
            val sheetHeight = size.height * 0.8f
            val sheetLeft = (size.width - sheetWidth) / 2f
            val sheetTop = (size.height - sheetHeight) / 2f

            // Pure white drawing canvas sheet
            drawRect(
                color = Color.White,
                topLeft = Offset(sheetLeft, sheetTop),
                size = androidx.compose.ui.geometry.Size(sheetWidth, sheetHeight)
            )

            // Render current active layer bitmap inside sheet area at correct offset
            if (viewModel.activeLayerId != 0L) {
                val bmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, viewModel.currentFrame)
                drawImage(bmp.asImageBitmap(), dstOffset = androidx.compose.ui.unit.IntOffset(sheetLeft.toInt(), sheetTop.toInt()))
            }

            // Draw modern circular brush/eraser cursor
            if (viewModel.isPointerDown && viewModel.currentScreenPosition != null && viewModel.activeLayerId != 0L) {
                val pos = viewModel.currentScreenPosition!!
                val radius = if (viewModel.selectedTool == ToolType.PENCIL) viewModel.strokeWidth / 2f else viewModel.strokeWidth * 2.5f

                drawCircle(
                    color = if (viewModel.selectedTool == ToolType.PENCIL) viewModel.currentColor.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f),
                    radius = radius,
                    center = pos
                )
                drawCircle(
                    color = if (viewModel.selectedTool == ToolType.PENCIL) Color.DarkGray else Color.Red,
                    radius = radius,
                    style = Stroke(width = 2f),
                    center = pos
                )
            }
        }
    }
}
