package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimationCanvas(viewModel: StudioViewModel) {
    val focusManager = LocalFocusManager.current

    // Pre-calculate onion skin opacity steps to avoid math in draw loop
    val pastAlphas = remember { floatArrayOf(0.35f, 0.35f / 3f, 0.35f / 9f) }
    val futureAlphas = remember { floatArrayOf(0.35f, 0.35f / 3f, 0.35f / 9f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF252525))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rot ->
                        viewModel.updateTransform(
                            viewModel.scale * zoom,
                            viewModel.offsetX + pan.x,
                            viewModel.offsetY + pan.y,
                            viewModel.rotation + rot
                        )
                    }
                }
                // Keyed ONLY on tool/state changes, NOT on scale/pan/rotation changes to avoid restarting pointer listener during pan/zoom!
                .pointerInput(viewModel.selectedTool, viewModel.isPlaying, viewModel.activeLayerId) {
                    if (viewModel.isPlaying || viewModel.activeLayerId == 0L) return@pointerInput
                    val sheetWidth = size.width * 0.85f
                    val sheetHeight = size.height * 0.8f
                    val sheetLeft = (size.width - sheetWidth) / 2f
                    val sheetTop = (size.height - sheetHeight) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    var lastStylusTime = 0L

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.type == PointerType.Stylus }) {
                                lastStylusTime = System.currentTimeMillis()
                            }

                            val pointer = event.changes.firstOrNull { it.pressed }
                            if (pointer != null && event.changes.size == 1) {
                                // Palm Rejection: 3.5s cooldown after stylus activity
                                val timeSinceStylus = System.currentTimeMillis() - lastStylusTime
                                val isStylusCoolingDown = timeSinceStylus < 3500L

                                if (pointer.type == PointerType.Touch && (event.changes.any { it.type == PointerType.Stylus } || isStylusCoolingDown)) {
                                    pointer.consume()
                                    continue
                                }

                                focusManager.clearFocus()
                                val pos = pointer.position

                                // Read current transform dynamically from viewModel
                                val currentScale = viewModel.scale
                                val currentOffsetX = viewModel.offsetX
                                val currentOffsetY = viewModel.offsetY
                                val currentRotation = viewModel.rotation

                                // Inverse transform screen touch position to world canvas coordinates
                                val unPanX = pos.x - currentOffsetX - center.x
                                val unPanY = pos.y - currentOffsetY - center.y

                                val rad = Math.toRadians(-currentRotation.toDouble())
                                val cosRad = cos(rad).toFloat()
                                val sinRad = sin(rad).toFloat()

                                val unRotX = unPanX * cosRad - unPanY * sinRad
                                val unRotY = unPanX * sinRad + unPanY * cosRad

                                val localX = (unRotX / currentScale) + center.x
                                val localY = (unRotY / currentScale) + center.y

                                val sheetX = localX - sheetLeft
                                val sheetY = localY - sheetTop

                                // Only draw if touch point is inside the white canvas sheet bounds!
                                val isInsideSheet = sheetX in 0f..sheetWidth && sheetY in 0f..sheetHeight

                                if (isInsideSheet) {
                                    viewModel.setPointerState(true, pos)

                                    if (pointer.pressed && !pointer.previousPressed) {
                                        viewModel.startStroke(sheetX, sheetY, pointer.pressure)
                                    } else if (pointer.pressed && pointer.position != pointer.previousPosition) {
                                        viewModel.addPoint(sheetX, sheetY, pointer.pressure)
                                    }
                                } else {
                                    viewModel.setPointerState(false, null)
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
            val activeNonce = viewModel.drawNonce
            viewModel.drawingManager.setSize(size.width.toInt(), size.height.toInt())
            if (activeNonce < -1) return@Canvas

            val sheetWidth = size.width * 0.85f
            val sheetHeight = size.height * 0.8f
            val sheetLeft = (size.width - sheetWidth) / 2f
            val sheetTop = (size.height - sheetHeight) / 2f
            val destOffset = IntOffset(sheetLeft.toInt(), sheetTop.toInt())

            withTransform({
                translate(viewModel.offsetX, viewModel.offsetY)
                scale(viewModel.scale, viewModel.scale, Offset(size.width / 2f, size.height / 2f))
                rotate(viewModel.rotation, Offset(size.width / 2f, size.height / 2f))
            }) {
                // Pure white drawing canvas sheet
                drawRect(
                    color = Color.White,
                    topLeft = Offset(sheetLeft, sheetTop),
                    size = Size(sheetWidth, sheetHeight)
                )

                // 1. Render Onion Skins (Papel Cebolla) using Bitmaps
                if (viewModel.activeLayerId != 0L) {
                    // Past frames (Red tint)
                    val pastCount = viewModel.onionSkinPast.coerceAtMost(3)
                    for (i in 1..pastCount) {
                        val targetFrame = viewModel.currentFrame - i
                        if (targetFrame >= 1) {
                            val pastBmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, targetFrame)
                            val alpha = pastAlphas[i - 1]
                            drawImage(
                                pastBmp.asImageBitmap(),
                                dstOffset = destOffset,
                                colorFilter = ColorFilter.tint(Color.Red.copy(alpha = alpha))
                            )
                        }
                    }

                    // Future frames (Green tint)
                    val futureCount = viewModel.onionSkinFuture.coerceAtMost(3)
                    for (i in 1..futureCount) {
                        val targetFrame = viewModel.currentFrame + i
                        if (targetFrame <= 24) {
                            val futureBmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, targetFrame)
                            val alpha = futureAlphas[i - 1]
                            drawImage(
                                futureBmp.asImageBitmap(),
                                dstOffset = destOffset,
                                colorFilter = ColorFilter.tint(Color.Green.copy(alpha = alpha))
                            )
                        }
                    }

                    // 2. Render Current Frame Bitmap
                    val bmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, viewModel.currentFrame)
                    drawImage(bmp.asImageBitmap(), dstOffset = destOffset)
                }
            }

            // 3. Render Circular Tool Radius Cursor Guide Ring
            val targetPos = viewModel.currentScreenPosition
            if (targetPos != null) {
                val baseSize = if (viewModel.selectedTool == ToolType.PENCIL) viewModel.strokeWidth else viewModel.strokeWidth * 2.5f
                val radiusPx = (baseSize * viewModel.scale) / 2f

                if (radiusPx > 0.5f) {
                    // Contrast Outer Shadow Ring
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = radiusPx + 1f,
                        center = targetPos,
                        style = Stroke(width = 2.5f)
                    )
                    // High-visibility Inner Ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = radiusPx,
                        center = targetPos,
                        style = Stroke(width = 1.5f)
                    )

                    // Soft Eraser Core Radius Ring
                    if (viewModel.selectedTool == ToolType.SOFT_ERASER) {
                        val coreRadius = radiusPx * viewModel.strokeHardness.coerceIn(0.05f, 0.95f)
                        drawCircle(
                            color = Color(0xFF00CCCB).copy(alpha = 0.8f),
                            radius = coreRadius,
                            center = targetPos,
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }
        }
    }
}
