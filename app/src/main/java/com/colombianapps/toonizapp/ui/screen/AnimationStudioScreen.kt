package com.colombianapps.toonizapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colombianapps.toonizapp.ui.components.AnimationCanvas
import com.colombianapps.toonizapp.ui.components.RightSidebar
import com.colombianapps.toonizapp.ui.components.TimelineView
import com.colombianapps.toonizapp.ui.components.TopToolbar
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun AnimationStudioScreen(viewModel: StudioViewModel = viewModel(), onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas
        AnimationCanvas(viewModel = viewModel)

        // Top Toolbar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TopToolbar(viewModel = viewModel, onBackClick = onBackClick)
        }

        // Reset Transform Button (positioned below Top Toolbar to avoid overlap)
        if (viewModel.scale != 1f || viewModel.offsetX != 0f || viewModel.offsetY != 0f || viewModel.rotation != 0f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 16.dp)
                    .clickable { viewModel.resetTransform() },
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Reestablecer Vista", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        // Right Sidebar
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            RightSidebar(viewModel = viewModel)
        }

        // Bottom Timeline
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            TimelineView(viewModel = viewModel)
        }
    }
}
