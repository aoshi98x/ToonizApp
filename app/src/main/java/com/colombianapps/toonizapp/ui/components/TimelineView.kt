package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun TimelineView(viewModel: StudioViewModel) {
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
            // Playback & Layer Controls Header
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

                Button(
                    onClick = { viewModel.addLayer() },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir Capa", style = MaterialTheme.typography.bodySmall)
                }
            }

            // OpenToonz Style Layer Rows & Frame X-Sheet
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header numbers (1 to 24)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Spacer(modifier = Modifier.width(110.dp)) // space for layer label
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

                // Layers rows (rendered in reverse so top layer is at the top of list, matching OpenToonz order)
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
                        // Layer Label & Controls
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

                        // Frame cells for this layer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for (f in 1..24) {
                                val isCellSelected = f == viewModel.currentFrame && isLayerActive
                                val hasStrokesInCell = viewModel.engine.getStrokesForLayerAndFrame(layer.id, f.toUInt()).isNotEmpty()

                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isCellSelected -> MaterialTheme.colorScheme.primary
                                                hasStrokesInCell -> Color(0xFF3B7A57).copy(alpha = 0.9f)
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .clickable {
                                            viewModel.setActiveLayer(layer.id.toLong())
                                            viewModel.updateCurrentFrame(f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasStrokesInCell) {
                                        Text(
                                            text = "1",
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
