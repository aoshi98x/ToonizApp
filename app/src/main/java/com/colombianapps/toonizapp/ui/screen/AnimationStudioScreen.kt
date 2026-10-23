package com.colombianapps.toonizapp.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colombianapps.toonizapp.ui.components.AnimationCanvas
import com.colombianapps.toonizapp.ui.components.RightSidebar
import com.colombianapps.toonizapp.ui.components.TimelineView
import com.colombianapps.toonizapp.ui.components.TopToolbar
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

private val ActiveCyan = Color(0xFF00CCCB)
private val BorderCyan = Color(0xFF163B43)
private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)

@Composable
fun AnimationStudioScreen(
    viewModel: StudioViewModel = viewModel(),
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importImageToNewLayer(context, uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas
        AnimationCanvas(viewModel = viewModel)

        if (viewModel.isPlaying) {
            // Full Screen Playback Mode Banner
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clickable { viewModel.stopPlayback() },
                shape = RoundedCornerShape(20.dp),
                color = PanelBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, ActiveCyan),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ActiveCyan))
                    Text(
                        "REPRODUCIENDO · ${viewModel.currentProject?.fps ?: 24} FPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Frame ${viewModel.currentFrame} / ${viewModel.getMaxFrameWithStrokes()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActiveCyan
                    )
                    Button(
                        onClick = { viewModel.stopPlayback() },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = BgDark)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pausar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Standard Editing View: Toolbars, Sidebar, and Timeline
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TopToolbar(
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    onExportClick = onExportClick,
                    onImportImageClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            // Reset Transform Button
            if (viewModel.scale != 1f || viewModel.offsetX != 0f || viewModel.offsetY != 0f || viewModel.rotation != 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 16.dp)
                        .clickable { viewModel.resetTransform() },
                    shape = RoundedCornerShape(3.dp),
                    color = ActiveCyan,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Reestablecer Vista", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BgDark)
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

        // Saving / Loading Progress Overlay
        if (viewModel.isSaving || viewModel.isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PanelBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActiveCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = ActiveCyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = if (viewModel.isSaving) "Procesando el Proyecto..." else "Cargando Proyecto...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Por favor espera un momento mientras se procesan los datos.",
                                fontSize = 12.sp,
                                color = Color(0xFF8FA8B0)
                            )
                        }
                    }
                }
            }
        }
    }
}
