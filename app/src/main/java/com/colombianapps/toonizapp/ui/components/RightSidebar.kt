package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import uniffi.rust_core.ToolType

private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun RightSidebar(viewModel: StudioViewModel) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = PanelBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("HERRAMIENTAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)

            // Pencil Button
            ToolButton(
                title = "✏️ Lápiz",
                isSelected = viewModel.selectedTool == ToolType.PENCIL,
                onClick = { viewModel.selectTool(ToolType.PENCIL) }
            )

            // Hard Eraser Button
            ToolButton(
                title = "🧽 B. Duro",
                isSelected = viewModel.selectedTool == ToolType.HARD_ERASER,
                onClick = { viewModel.selectTool(ToolType.HARD_ERASER) }
            )

            // Soft Eraser Button
            ToolButton(
                title = "🖌️ B. Suave",
                isSelected = viewModel.selectedTool == ToolType.SOFT_ERASER,
                onClick = { viewModel.selectTool(ToolType.SOFT_ERASER) }
            )
        }
    }
}

@Composable
private fun ToolButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ActiveCyan else CardBg,
            contentColor = if (isSelected) BgDark else Color.White
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
        modifier = Modifier.width(105.dp).height(36.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
