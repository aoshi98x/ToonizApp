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
import androidx.compose.ui.graphics.graphicsLayer
import com.colombianapps.toonizapp.ui.viewmodel.CanvasTool
import com.colombianapps.toonizapp.ui.viewmodel.SelectionType
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun TopToolbar(viewModel: StudioViewModel, onBackClick: () -> Unit, onExportClick: () -> Unit) {
    if (viewModel.isColorPickerOpen) {
        ColorWheelPickerDialog(viewModel = viewModel)
    }

    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
        shape = RoundedCornerShape(4.dp),
        color = PanelBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hamburger Options Menu Button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menú de Opciones", modifier = Modifier.size(20.dp))
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(PanelBg).border(1.dp, BorderCyan)
                ) {
                    DropdownMenuItem(
                        text = { Text("💾 Guardar Proyecto", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            viewModel.saveProject()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🎬 Renderizar / Exportar", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            onExportClick()
                            showMenu = false
                        }
                    )
                    HorizontalDivider(color = BorderCyan)
                    DropdownMenuItem(
                        text = { Text("⬅️ Volver a Proyectos", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            onBackClick()
                            showMenu = false
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = BorderCyan)

            // DYNAMIC TOOL CONTROLS PANEL
            when (viewModel.activeTool) {
                CanvasTool.PENCIL -> {
                    // Badge
                    Surface(shape = RoundedCornerShape(3.dp), color = ActiveCyan, modifier = Modifier.height(30.dp)) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text("✏️ Lápiz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BgDark)
                        }
                    }

                    // Thickness
                    Text("Grosor:", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.strokeWidth.coerceIn(1f, 100f),
                        onValueChange = { viewModel.updateStrokeWidth(it) },
                        valueRange = 1f..100f,
                        modifier = Modifier.width(70.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(32.dp)
                            .background(CardBg, RoundedCornerShape(3.dp))
                            .border(1.dp, BorderCyan, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = viewModel.strokeWidth.toInt().toString(),
                            onValueChange = { viewModel.updateStrokeWidth(it.toFloatOrNull() ?: 1f) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        )
                    }

                    // Color Picker Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(viewModel.currentColor)
                            .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                            .clickable { viewModel.isColorPickerOpen = true }
                    )

                    // Hardness Slider
                    Text("Dureza: ${(viewModel.strokeHardness * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.strokeHardness,
                        onValueChange = { viewModel.updateStrokeHardness(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.width(60.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
                }
                CanvasTool.HARD_ERASER, CanvasTool.SOFT_ERASER -> {
                    // Badge
                    Surface(shape = RoundedCornerShape(3.dp), color = ActiveCyan, modifier = Modifier.height(30.dp)) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            val badge = if (viewModel.activeTool == CanvasTool.HARD_ERASER) "🧽 B. Duro" else "🖌️ B. Suave"
                            Text(badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BgDark)
                        }
                    }

                    // Thickness
                    Text("Grosor:", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.strokeWidth.coerceIn(1f, 100f),
                        onValueChange = { viewModel.updateStrokeWidth(it) },
                        valueRange = 1f..100f,
                        modifier = Modifier.width(80.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(32.dp)
                            .background(CardBg, RoundedCornerShape(3.dp))
                            .border(1.dp, BorderCyan, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = viewModel.strokeWidth.toInt().toString(),
                            onValueChange = { viewModel.updateStrokeWidth(it.toFloatOrNull() ?: 1f) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        )
                    }

                    // Hardness Slider
                    Text("Dureza: ${(viewModel.strokeHardness * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                    Slider(
                        value = viewModel.strokeHardness,
                        onValueChange = { viewModel.updateStrokeHardness(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.width(70.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ActiveCyan)
                    )
                }
                CanvasTool.SELECTION -> {
                    // Selection Badge
                    Surface(shape = RoundedCornerShape(3.dp), color = ActiveCyan, modifier = Modifier.height(30.dp)) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            val selLabel = when (viewModel.activeSelectionType) {
                                SelectionType.RECTANGLE -> "🔲 Selección Rectangular"
                                SelectionType.POLYGON -> "📐 Selección Poligonal"
                                SelectionType.FREEHAND -> "〰️ Selección Libre"
                            }
                            Text(selLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BgDark)
                        }
                    }

                    if (viewModel.isSelectionActive) {
                        // Mirror H
                        IconButton(onClick = { viewModel.flipSelectionHorizontal() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Flip, contentDescription = "Espejo Horizontal", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        // Mirror V
                        IconButton(onClick = { viewModel.flipSelectionVertical() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Flip,
                                contentDescription = "Espejo Vertical",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer(rotationZ = 90f)
                            )
                        }
                        // Clear Selection
                        Button(
                            onClick = { viewModel.clearSelectionBounds() },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = Color.White)
                        ) {
                            Text("Cancelar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        // Apply Selection
                        Button(
                            onClick = { viewModel.applySelectionTransform() },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = BgDark)
                        ) {
                            Text("Aplicar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Dibuja sobre el lienzo para seleccionar y transformar", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Undo, Redo & Clear Buttons
            IconButton(
                onClick = { viewModel.undo() },
                enabled = viewModel.canUndo,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White, disabledContentColor = TextMuted.copy(alpha = 0.4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { viewModel.redo() },
                enabled = viewModel.canRedo,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White, disabledContentColor = TextMuted.copy(alpha = 0.4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer", modifier = Modifier.size(18.dp))
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
