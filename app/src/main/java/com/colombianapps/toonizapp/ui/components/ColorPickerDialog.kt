package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun ColorPickerDialog(viewModel: StudioViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.isColorPickerOpen = false },
        title = { Text("Selector de Color HSB") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(viewModel.currentColor)
                )

                // Hue Slider (0 - 360)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Tonalidad (Hue): ${viewModel.toolHue.toInt()}°", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = viewModel.toolHue,
                        onValueChange = { viewModel.setHue(it) },
                        valueRange = 0f..360f
                    )
                }

                // Saturation Slider (0 - 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Saturación: ${(viewModel.toolSaturation * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = viewModel.toolSaturation,
                        onValueChange = { viewModel.setSaturation(it) },
                        valueRange = 0f..1f
                    )
                }

                // Brightness Slider (0 - 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Brillo: ${(viewModel.toolBrightness * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = viewModel.toolBrightness,
                        onValueChange = { viewModel.setBrightness(it) },
                        valueRange = 0f..1f
                    )
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
