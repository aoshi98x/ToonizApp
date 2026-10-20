package com.colombianapps.toonizapp.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import com.colombianapps.toonizapp.R
import com.colombianapps.toonizapp.data.model.AnimationProject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Pro Studio Dark Cyan Palette
private val StudioBg = Color(0xFF081417)
private val PanelBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val BorderCyan = Color(0xFF163B43)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun ProjectManagerScreen(
    projects: List<AnimationProject>,
    onCreateProjectClick: () -> Unit,
    onProjectClick: (AnimationProject) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudioBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP BAR
            TopStudioBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onCreateNew = onCreateProjectClick
            )

            HorizontalDivider(color = BorderCyan, thickness = 1.dp)

            // 2. QUICK PRESETS & ACTIONS BAR
            QuickPresetsBar(
                onCreateNew = onCreateProjectClick,
                onSelectPreset = { name, w, h, fps ->
                    val proj = AnimationProject(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        canvasWidth = w,
                        canvasHeight = h,
                        fps = fps,
                        dpi = 300
                    )
                    onProjectClick(proj)
                }
            )

            HorizontalDivider(color = BorderCyan, thickness = 1.dp)

            // 3. MAIN PROJECTS LIST / GRID
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (filteredProjects.isEmpty()) {
                    EmptyStateView(
                        isSearching = searchQuery.isNotBlank(),
                        onCreateNew = onCreateProjectClick
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProjects) { project ->
                            ProjectCardItem(
                                project = project,
                                onOpen = { onProjectClick(project) },
                                onDelete = { onDeleteProject(project.id) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderCyan, thickness = 1.dp)

            // 4. BOTTOM STATUS BAR
            BottomStatusBar(projectCount = projects.size)
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT: TOP BAR
// -----------------------------------------------------------------------------
@Composable
private fun TopStudioBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
                    .border(1.dp, BorderCyan, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_tooniz),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tooniz Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActiveCyan)
                    ) {
                        Text("Pro", fontSize = 10.sp, color = ActiveCyan, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
                Text("Gestor de Proyectos de Animación", fontSize = 10.sp, color = TextMuted)
            }
        }

        // Search Bar & Create Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar proyecto...", fontSize = 12.sp, color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .width(240.dp)
                    .height(38.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedBorderColor = ActiveCyan,
                    unfocusedBorderColor = BorderCyan
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            )

            Button(
                onClick = onCreateNew,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = StudioBg)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Proyecto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT: QUICK PRESETS BAR
// -----------------------------------------------------------------------------
@Composable
private fun QuickPresetsBar(
    onCreateNew: () -> Unit,
    onSelectPreset: (String, Int, Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("PLANTILLAS RÁPIDAS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)

            val presets = listOf(
                Triple("16:9 FHD (1920x1080)", Pair(1920, 1080), 24),
                Triple("9:16 Story (1080x1920)", Pair(1080, 1920), 30),
                Triple("4K Cine (3840x2160)", Pair(3840, 2160), 24)
            )

            for ((label, size, fps) in presets) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable { onSelectPreset("Proyecto $label", size.first, size.second, fps) },
                    color = CardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
                ) {
                    Text(
                        text = "$label · $fps FPS",
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        TextButton(
            onClick = onCreateNew,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+ Personalizado", fontSize = 11.sp, color = ActiveCyan, fontWeight = FontWeight.Bold)
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT: PROJECT CARD ITEM
// -----------------------------------------------------------------------------
@Composable
private fun ProjectCardItem(
    project: AnimationProject,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.lastModified) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(project.lastModified))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpen() },
        shape = RoundedCornerShape(6.dp),
        color = PanelBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Title & Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar Proyecto",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Specs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .border(1.dp, BorderCyan, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RESOLUCIÓN", fontSize = 8.sp, color = TextMuted)
                    Text("${project.canvasWidth}x${project.canvasHeight}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FPS", fontSize = 8.sp, color = TextMuted)
                    Text("${project.fps} FPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ActiveCyan)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DPI", fontSize = 8.sp, color = TextMuted)
                    Text("${project.dpi} DPI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Footer Row: Last Modified & Open Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Text(dateStr, fontSize = 10.sp, color = TextMuted)
                }

                Text("Abrir Canvas >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ActiveCyan)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT: EMPTY STATE
// -----------------------------------------------------------------------------
@Composable
private fun EmptyStateView(isSearching: Boolean, onCreateNew: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
            .border(1.dp, BorderCyan, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, ActiveCyan, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_tooniz),
                    contentDescription = null,
                    modifier = Modifier.size(54.dp)
                )
            }

            Text(
                text = if (isSearching) "No se encontraron proyectos" else "No hay proyectos iniciados",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            Text(
                text = if (isSearching) "Intenta con otro término de búsqueda." else "Crea un proyecto en blanco o selecciona una plantilla rápida.",
                fontSize = 12.sp,
                color = TextMuted
            )

            if (!isSearching) {
                Button(
                    onClick = onCreateNew,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = StudioBg)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crear Nuevo Proyecto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT: BOTTOM STATUS BAR
// -----------------------------------------------------------------------------
@Composable
private fun BottomStatusBar(projectCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBg)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ActiveCyan))
            Text("ESTUDIO LISTO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("|  $projectCount ${if (projectCount == 1) "Proyecto Guardado" else "Proyectos Guardados"}", fontSize = 11.sp, color = TextMuted)
        }

        Text("Tooniz Studio Engine · Vector Core 64b", fontSize = 10.sp, color = TextMuted)
    }
}
