package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ColorWheelPickerDialog(viewModel: StudioViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.isColorPickerOpen = false },
        title = { Text("Selector de Color Estilo Rueda") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(viewModel.currentColor)
                )

                // Color Wheel & Saturation/Brightness Box Canvas
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val pos = change.position
                                val dx = pos.x - center.x
                                val dy = pos.y - center.y
                                val dist = hypot(dx, dy)
                                val radius = size.width / 2f

                                if (dist <= radius * 0.75f) {
                                    // Inside SV Square
                                    val sqSize = radius * 0.8f
                                    val left = center.x - sqSize / 2f
                                    val top = center.y - sqSize / 2f
                                    val s = ((pos.x - left) / sqSize).coerceIn(0f, 1f)
                                    val b = (1f - ((pos.y - top) / sqSize)).coerceIn(0f, 1f)
                                    viewModel.setSaturation(s)
                                    viewModel.setBrightness(b)
                                } else if (dist <= radius) {
                                    // On Hue Ring
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    viewModel.setHue(angle)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        // Draw Hue Ring
                        for (angle in 0 until 360 step 2) {
                            val rad = Math.toRadians(angle.toDouble())
                            val hueColor = Color.hsv(angle.toFloat(), 1f, 1f)
                            val start = Offset(
                                center.x + (radius * 0.75f) * cos(rad).toFloat(),
                                center.y + (radius * 0.75f) * sin(rad).toFloat()
                            )
                            val end = Offset(
                                center.x + radius * cos(rad).toFloat(),
                                center.y + radius * sin(rad).toFloat()
                            )
                            drawLine(color = hueColor, start = start, end = end, strokeWidth = 6f)
                        }

                        // Draw Saturation/Brightness Square inside
                        val sqSize = radius * 0.8f
                        val left = center.x - sqSize / 2f
                        val top = center.y - sqSize / 2f

                        for (x in 0..sqSize.toInt() step 6) {
                            for (y in 0..sqSize.toInt() step 6) {
                                val s = x / sqSize
                                val b = 1f - (y / sqSize)
                                val cellColor = Color.hsv(viewModel.toolHue, s, b)
                                drawRect(
                                    color = cellColor,
                                    topLeft = Offset(left + x, top + y),
                                    size = androidx.compose.ui.geometry.Size(6f, 6f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.isColorPickerOpen = false }) {
                Text("Aceptar")
            }
        }
    )
}
