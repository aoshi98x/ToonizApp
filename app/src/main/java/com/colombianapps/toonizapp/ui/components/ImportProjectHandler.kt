package com.colombianapps.toonizapp.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.colombianapps.toonizapp.data.model.AnimationProject
import com.colombianapps.toonizapp.data.repository.ProjectRepository
import com.colombianapps.toonizapp.util.SafProjectManager
import kotlinx.coroutines.launch

class SafFileHandlerState(
    val launchImportPicker: () -> Unit,
    val launchExportPicker: (AnimationProject) -> Unit,
    val isProcessing: Boolean
)

@Composable
fun rememberSafFileHandler(
    repository: ProjectRepository,
    onImportSuccess: (AnimationProject) -> Unit,
    onExportSuccess: (AnimationProject) -> Unit
): SafFileHandlerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var currentExportProject by remember { mutableStateOf<AnimationProject?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                val result = SafProjectManager.importProjectFromUri(context, uri, repository)
                isProcessing = false
                result.onSuccess { proj ->
                    Toast.makeText(context, "Proyecto '${proj.name}' importado correctamente", Toast.LENGTH_LONG).show()
                    onImportSuccess(proj)
                }.onFailure { e ->
                    Toast.makeText(context, e.localizedMessage ?: "Error al importar el proyecto", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val project = currentExportProject
        if (uri != null && project != null) {
            isProcessing = true
            scope.launch {
                val result = SafProjectManager.exportProjectToUri(context, uri, project, repository)
                isProcessing = false
                currentExportProject = null
                result.onSuccess {
                    Toast.makeText(context, "Proyecto '${project.name}' guardado exitosamente", Toast.LENGTH_LONG).show()
                    onExportSuccess(project)
                }.onFailure { e ->
                    Toast.makeText(context, e.localizedMessage ?: "Error al guardar el proyecto", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            currentExportProject = null
        }
    }

    return remember(repository) {
        SafFileHandlerState(
            launchImportPicker = {
                openDocumentLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
            },
            launchExportPicker = { project ->
                currentExportProject = project
                val cleanName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                createDocumentLauncher.launch("${cleanName}.tooniz")
            },
            isProcessing = isProcessing
        )
    }
}
