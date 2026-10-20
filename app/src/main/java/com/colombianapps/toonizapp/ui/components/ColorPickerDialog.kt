package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

private val DialogBg = Color(0xFF0D1E22)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun ColorPickerDialog(viewModel: StudioViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.isColorPickerOpen = false },
        containerColor = DialogBg,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(6.dp),
        title = { Text("Selector de Color HSB", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(viewModel.currentColor)
                )

                // Hue Slider (0 - 360)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Tonalidad (Hue): ${viewModel.toolHue.toInt()}°", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.toolHue,
                        onValueChange = { viewModel.setHue(it) },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
                }

                // Saturation Slider (0 - 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Saturación: ${(viewModel.toolSaturation * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.toolSaturation,
                        onValueChange = { viewModel.setSaturation(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
                }

                // Brightness Slider (0 - 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Brillo: ${(viewModel.toolBrightness * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.toolBrightness,
                        onValueChange = { viewModel.setBrightness(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
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
