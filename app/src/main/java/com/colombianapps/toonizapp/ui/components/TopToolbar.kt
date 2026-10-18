package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

@Composable
fun TopToolbar(viewModel: StudioViewModel) {
    Surface(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pencil Button
            IconButton(
                onClick = { viewModel.selectTool(ToolType.PENCIL) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (viewModel.selectedTool == ToolType.PENCIL) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Lápiz", tint = if (viewModel.selectedTool == ToolType.PENCIL) MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }

            // Eraser Button
            IconButton(
                onClick = { viewModel.selectTool(ToolType.ERASER) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (viewModel.selectedTool == ToolType.ERASER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = "Borrador", tint = if (viewModel.selectedTool == ToolType.ERASER) MaterialTheme.colorScheme.primary else LocalContentColor.current)
            }

            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))

            // Onion Skin Menu
            OnionSkinMenu(viewModel = viewModel)

            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))

            // Undo Button
            IconButton(onClick = { viewModel.undo() }) {
                Icon(Icons.Default.Undo, contentDescription = "Deshacer")
            }

            // Clear Button
            IconButton(onClick = { viewModel.clearCurrentFrame() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar frame actual")
            }
        }
    }
}
