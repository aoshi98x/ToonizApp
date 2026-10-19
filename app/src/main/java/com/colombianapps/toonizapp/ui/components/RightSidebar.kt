package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

@Composable
fun RightSidebar(viewModel: StudioViewModel) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Herramientas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

            // Pencil Button
            Button(
                onClick = { viewModel.selectTool(ToolType.PENCIL) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.selectedTool == ToolType.PENCIL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (viewModel.selectedTool == ToolType.PENCIL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.width(110.dp).height(38.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("✏️ Lápiz", style = MaterialTheme.typography.bodySmall)
            }

            // Hard Eraser Button
            Button(
                onClick = { viewModel.selectTool(ToolType.HARD_ERASER) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.selectedTool == ToolType.HARD_ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (viewModel.selectedTool == ToolType.HARD_ERASER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.width(110.dp).height(38.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("🧽 B. Duro", style = MaterialTheme.typography.bodySmall)
            }

            // Soft Eraser Button
            Button(
                onClick = { viewModel.selectTool(ToolType.SOFT_ERASER) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.selectedTool == ToolType.SOFT_ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (viewModel.selectedTool == ToolType.SOFT_ERASER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.width(110.dp).height(38.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("🖌️ B. Suave", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
