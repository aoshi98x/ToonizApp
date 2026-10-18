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
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Playback & Level Controls Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("Frame: ${viewModel.currentFrame} / 24", style = MaterialTheme.typography.titleSmall)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Exposure controls (+Exp / -Exp)
                    Button(
                        onClick = { viewModel.extendExposure(viewModel.currentFrame, -1) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("-Exp", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.extendExposure(viewModel.currentFrame, 1) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("+Exp", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.addLayer() },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Nivel", style = MaterialTheme.typography.bodySmall)
                    }

                    if (viewModel.layers.isNotEmpty() && viewModel.activeLayerId != 0L) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Nivel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // If 0 levels, show prompt
            if (viewModel.layers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay niveles. Toca 'Añadir Nivel' para comenzar a animar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                // OpenToonz Style Levels Rows & Frame X-Sheet with Vertical Scroll for 3+ Levels
                val horizontalScrollState = rememberScrollState()
                val verticalScrollState = rememberScrollState()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header numbers (1 to 24)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Spacer(modifier = Modifier.width(110.dp)) // space for level label
                        for (f in 1..24) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$f",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (f == viewModel.currentFrame) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }

                    // Scrollable container for levels (scrolls vertically if 3+ levels)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(verticalScrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        viewModel.layers.reversed().forEach { layer ->
                            val isLayerActive = layer.id.toLong() == viewModel.activeLayerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLayerActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { viewModel.setActiveLayer(layer.id.toLong()) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Level Label & Controls
                                Row(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = "Visible",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.DarkGray
                                    )
                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isLayerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isLayerActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    )
                                }

                                // Frame cells for this level with exposure bars
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    var skipCells = 0
                                    for (f in 1..24) {
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

                                        val cellWidth = (36.dp * exposure) + (3.dp * (exposure - 1).coerceAtLeast(0))

                                        Box(
                                            modifier = Modifier
                                                .width(cellWidth)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when {
                                                        isCellSelected -> MaterialTheme.colorScheme.primary
                                                        hasStrokes -> Color(0xFF3B7A57).copy(alpha = 0.9f)
                                                        else -> MaterialTheme.colorScheme.surface
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
                                                    text = "A$f" + if (exposure > 1) " ($exposure f)" else "",
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
