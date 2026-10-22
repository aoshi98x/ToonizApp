package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.ui.viewmodel.CanvasTool
import com.colombianapps.toonizapp.ui.viewmodel.SelectionType
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import com.colombianapps.toonizapp.R
private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun RightSidebar(viewModel: StudioViewModel) {
    var showSelectionMenu by remember { mutableStateOf(false) }
    var showEraserMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = PanelBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Selection Tool Icon Button
            Box {
                IconButton(
                    onClick = {
                        showSelectionMenu = true
                        showEraserMenu = false
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (viewModel.activeTool == CanvasTool.SELECTION) ActiveCyan else CardBg)
                        .border(1.dp, if (viewModel.activeTool == CanvasTool.SELECTION) ActiveCyan else BorderCyan, RoundedCornerShape(3.dp))
                ) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = "Selección",
                        tint = if (viewModel.activeTool == CanvasTool.SELECTION) BgDark else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSelectionMenu,
                    onDismissRequest = { showSelectionMenu = false },
                    modifier = Modifier.background(PanelBg).border(1.dp, BorderCyan)
                ) {
                    DropdownMenuItem(
                        text = { Text("🔲 Selección Rectangular", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            viewModel.selectSelectionType(SelectionType.RECTANGLE)
                            showSelectionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📐 Selección Poligonal", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            viewModel.selectSelectionType(SelectionType.POLYGON)
                            showSelectionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("〰️ Selección Libre (Lazo)", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            viewModel.selectSelectionType(SelectionType.FREEHAND)
                            showSelectionMenu = false
                        }
                    )
                }
            }

            // 2. Pencil Icon Button
            IconButton(
                onClick = {
                    viewModel.selectCanvasTool(CanvasTool.PENCIL)
                    showSelectionMenu = false
                    showEraserMenu = false
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (viewModel.activeTool == CanvasTool.PENCIL) ActiveCyan else CardBg)
                    .border(1.dp, if (viewModel.activeTool == CanvasTool.PENCIL) ActiveCyan else BorderCyan, RoundedCornerShape(3.dp))
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Lápiz",
                    tint = if (viewModel.activeTool == CanvasTool.PENCIL) BgDark else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3. Eraser Icon Button
            Box {
                IconButton(
                    onClick = {
                        showEraserMenu = true
                        showSelectionMenu = false
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (viewModel.activeTool == CanvasTool.HARD_ERASER || viewModel.activeTool == CanvasTool.SOFT_ERASER) ActiveCyan else CardBg)
                        .border(1.dp, if (viewModel.activeTool == CanvasTool.HARD_ERASER || viewModel.activeTool == CanvasTool.SOFT_ERASER) ActiveCyan else BorderCyan, RoundedCornerShape(3.dp))
                ) {
                    Icon(
                        painter = painterResource(id= R.drawable.ink_eraser),
                        contentDescription = "Borrador",
                        tint = if (viewModel.activeTool == CanvasTool.HARD_ERASER || viewModel.activeTool == CanvasTool.SOFT_ERASER) BgDark else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showEraserMenu,
                    onDismissRequest = { showEraserMenu = false },
                    modifier = Modifier.background(PanelBg).border(1.dp, BorderCyan)
                ) {
                    DropdownMenuItem(
                        text = { Text("🧽 Borrador Duro", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            viewModel.selectCanvasTool(CanvasTool.HARD_ERASER)
                            showEraserMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🖌️ Borrador Suave", fontSize = 12.sp, color = Color.White) },
                        onClick = {
                            viewModel.selectCanvasTool(CanvasTool.SOFT_ERASER)
                            showEraserMenu = false
                        }
                    )
                }
            }
        }
    }
}
