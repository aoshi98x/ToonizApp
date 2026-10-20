package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun TimelineView(viewModel: StudioViewModel) {
    val ignoredVersion = viewModel.strokesVersion
    if (ignoredVersion < -1) return

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Nivel") },
            text = { Text("¿Estás seguro de que deseas eliminar este nivel y todos sus dibujos?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLayer(viewModel.activeLayerId)
                    showDeleteDialog = false
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(4.dp)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        tonalElevation = 8.dp,
        color = Color(0xFF1F9998)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Playback & Level Controls Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir"
                        )
                    }
                    Text("Frame: ${viewModel.currentFrame} / ${viewModel.totalTimelineFrames}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // -Exp button
                    Button(
                        onClick = { viewModel.adjustExposure(-1) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57373),
                            contentColor = Color.White
                        )
                    ) {
                        Text("-Exp", style = MaterialTheme.typography.labelSmall)
                    }

                    // +Exp button
                    Button(
                        onClick = { viewModel.adjustExposure(1) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F3333)
                        )
                    ) {
                        Text("+Exp", style = MaterialTheme.typography.labelSmall)
                    }

                    // Add Level button
                    Button(
                        onClick = { viewModel.addLayer() },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00CCCB),
                            contentColor = Color(0xFF1F3333)
                        )
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Nivel", style = MaterialTheme.typography.bodySmall)
                    }

                    if (viewModel.layers.isNotEmpty() && viewModel.activeLayerId != 0L) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(28.dp),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Nivel", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // If 0 levels, show prompt
            if (viewModel.layers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay niveles. Toca 'Añadir Nivel' para comenzar a animar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val verticalScrollState = rememberScrollState()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header numbers (1 to 240)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Spacer(modifier = Modifier.width(100.dp))
                        for (f in 1..viewModel.totalTimelineFrames) {
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$f",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (f == viewModel.currentFrame) Color(0xFF00CCCB) else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Scrollable levels
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .verticalScroll(verticalScrollState),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        viewModel.layers.reversed().forEach { layer ->
                            val isLayerActive = layer.id.toLong() == viewModel.activeLayerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isLayerActive) Color(0xFF00CCCB).copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { viewModel.setActiveLayer(layer.id.toLong()) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Level Label & Controls
                                Row(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = "Visible",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isLayerActive) Color(0xFF00CCCB) else Color.White,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isLayerActive) Color(0xFF00CCCB) else Color.Transparent)
                                    )
                                }

                                // Frame cells (1 to 240)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    var skipCells = 0
                                    for (f in 1..viewModel.totalTimelineFrames) {
                                        if (skipCells > 0) {
                                            skipCells--
                                            continue
                                        }

                                        val strokesInCell = viewModel.engine.getStrokesForLayerAndFrame(layer.id, f.toUInt())
                                        val hasStrokes = strokesInCell.isNotEmpty()
                                        val exposure = if (hasStrokes) strokesInCell.first().exposure.toInt() else 1
                                        val isCellSelected = f == viewModel.currentFrame && isLayerActive

                                        if (hasStrokes && exposure > 1) {
                                            skipCells = exposure - 1
                                        }

                                        val cellWidth = (34.dp * exposure) + (2.dp * (exposure - 1).coerceAtLeast(0))

                                        Box(
                                            modifier = Modifier
                                                .width(cellWidth)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    when {
                                                        isCellSelected -> Color(0xFF00CCCB)
                                                        hasStrokes -> Color(0xFF296666)
                                                        else -> Color(0xFF1F3333)
                                                    }
                                                )
                                                .clickable {
                                                    viewModel.setActiveLayer(layer.id.toLong())
                                                    viewModel.updateCurrentFrame(f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasStrokes) {
                                                Text(
                                                    text = "A$f" + if (exposure > 1) " (${exposure}f)" else "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
