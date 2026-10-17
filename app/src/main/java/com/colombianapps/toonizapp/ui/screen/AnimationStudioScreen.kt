package com.colombianapps.toonizapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colombianapps.toonizapp.ui.components.AnimationCanvas
import com.colombianapps.toonizapp.ui.components.RightSidebar
import com.colombianapps.toonizapp.ui.components.TimelineView
import com.colombianapps.toonizapp.ui.components.TopToolbar
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

@Composable
fun AnimationStudioScreen(viewModel: StudioViewModel = viewModel()) {
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
            TopToolbar(viewModel = viewModel)
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
