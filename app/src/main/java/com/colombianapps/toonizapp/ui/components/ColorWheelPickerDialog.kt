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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val DialogBg = Color(0xFF0D1E22)
private val ActiveCyan = Color(0xFF00CCCB)

@Composable
fun ColorWheelPickerDialog(viewModel: StudioViewModel) {
    val hueColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    AlertDialog(
        onDismissRequest = { viewModel.isColorPickerOpen = false },
        containerColor = DialogBg,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(6.dp),
        title = { Text("Rueda de Color Pro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
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
                        .clip(RoundedCornerShape(4.dp))
                        .background(viewModel.currentColor)
                )

                // Color Wheel & Saturation/Brightness Box Canvas with indicators
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
                                    val sqSize = radius * 0.8f
                                    val left = center.x - sqSize / 2f
                                    val top = center.y - sqSize / 2f
                                    val s = ((pos.x - left) / sqSize).coerceIn(0f, 1f)
                                    val b = (1f - ((pos.y - top) / sqSize)).coerceIn(0f, 1f)
                                    viewModel.setSaturation(s)
                                    viewModel.setBrightness(b)
                                } else if (dist <= radius) {
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

                        // 1. Draw Hue Ring in 1 GPU Draw Call using SweepGradient
                        val ringThickness = radius * 0.25f
                        val ringRadius = radius - (ringThickness / 2f)
                        val sweepBrush = Brush.sweepGradient(hueColors, center = center)
                        drawCircle(
                            brush = sweepBrush,
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = ringThickness)
                        )

                        // 2. Draw Saturation/Brightness Square in 2 GPU Draw Calls using Linear Gradients
                        val sqSize = radius * 0.8f
                        val left = center.x - sqSize / 2f
                        val top = center.y - sqSize / 2f
                        val sqSize2d = Size(sqSize, sqSize)
                        val sqTopLeft = Offset(left, top)

                        // Horizontal Saturation Gradient (White -> Pure Hue)
                        val pureHueColor = Color.hsv(viewModel.toolHue, 1f, 1f)
                        val satBrush = Brush.horizontalGradient(
                            colors = listOf(Color.White, pureHueColor),
                            startX = left,
                            endX = left + sqSize
                        )
                        drawRect(brush = satBrush, topLeft = sqTopLeft, size = sqSize2d)

                        // Vertical Brightness Gradient (Transparent -> Black)
                        val valBrush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = top,
                            endY = top + sqSize
                        )
                        drawRect(brush = valBrush, topLeft = sqTopLeft, size = sqSize2d)

                        // 3. Draw Hue Ring Indicator Handle
                        val hueRad = Math.toRadians(viewModel.toolHue.toDouble())
                        val hueHandleDist = ringRadius
                        val hueHandleCenter = Offset(
                            center.x + hueHandleDist * cos(hueRad).toFloat(),
                            center.y + hueHandleDist * sin(hueRad).toFloat()
                        )
                        drawCircle(color = Color.White, radius = 6f, center = hueHandleCenter)
                        drawCircle(color = Color.Black, radius = 4f, center = hueHandleCenter)

                        // 4. Draw SV Square Indicator Handle
                        val svHandleX = left + viewModel.toolSaturation * sqSize
                        val svHandleY = top + (1f - viewModel.toolBrightness) * sqSize
                        drawCircle(color = Color.White, radius = 6f, center = Offset(svHandleX, svHandleY))
                        drawCircle(color = Color.Black, radius = 4f, center = Offset(svHandleX, svHandleY))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.isColorPickerOpen = false },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = Color(0xFF071215))
            ) {
                Text("Aceptar", fontWeight = FontWeight.Bold)
            }
        }
    )
}
