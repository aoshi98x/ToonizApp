package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.colombianapps.toonizapp.data.model.AnimationProject
import java.util.UUID

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (AnimationProject) -> Unit
) {
    var name by remember { mutableStateOf("Mi Animación") }
    var widthStr by remember { mutableStateOf("1920") }
    var heightStr by remember { mutableStateOf("1080") }
    var fpsStr by remember { mutableStateOf("24") }
    var dpiStr by remember { mutableStateOf("300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Proyecto") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Proyecto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Ancho Lienzo (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("Alto Lienzo (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fpsStr,
                        onValueChange = { fpsStr = it },
                        label = { Text("FPS (Reproducción)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dpiStr,
                        onValueChange = { dpiStr = it },
                        label = { Text("DPI") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val project = AnimationProject(
                    id = UUID.randomUUID().toString(),
                    name = if (name.isBlank()) "Proyecto Sin Nombre" else name,
                    canvasWidth = widthStr.toIntOrNull() ?: 1920,
                    canvasHeight = heightStr.toIntOrNull() ?: 1080,
                    fps = fpsStr.toIntOrNull() ?: 24,
                    dpi = dpiStr.toIntOrNull() ?: 300
                )
                onCreate(project)
            }) {
                Text("Crear y Abrir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
