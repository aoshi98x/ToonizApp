package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun OnionSkinMenu(viewModel: StudioViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.onionSkinPast > 0 || viewModel.onionSkinFuture > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (viewModel.onionSkinPast > 0 || viewModel.onionSkinFuture > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Papel Cebolla", style = MaterialTheme.typography.bodySmall)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Anteriores (Rojo): ${viewModel.onionSkinPast}") },
                onClick = {}
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0..3) {
                    OutlinedButton(
                        onClick = { viewModel.updateOnionSkinPast(i) },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (viewModel.onionSkinPast == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text("$i", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text("Siguientes (Verde): ${viewModel.onionSkinFuture}") },
                onClick = {}
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0..3) {
                    OutlinedButton(
                        onClick = { viewModel.updateOnionSkinFuture(i) },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (viewModel.onionSkinFuture == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text("$i", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
