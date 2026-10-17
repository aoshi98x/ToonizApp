package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

@Composable
fun AnimationCanvas(viewModel: StudioViewModel) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    viewModel.updateTransform(viewModel.scale * zoom, pan.x, pan.y)
                }
            }
            .pointerInput(viewModel.selectedTool, viewModel.scale, viewModel.offsetX, viewModel.offsetY, viewModel.isPlaying) {
                if (viewModel.isPlaying) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.pressed }
                        if (pointer != null && event.changes.size == 1) {
                            viewModel.setPointerState(true, pointer.position)
                            val worldX = (pointer.position.x - viewModel.offsetX) / viewModel.scale
                            val worldY = (pointer.position.y - viewModel.offsetY) / viewModel.scale

                            if (pointer.pressed && !pointer.previousPressed) {
                                viewModel.startStroke(worldX, worldY, pointer.pressure)
                            } else if (pointer.pressed && pointer.position != pointer.previousPosition) {
                                viewModel.addPoint(worldX, worldY, pointer.pressure)
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
        val ignoredVersion = viewModel.strokesVersion
        if (ignoredVersion < 0) return@Canvas

        drawRect(color = Color(0xFFFAFAFA))

        val strokes = viewModel.getStrokes()

        drawContext.transform.apply {
            scale(viewModel.scale, viewModel.scale, Offset(0f, 0f))
            translate(viewModel.offsetX / viewModel.scale, viewModel.offsetY / viewModel.scale)
        }

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            val path = Path().apply {
                val first = stroke.points[0]
                moveTo(first.x, first.y)
                for (i in 1 until stroke.points.size) {
                    val p = stroke.points[i]
                    lineTo(p.x, p.y)
                }
            }

            if (stroke.toolType == ToolType.PENCIL) {
                val strokeColor = Color(stroke.color.toLong())
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Draw modern circular brush/eraser cursor
        if (viewModel.isPointerDown && viewModel.currentScreenPosition != null) {
            val cursorWorldX = (viewModel.currentScreenPosition!!.x - viewModel.offsetX) / viewModel.scale
            val cursorWorldY = (viewModel.currentScreenPosition!!.y - viewModel.offsetY) / viewModel.scale
            val radius = if (viewModel.selectedTool == ToolType.PENCIL) viewModel.strokeWidth / 2f else viewModel.strokeWidth * 2.5f

            drawCircle(
                color = if (viewModel.selectedTool == ToolType.PENCIL) viewModel.currentColor.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(cursorWorldX, cursorWorldY)
            )
            drawCircle(
                color = if (viewModel.selectedTool == ToolType.PENCIL) Color.DarkGray else Color.Red,
                radius = radius,
                style = Stroke(width = 2f / viewModel.scale),
                center = Offset(cursorWorldX, cursorWorldY)
            )
        }
    }
}
