package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun RightSidebar(viewModel: StudioViewModel) {
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Magenta, Color.White)
    val widths = listOf(4f, 8f, 16f, 24f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Colors
            colors.forEach { col ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                            width = if (viewModel.currentColor == col) 3.dp else 1.dp,
                            color = if (viewModel.currentColor == col) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { viewModel.selectColor(col) }
                )
            }

            HorizontalDivider(modifier = Modifier.width(24.dp).height(1.dp))

            // Widths
            widths.forEach { w ->
                Button(
                    onClick = { viewModel.updateStrokeWidth(w) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.strokeWidth == w) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (viewModel.strokeWidth == w) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("${w.toInt()}")
                }
            }
        }
    }
}
