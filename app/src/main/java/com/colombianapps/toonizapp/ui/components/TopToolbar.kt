package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun TopToolbar(viewModel: StudioViewModel, onBackClick: () -> Unit) {
    if (viewModel.isColorPickerOpen) {
        ColorWheelPickerDialog(viewModel = viewModel)
    }

    Surface(
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
        shape = RoundedCornerShape(4.dp),
        color = PanelBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
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

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = BorderCyan)

            // Selected Tool Name Badge
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = ActiveCyan,
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
                    Text(toolName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BgDark)
                }
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = BorderCyan)

            // Thickness Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Grosor:", fontSize = 11.sp, color = TextMuted)
                Slider(
                    value = viewModel.strokeWidth.coerceIn(1f, 100f),
                    onValueChange = { viewModel.updateStrokeWidth(it) },
                    valueRange = 1f..100f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                )
                OutlinedTextField(
                    value = viewModel.strokeWidth.toInt().toString(),
                    onValueChange = {
                        val parsed = it.toFloatOrNull() ?: 1f
                        viewModel.updateStrokeWidth(parsed)
                    },
                    modifier = Modifier.width(68.dp).height(42.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(3.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedBorderColor = ActiveCyan,
                        unfocusedBorderColor = BorderCyan
                    )
                )
            }

            // Color Picker Button
            if (viewModel.selectedTool == ToolType.PENCIL) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(viewModel.currentColor)
                        .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                        .clickable { viewModel.isColorPickerOpen = true }
                )
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = BorderCyan)

            // Hardness Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Dureza: ${(viewModel.strokeHardness * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                Slider(
                    value = viewModel.strokeHardness,
                    onValueChange = { viewModel.updateStrokeHardness(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(70.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                )
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = BorderCyan)

            // Onion Skin Menu
            OnionSkinMenu(viewModel = viewModel)

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = BorderCyan)

            // Save Button
            Button(
                onClick = { viewModel.saveProject() },
                enabled = viewModel.hasUnsavedChanges,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActiveCyan,
                    contentColor = BgDark,
                    disabledContainerColor = CardBg,
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Undo, Redo & Clear
            IconButton(
                onClick = { viewModel.undo() },
                enabled = viewModel.canUndo,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = TextMuted.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer (Paso atrás)", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { viewModel.redo() },
                enabled = viewModel.canRedo,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = TextMuted.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer (Paso adelante)", modifier = Modifier.size(18.dp))
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
