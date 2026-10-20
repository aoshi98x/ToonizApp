package com.colombianapps.toonizapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.data.model.AnimationProject
import java.util.UUID

private val DialogBg = Color(0xFF0D1E22)
private val CardBg = Color(0xFF0A181C)
private val ActiveCyan = Color(0xFF00CCCB)
private val BorderCyan = Color(0xFF163B43)
private val TextMuted = Color(0xFF8FA8B0)

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
        containerColor = DialogBg,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(6.dp),
        title = {
            Text("Crear Nuevo Proyecto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Proyecto", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedBorderColor = ActiveCyan,
                        unfocusedBorderColor = BorderCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Ancho (px)", fontSize = 11.sp, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            focusedBorderColor = ActiveCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("Alto (px)", fontSize = 11.sp, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            focusedBorderColor = ActiveCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fpsStr,
                        onValueChange = { fpsStr = it },
                        label = { Text("FPS", fontSize = 11.sp, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            focusedBorderColor = ActiveCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = dpiStr,
                        onValueChange = { dpiStr = it },
                        label = { Text("DPI", fontSize = 11.sp, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            focusedBorderColor = ActiveCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val project = AnimationProject(
                        id = UUID.randomUUID().toString(),
                        name = if (name.isBlank()) "Proyecto Sin Nombre" else name,
                        canvasWidth = widthStr.toIntOrNull() ?: 1920,
                        canvasHeight = heightStr.toIntOrNull() ?: 1080,
                        fps = fpsStr.toIntOrNull() ?: 24,
                        dpi = dpiStr.toIntOrNull() ?: 300
                    )
                    onCreate(project)
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveCyan, contentColor = Color(0xFF081417))
            ) {
                Text("Crear y Abrir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, BorderCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancelar")
            }
        }
    )
}
