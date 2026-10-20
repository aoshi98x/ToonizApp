package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable
fun OnionSkinMenu(viewModel: StudioViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = viewModel.onionSkinPast > 0 || viewModel.onionSkinFuture > 0

    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            shape = RoundedCornerShape(3.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) ActiveCyan else CardBg,
                contentColor = if (isActive) BgDark else Color.White
            ),
            border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Papel Cebolla", fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PanelBg).border(1.dp, BorderCyan, RoundedCornerShape(4.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Anteriores (Rojo): ${viewModel.onionSkinPast}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) },
                onClick = {}
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0..3) {
                    val isSelected = viewModel.onionSkinPast == i
                    OutlinedButton(
                        onClick = { viewModel.updateOnionSkinPast(i) },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) ActiveCyan else CardBg,
                            contentColor = if (isSelected) BgDark else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ActiveCyan else BorderCyan)
                    ) {
                        Text("$i", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderCyan)

            DropdownMenuItem(
                text = { Text("Siguientes (Verde): ${viewModel.onionSkinFuture}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) },
                onClick = {}
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0..3) {
                    val isSelected = viewModel.onionSkinFuture == i
                    OutlinedButton(
                        onClick = { viewModel.updateOnionSkinFuture(i) },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) ActiveCyan else CardBg,
                            contentColor = if (isSelected) BgDark else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ActiveCyan else BorderCyan)
                    ) {
                        Text("$i", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
