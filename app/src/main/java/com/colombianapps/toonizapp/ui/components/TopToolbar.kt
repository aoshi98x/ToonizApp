package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

@Composable
fun TopToolbar(viewModel: StudioViewModel, onBackClick: () -> Unit) {
    if (viewModel.isColorPickerOpen) {
        ColorWheelPickerDialog(viewModel = viewModel)
    }

    Surface(
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
        shape = RoundedCornerShape(4.dp),
        tonalElevation = 6.dp,
        color = Color(0xFF1F9998) // #1F9998 background for panel
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back Button to Project Manager
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Proyectos", modifier = Modifier.size(18.dp))
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.3f))

            // Selected Tool Name Badge
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = Color(0xFF00CCCB),
                modifier = Modifier.height(32.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val toolName = when (viewModel.selectedTool) {
                        ToolType.PENCIL -> "✏️ Lápiz"
                        ToolType.HARD_ERASER -> "🧽 B. Duro"
                        ToolType.SOFT_ERASER -> "🖌️ B. Suave"
                    }
                    Text(toolName, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1F3333))
                }
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.3f))

            // Thickness Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Grosor:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                Slider(
                    value = viewModel.strokeWidth.coerceIn(1f, 100f),
                    onValueChange = { viewModel.updateStrokeWidth(it) },
                    valueRange = 1f..100f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF00CCCB))
                )
                OutlinedTextField(
                    value = viewModel.strokeWidth.toInt().toString(),
                    onValueChange = {
                        val parsed = it.toFloatOrNull() ?: 1f
                        viewModel.updateStrokeWidth(parsed)
                    },
                    modifier = Modifier.width(55.dp).height(42.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Color Picker Button
            if (viewModel.selectedTool == ToolType.PENCIL) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(viewModel.currentColor)
                        .border(1.5.dp, Color.White, RoundedCornerShape(2.dp))
                        .clickable { viewModel.isColorPickerOpen = true }
                )
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.3f))

            // Hardness Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Dureza: ${(viewModel.strokeHardness * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                Slider(
                    value = viewModel.strokeHardness,
                    onValueChange = { viewModel.updateStrokeHardness(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(70.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF00CCCB))
                )
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.3f))

            // Onion Skin Menu
            OnionSkinMenu(viewModel = viewModel)

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.3f))

            // Save Button
            Button(
                onClick = { viewModel.saveProject() },
                enabled = viewModel.hasUnsavedChanges,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00CCCB),
                    contentColor = Color(0xFF1F3333),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Guardar", style = MaterialTheme.typography.labelSmall)
            }

            // Undo & Clear
            IconButton(
                onClick = { viewModel.undo() },
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Deshacer", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { viewModel.clearCurrentFrame() },
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar frame", modifier = Modifier.size(18.dp))
            }
        }
    }
}
