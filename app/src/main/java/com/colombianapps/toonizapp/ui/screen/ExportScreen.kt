package com.colombianapps.toonizapp.ui.screen

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import com.colombianapps.toonizapp.util.Mp4ExportManager
import com.colombianapps.toonizapp.util.ZipExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: StudioViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf("Listo para exportar") }
    var exportedFilePath by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("Renderizar y Exportar Animación", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelBg)
            )

            HorizontalDivider(color = BorderCyan, thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Project Summary Card
                val proj = viewModel.currentProject
                val maxFrames = viewModel.getMaxFrameWithStrokes()
                val fps = proj?.fps ?: 24
                val durationSec = maxFrames / fps

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PanelBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(proj?.name ?: "Proyecto sin título", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Resolución: ${proj?.canvasWidth ?: 1920}x${proj?.canvasHeight ?: 1080} | FPS: $fps | Cuadros: $maxFrames | Duración: ~${durationSec}s", fontSize = 12.sp, color = TextMuted)
                    }
                }

                // Option 1: Export ZIP
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PanelBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = ActiveCyan)
                            Text("Exportar Capas a Archivo ZIP (.zip)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                        Text("Exporta cada nivel en su propia carpeta con los dibujos PNG nombrados exactamente como se ven en la app (ej: Nivel 1/A1.png).", fontSize = 12.sp, color = TextMuted)

                        Button(
                            onClick = {
                                isExporting = true
                                statusMessage = "Generando archivo ZIP..."
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val file = ZipExportManager.exportToZip(context, viewModel) { progress ->
                                            exportProgress = progress
                                        }
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            exportedFilePath = file.absolutePath
                                            statusMessage = "¡ZIP exportado con éxito!"
                                            shareFile(context, file, "application/zip")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            statusMessage = "Error al exportar ZIP: ${e.localizedMessage}"
                                        }
                                    }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = BgDark)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generar y Compartir ZIP", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Option 2: Export MP4 Video
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PanelBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = ActiveCyan)
                            Text("Renderizar Animación a Video MP4 (.mp4)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                        Text("Renderiza todos los niveles en un video H.264 MP4 fluido a la velocidad exacta del proyecto (Hardware acelerado).", fontSize = 12.sp, color = TextMuted)

                        Button(
                            onClick = {
                                isExporting = true
                                statusMessage = "Renderizando video MP4..."
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val file = Mp4ExportManager.exportToMp4(context, viewModel) { progress ->
                                            exportProgress = progress
                                            statusMessage = "Renderizando MP4: ${(progress * 100).toInt()}%"
                                        }
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            exportedFilePath = file.absolutePath
                                            statusMessage = "¡Video MP4 renderizado con éxito!"
                                            shareFile(context, file, "video/mp4")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            statusMessage = "Error al renderizar MP4: ${e.localizedMessage}"
                                        }
                                    }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = BgDark)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Renderizar Video MP4", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = ActiveCyan,
                            trackColor = CardBg
                        )
                        Text(statusMessage, fontSize = 12.sp, color = ActiveCyan, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(statusMessage, fontSize = 12.sp, color = TextMuted)
                }

                if (exportedFilePath != null) {
                    Button(
                        onClick = {
                            val file = java.io.File(exportedFilePath!!)
                            val mime = if (exportedFilePath!!.endsWith(".zip")) "application/zip" else "video/mp4"
                            shareFile(context, file, mime)
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg, contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActiveCyan)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir Archivo Exportado")
                    }
                }
            }
        }
    }
}

private fun shareFile(context: android.content.Context, file: java.io.File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir exportación de Tooniz"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
