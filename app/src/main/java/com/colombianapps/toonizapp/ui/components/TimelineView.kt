package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val OccupiedTeal = Color(0xFF165257)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun TimelineView(viewModel: StudioViewModel) {
    val ignoredVersion = viewModel.strokesVersion
    if (ignoredVersion < -1) return

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = PanelBg,
            shape = RoundedCornerShape(4.dp),
            title = { Text("Eliminar Nivel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("¿Estás seguro de que deseas eliminar este nivel y todos sus dibujos?", color = TextMuted, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLayer(viewModel.activeLayerId)
                        showDeleteDialog = false
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        color = PanelBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = ActiveCyan)
                    ) {
                        Icon(
                            if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir"
                        )
                    }
                    Text(
                        "Frame: ${viewModel.currentFrame} / ${viewModel.totalTimelineFrames}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // -Exp button
                    Button(
                        onClick = { viewModel.adjustExposure(-1) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57373),
                            contentColor = Color.White
                        )
                    ) {
                        Text("-Exp", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // +Exp button
                    Button(
                        onClick = { viewModel.adjustExposure(1) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActiveCyan,
                            contentColor = BgDark
                        )
                    ) {
                        Text("+Exp", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Add Level button
                    Button(
                        onClick = { viewModel.addLayer() },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActiveCyan,
                            contentColor = BgDark
                        )
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Nivel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay niveles. Toca 'Añadir Nivel' para comenzar a animar.",
                        fontSize = 12.sp,
                        color = TextMuted
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
                                    fontSize = 10.sp,
                                    fontWeight = if (f == viewModel.currentFrame) FontWeight.Bold else FontWeight.Normal,
                                    color = if (f == viewModel.currentFrame) ActiveCyan else TextMuted
                                )
                            }
                        }
                    }

                    // Scrollable levels (Fixed at max 2 levels height: 68.dp)
                    val maxLevelsHeight = 68.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxLevelsHeight)
                            .verticalScroll(verticalScrollState),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        viewModel.layers.reversed().forEach { layer ->
                            val isLayerActive = layer.id.toLong() == viewModel.activeLayerId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isLayerActive) ActiveCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(if (isLayerActive) 1.dp else 0.dp, if (isLayerActive) BorderCyan else Color.Transparent, RoundedCornerShape(2.dp))
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
                                        tint = TextMuted
                                    )
                                    Text(
                                        text = layer.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isLayerActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLayerActive) ActiveCyan else Color.White,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isLayerActive) ActiveCyan else Color.Transparent)
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

                                        val summary = viewModel.getCellSummary(layer.id, f.toUInt())
                                        val hasStrokes = summary.hasStrokes
                                        val exposure = summary.exposure
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
                                                        isCellSelected -> ActiveCyan
                                                        hasStrokes -> OccupiedTeal
                                                        else -> CardBg
                                                    }
                                                )
                                                .border(1.dp, BorderCyan, RoundedCornerShape(2.dp))
                                                .clickable {
                                                    viewModel.setActiveLayer(layer.id.toLong())
                                                    viewModel.updateCurrentFrame(f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasStrokes) {
                                                Text(
                                                    text = "A$f" + if (exposure > 1) " (${exposure}f)" else "",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCellSelected) BgDark else Color.White
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
