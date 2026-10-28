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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import com.colombianapps.toonizapp.ui.viewmodel.CanvasTool
import com.colombianapps.toonizapp.ui.viewmodel.SelectionType
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class SheetBounds(
    val width: Float,
    val height: Float,
    val left: Float,
    val top: Float
)

private fun calculateSheetBounds(
    viewportWidth: Float,
    viewportHeight: Float,
    projectWidth: Int,
    projectHeight: Int
): SheetBounds {
    val projW = projectWidth.toFloat().coerceAtLeast(1f)
    val projH = projectHeight.toFloat().coerceAtLeast(1f)
    val projRatio = projW / projH

    val maxW = viewportWidth * 0.85f
    val maxH = viewportHeight * 0.80f

    val sheetW: Float
    val sheetH: Float

    if (maxW / maxH > projRatio) {
        sheetH = maxH
        sheetW = maxH * projRatio
    } else {
        sheetW = maxW
        sheetH = maxW / projRatio
    }

    val sheetL = (viewportWidth - sheetW) / 2f
    val sheetT = (viewportHeight - sheetH) / 2f

    return SheetBounds(sheetW, sheetH, sheetL, sheetT)
}

@Composable
fun AnimationCanvas(viewModel: StudioViewModel) {
    val focusManager = LocalFocusManager.current

    // Pre-calculate onion skin opacity steps: 50% for 1st adjacent drawing, 35% for 2nd, 20% for 3rd
    val pastAlphas = remember { floatArrayOf(0.50f, 0.35f, 0.20f) }
    val futureAlphas = remember { floatArrayOf(0.50f, 0.35f, 0.20f) }

    val dashedStroke = remember {
        Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF252525))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (viewModel.activeTool == CanvasTool.SELECTION && viewModel.isSelectionActive) {
                            val currentScale = viewModel.scale
                            val currentRotation = viewModel.rotation
                            val rad = Math.toRadians(-currentRotation.toDouble())
                            val cosRad = cos(rad).toFloat()
                            val sinRad = sin(rad).toFloat()

                            val rawDx = pan.x / currentScale
                            val rawDy = pan.y / currentScale
                            val panInSheetX = rawDx * cosRad - rawDy * sinRad
                            val panInSheetY = rawDx * sinRad + rawDy * cosRad

                            // Si el gesto es predominantemente rotación, cancelamos la traslación accidental (pan)
                            val isPureRotation = kotlin.math.abs(rotation) > 0.5f && hypot(panInSheetX, panInSheetY) < 5f
                            val cleanPanX = if (isPureRotation) 0f else panInSheetX
                            val cleanPanY = if (isPureRotation) 0f else panInSheetY

                            viewModel.transformSelectionByGesture(cleanPanX, cleanPanY, zoom, rotation)
                        } else {
                            viewModel.updateTransform(
                                viewModel.scale * zoom,
                                viewModel.offsetX + pan.x,
                                viewModel.offsetY + pan.y,
                                viewModel.rotation
                            )
                        }
                    }
                }
                .pointerInput(
                    viewModel.activeTool,
                    viewModel.activeSelectionType,
                    viewModel.isSelectionActive,
                    viewModel.selectedTool,
                    viewModel.isPlaying,
                    viewModel.activeLayerId,
                    viewModel.currentProject
                ) {
                    if (viewModel.isPlaying || viewModel.activeLayerId == 0L) return@pointerInput
                    val project = viewModel.currentProject
                    val bounds = calculateSheetBounds(
                        size.width.toFloat(),
                        size.height.toFloat(),
                        project?.canvasWidth ?: 1920,
                        project?.canvasHeight ?: 1080
                    )
                    val sheetWidth = bounds.width
                    val sheetHeight = bounds.height
                    val sheetLeft = bounds.left
                    val sheetTop = bounds.top
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
                                val timeSinceStylus = System.currentTimeMillis() - lastStylusTime
                                val isStylusCoolingDown = timeSinceStylus < 3500L

                                if (pointer.type == PointerType.Touch && (event.changes.any { it.type == PointerType.Stylus } || isStylusCoolingDown)) {
                                    pointer.consume()
                                    continue
                                }

                                focusManager.clearFocus()
                                val pos = pointer.position

                                val currentScale = viewModel.scale
                                val currentOffsetX = viewModel.offsetX
                                val currentOffsetY = viewModel.offsetY
                                val currentRotation = viewModel.rotation

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

                                val isInsideSheet = sheetX in 0f..sheetWidth && sheetY in 0f..sheetHeight

                                if (viewModel.activeTool == CanvasTool.SELECTION && viewModel.isSelectionActive) {
                                    // Handle drag & transform active selection box anywhere on screen
                                    viewModel.setPointerState(true, pos)
                                    if (pointer.pressed && !pointer.previousPressed) {
                                        viewModel.startHandleDrag(sheetX, sheetY)
                                    } else if (pointer.pressed && pointer.position != pointer.previousPosition) {
                                        val rawDx = (pointer.position.x - pointer.previousPosition.x) / currentScale
                                        val rawDy = (pointer.position.y - pointer.previousPosition.y) / currentScale
                                        val deltaX = rawDx * cosRad - rawDy * sinRad
                                        val deltaY = rawDx * sinRad + rawDy * cosRad
                                        viewModel.handleSelectionDrag(deltaX, deltaY, sheetX, sheetY)
                                    }
                                } else if (isInsideSheet) {
                                    viewModel.setPointerState(true, pos)

                                    if (viewModel.activeTool == CanvasTool.SELECTION) {
                                        // Drawing new selection boundary inside sheet
                                        when (viewModel.activeSelectionType) {
                                            SelectionType.RECTANGLE -> {
                                                if (pointer.pressed && !pointer.previousPressed) {
                                                    viewModel.selectionPoints.clear()
                                                    viewModel.selectionPoints.add(Offset(sheetX, sheetY))
                                                } else if (pointer.pressed) {
                                                    if (viewModel.selectionPoints.size < 2) {
                                                        viewModel.selectionPoints.add(Offset(sheetX, sheetY))
                                                    } else {
                                                        viewModel.selectionPoints[1] = Offset(sheetX, sheetY)
                                                    }
                                                }
                                            }
                                            SelectionType.POLYGON -> {
                                                if (pointer.pressed && !pointer.previousPressed) {
                                                    val pt = Offset(sheetX, sheetY)
                                                    if (viewModel.selectionPoints.size >= 3) {
                                                        val first = viewModel.selectionPoints.first()
                                                        if (hypot(pt.x - first.x, pt.y - first.y) < 30f) {
                                                            // Close polygon
                                                            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                                                            var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
                                                            for (p in viewModel.selectionPoints) {
                                                                minX = min(minX, p.x); minY = min(minY, p.y)
                                                                maxX = max(maxX, p.x); maxY = max(maxY, p.y)
                                                            }
                                                            viewModel.finalizeSelectionArea(Rect(minX, minY, maxX, maxY))
                                                        } else {
                                                            viewModel.selectionPoints.add(pt)
                                                        }
                                                    } else {
                                                        viewModel.selectionPoints.add(pt)
                                                    }
                                                }
                                            }
                                            SelectionType.FREEHAND -> {
                                                if (pointer.pressed && !pointer.previousPressed) {
                                                    viewModel.selectionPoints.clear()
                                                    viewModel.selectionPoints.add(Offset(sheetX, sheetY))
                                                } else if (pointer.pressed) {
                                                    viewModel.selectionPoints.add(Offset(sheetX, sheetY))
                                                }
                                            }
                                        }
                                    } else {
                                        // PENCIL / ERASER Drawing
                                        if (pointer.pressed && !pointer.previousPressed) {
                                            viewModel.startStroke(sheetX, sheetY, pointer.pressure)
                                        } else if (pointer.pressed && pointer.position != pointer.previousPosition) {
                                            viewModel.addPoint(sheetX, sheetY, pointer.pressure)
                                        }
                                    }
                                } else {
                                    viewModel.setPointerState(false, null)
                                }
                                pointer.consume()
                            } else {
                                viewModel.setPointerState(false, null)
                                if (event.changes.any { !it.pressed && it.previousPressed }) {
                                    if (viewModel.activeTool == CanvasTool.SELECTION && !viewModel.isSelectionActive) {
                                        if (viewModel.activeSelectionType == SelectionType.RECTANGLE && viewModel.selectionPoints.size == 2) {
                                            val p1 = viewModel.selectionPoints[0]
                                            val p2 = viewModel.selectionPoints[1]
                                            val bounds = Rect(min(p1.x, p2.x), min(p1.y, p2.y), max(p1.x, p2.x), max(p1.y, p2.y))
                                            viewModel.finalizeSelectionArea(bounds)
                                        } else if (viewModel.activeSelectionType == SelectionType.FREEHAND && viewModel.selectionPoints.size > 2) {
                                            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                                            var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
                                            for (p in viewModel.selectionPoints) {
                                                minX = min(minX, p.x); minY = min(minY, p.y)
                                                maxX = max(maxX, p.x); maxY = max(maxY, p.y)
                                            }
                                            viewModel.finalizeSelectionArea(Rect(minX, minY, maxX, maxY))
                                        }
                                    } else if (viewModel.activeTool != CanvasTool.SELECTION) {
                                        viewModel.endStroke()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val activeNonce = viewModel.drawNonce
            viewModel.drawingManager.setSize(size.width.toInt(), size.height.toInt())
            if (activeNonce < -1) return@Canvas

            val project = viewModel.currentProject
            val bounds = calculateSheetBounds(
                size.width,
                size.height,
                project?.canvasWidth ?: 1920,
                project?.canvasHeight ?: 1080
            )
            val sheetWidth = bounds.width
            val sheetHeight = bounds.height
            val sheetLeft = bounds.left
            val sheetTop = bounds.top
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

                // Render ALL visible layers simultaneously in stack Z-order (index 0 is bottom, last is top)
                for (layer in viewModel.layers) {
                    if (viewModel.isLayerVisible(layer.id.toLong())) {
                        val bmp = viewModel.drawingManager.findBitmap(layer.id.toLong(), viewModel.currentFrame)
                        if (bmp != null) {
                            val opacity = viewModel.getLayerOpacity(layer.id.toLong())
                            drawImage(
                                bmp.asImageBitmap(),
                                dstOffset = destOffset,
                                alpha = opacity
                            )
                        }
                    }
                }

                // Render Selection Extracted Bitmap & Transformation Handles
                if (viewModel.activeTool == CanvasTool.SELECTION) {
                    if (!viewModel.isSelectionActive && viewModel.selectionPoints.size >= 2) {
                        if (viewModel.activeSelectionType == SelectionType.RECTANGLE) {
                            val p1 = viewModel.selectionPoints[0]
                            val p2 = viewModel.selectionPoints[1]
                            val bounds = Rect(
                                min(p1.x, p2.x) + sheetLeft,
                                min(p1.y, p2.y) + sheetTop,
                                max(p1.x, p2.x) + sheetLeft,
                                max(p1.y, p2.y) + sheetTop
                            )
                            drawRect(
                                color = Color(0xFF00CCCB),
                                topLeft = Offset(bounds.left, bounds.top),
                                size = Size(bounds.width, bounds.height),
                                style = dashedStroke
                            )
                        } else {
                            for (i in 0 until viewModel.selectionPoints.size - 1) {
                                val start = viewModel.selectionPoints[i] + Offset(sheetLeft, sheetTop)
                                val end = viewModel.selectionPoints[i + 1] + Offset(sheetLeft, sheetTop)
                                drawLine(Color(0xFF00CCCB), start, end, strokeWidth = 2f)
                            }
                        }
                    }

                    val pivotInSheet = viewModel.getSelectionPivot()
                    if (viewModel.isSelectionActive && pivotInSheet != null && viewModel.selectionBitmap != null) {
                        val selBmp = viewModel.selectionBitmap!!

                        val scaledWidth = selBmp.width * viewModel.selectionScaleX
                        val scaledHeight = selBmp.height * viewModel.selectionScaleY
                        val centerX = sheetLeft + pivotInSheet.x
                        val centerY = sheetTop + pivotInSheet.y

                        val halfW = scaledWidth / 2f
                        val halfH = scaledHeight / 2f

                        val flipX = if (viewModel.isSelectionFlippedHorizontally) -1f else 1f
                        val flipY = if (viewModel.isSelectionFlippedVertically) -1f else 1f

                        // Transformación unificada sobre el pivote central (0,0)
                        withTransform({
                            // 1. Mueve el sistema de coordenadas al pivote (centro de la selección)
                            translate(centerX, centerY)
                            // 2. Rota en Z sobre el origen local (0, 0)
                            rotate(viewModel.selectionRotation, pivot = Offset.Zero)
                            // 3. Escala/espeja sobre el origen local (0, 0)
                            scale(viewModel.selectionScaleX * flipX, viewModel.selectionScaleY * flipY, pivot = Offset.Zero)
                        }) {
                            // Dibujar bitmap centrado en (0, 0)
                            drawImage(
                                selBmp.asImageBitmap(),
                                dstOffset = IntOffset(-selBmp.width / 2, -selBmp.height / 2)
                            )
                        }

                        // Dibujar marco y manejadores con la misma matriz
                        withTransform({
                            translate(centerX, centerY)
                            rotate(viewModel.selectionRotation, pivot = Offset.Zero)
                        }) {
                            // Caja punteada de selección centrada en (0, 0)
                            drawRect(
                                color = Color(0xFF00CCCB),
                                topLeft = Offset(-halfW, -halfH),
                                size = Size(scaledWidth, scaledHeight),
                                style = dashedStroke
                            )

                            // Esquinas (Manejadores de escala)
                            drawCircle(Color.White, radius = 11f, center = Offset(-halfW, -halfH))
                            drawCircle(Color(0xFF00CCCB), radius = 8f, center = Offset(-halfW, -halfH))

                            drawCircle(Color.White, radius = 11f, center = Offset(halfW, -halfH))
                            drawCircle(Color(0xFF00CCCB), radius = 8f, center = Offset(halfW, -halfH))

                            drawCircle(Color.White, radius = 11f, center = Offset(-halfW, halfH))
                            drawCircle(Color(0xFF00CCCB), radius = 8f, center = Offset(-halfW, halfH))

                            drawCircle(Color.White, radius = 11f, center = Offset(halfW, halfH))
                            drawCircle(Color(0xFF00CCCB), radius = 8f, center = Offset(halfW, halfH))

                            // Tallo y manejador circular de rotación
                            val stemTopY = -halfH - 35f
                            drawLine(Color(0xFF00CCCB), Offset(0f, -halfH), Offset(0f, stemTopY), strokeWidth = 2f)
                            drawCircle(Color(0xFF00CCCB), radius = 14f, center = Offset(0f, stemTopY))
                            drawCircle(Color.White, radius = 11f, center = Offset(0f, stemTopY))

                            val arcRect = Rect(Offset(-6f, stemTopY - 6f), Size(12f, 12f))
                            val rotatePath = androidx.compose.ui.graphics.Path().apply {
                                addArc(arcRect, 30f, 280f)
                            }
                            drawPath(rotatePath, color = Color(0xFF00CCCB), style = Stroke(width = 2.5f))

                            // Punto verde central (Pivote Z)
                            drawCircle(Color(0xFF00FF66), radius = 6f, center = Offset.Zero)
                            drawCircle(Color.White, radius = 8f, center = Offset.Zero, style = Stroke(width = 1.5f))
                        }
                    }
                }

                // Render Onion Skins ONLY for active layer if visible
                if (viewModel.activeLayerId != 0L && viewModel.isLayerVisible(viewModel.activeLayerId)) {
                    // Past Drawings (Red tint)
                    val pastFrames = viewModel.getPastDrawingFrames(viewModel.activeLayerId.toULong(), viewModel.currentFrame, viewModel.onionSkinPast)
                    pastFrames.forEachIndexed { index, targetFrame ->
                        val pastBmp = viewModel.drawingManager.findBitmap(viewModel.activeLayerId, targetFrame)
                        if (pastBmp != null) {
                            val alpha = pastAlphas.getOrElse(index) { 0.20f }
                            drawImage(
                                pastBmp.asImageBitmap(),
                                dstOffset = destOffset,
                                colorFilter = ColorFilter.tint(Color.Red.copy(alpha = alpha))
                            )
                        }
                    }

                    // Future Drawings (Green tint)
                    val futureFrames = viewModel.getFutureDrawingFrames(viewModel.activeLayerId.toULong(), viewModel.currentFrame, viewModel.onionSkinFuture)
                    futureFrames.forEachIndexed { index, targetFrame ->
                        val futureBmp = viewModel.drawingManager.findBitmap(viewModel.activeLayerId, targetFrame)
                        if (futureBmp != null) {
                            val alpha = futureAlphas.getOrElse(index) { 0.20f }
                            drawImage(
                                futureBmp.asImageBitmap(),
                                dstOffset = destOffset,
                                colorFilter = ColorFilter.tint(Color.Green.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }

            // Render Circular Tool Radius Cursor Guide Ring
            if (viewModel.activeTool != CanvasTool.SELECTION) {
                val targetPos = viewModel.currentScreenPosition
                if (targetPos != null) {
                    val baseSize = if (viewModel.selectedTool == ToolType.PENCIL) viewModel.strokeWidth else viewModel.strokeWidth * 2.5f
                    val radiusPx = (baseSize * viewModel.scale) / 2f

                    if (radiusPx > 0.5f) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.6f),
                            radius = radiusPx + 1f,
                            center = targetPos,
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = radiusPx,
                            center = targetPos,
                            style = Stroke(width = 1.5f)
                        )

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
}
