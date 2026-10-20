package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimationCanvas(viewModel: StudioViewModel) {
    val focusManager = LocalFocusManager.current

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
                .pointerInput(viewModel.selectedTool, viewModel.isPlaying, viewModel.activeLayerId, viewModel.scale, viewModel.offsetX, viewModel.offsetY, viewModel.rotation) {
                    if (viewModel.isPlaying || viewModel.activeLayerId == 0L) return@pointerInput
                    val sheetWidth = size.width * 0.85f
                    val sheetHeight = size.height * 0.8f
                    val sheetLeft = (size.width - sheetWidth) / 2f
                    val sheetTop = (size.height - sheetHeight) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.pressed }
                            if (pointer != null && event.changes.size == 1) {
                                // Palm Rejection
                                if (pointer.type == PointerType.Touch && event.changes.any { it.type == PointerType.Stylus }) {
                                    pointer.consume()
                                    continue
                                }

                                focusManager.clearFocus()
                                val pos = pointer.position

                                // Inverse transform screen touch position to world canvas coordinates
                                val unPanX = pos.x - viewModel.offsetX - center.x
                                val unPanY = pos.y - viewModel.offsetY - center.y

                                val rad = Math.toRadians(-viewModel.rotation.toDouble())
                                val unRotX = unPanX * cos(rad).toFloat() - unPanY * sin(rad).toFloat()
                                val unRotY = unPanX * sin(rad).toFloat() + unPanY * cos(rad).toFloat()

                                val localX = (unRotX / viewModel.scale) + center.x
                                val localY = (unRotY / viewModel.scale) + center.y

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

            withTransform({
                translate(viewModel.offsetX, viewModel.offsetY)
                scale(viewModel.scale, viewModel.scale, Offset(size.width / 2f, size.height / 2f))
                rotate(viewModel.rotation, Offset(size.width / 2f, size.height / 2f))
            }) {
                // Pure white drawing canvas sheet
                drawRect(
                    color = Color.White,
                    topLeft = Offset(sheetLeft, sheetTop),
                    size = androidx.compose.ui.geometry.Size(sheetWidth, sheetHeight)
                )

                // 1. Render Onion Skins (Papel Cebolla) using Bitmaps
                if (viewModel.activeLayerId != 0L) {
                    // Past frames (Red tint)
                    var pastAlpha = 0.35f
                    for (i in 1..viewModel.onionSkinPast) {
                        val targetFrame = viewModel.currentFrame - i
                        if (targetFrame >= 1) {
                            val pastBmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, targetFrame)
                            drawImage(
                                pastBmp.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(sheetLeft.toInt(), sheetTop.toInt()),
                                colorFilter = ColorFilter.tint(Color.Red.copy(alpha = pastAlpha))
                            )
                        }
                        pastAlpha /= 3f
                    }

                    // Future frames (Green tint)
                    var futureAlpha = 0.35f
                    for (i in 1..viewModel.onionSkinFuture) {
                        val targetFrame = viewModel.currentFrame + i
                        if (targetFrame <= 24) {
                            val futureBmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, targetFrame)
                            drawImage(
                                futureBmp.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(sheetLeft.toInt(), sheetTop.toInt()),
                                colorFilter = ColorFilter.tint(Color.Green.copy(alpha = futureAlpha))
                            )
                        }
                        futureAlpha /= 3f
                    }

                    // 2. Render Current Frame Bitmap
                    val bmp = viewModel.drawingManager.getBitmap(viewModel.activeLayerId, viewModel.currentFrame)
                    drawImage(bmp.asImageBitmap(), dstOffset = androidx.compose.ui.unit.IntOffset(sheetLeft.toInt(), sheetTop.toInt()))
                }
            }
        }
    }
}
