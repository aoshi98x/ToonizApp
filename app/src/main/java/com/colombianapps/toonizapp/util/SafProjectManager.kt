package com.colombianapps.toonizapp.util

import android.content.Context
import android.net.Uri
import com.colombianapps.toonizapp.data.model.AnimationProject
import com.colombianapps.toonizapp.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object SafProjectManager {

    suspend fun exportProjectToUri(
        context: Context,
        uri: Uri,
        project: AnimationProject,
        repository: ProjectRepository
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packageFile = repository.exportProjectToPackage(project)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir la ruta de destino elegida"))

            outputStream.use { out ->
                packageFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (packageFile.exists()) {
                packageFile.delete()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importProjectFromUri(
        context: Context,
        uri: Uri,
        repository: ProjectRepository
    ): Result<AnimationProject> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado"))

            val tempFile = File(context.cacheDir, "import_saf_${UUID.randomUUID()}.tooniz")

            tempFile.outputStream().use { out ->
                inputStream.use { input ->
                    input.copyTo(out)
                }
            }

            val importedProject = repository.importProjectFromPackage(tempFile)
            if (tempFile.exists()) {
                tempFile.delete()
            }

            if (importedProject != null) {
                Result.success(importedProject)
            } else {
                Result.failure(Exception("El archivo seleccionado no es un paquete de proyecto Tooniz válido"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
