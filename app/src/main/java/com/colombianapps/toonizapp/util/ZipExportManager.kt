package com.colombianapps.toonizapp.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExportManager {

    fun exportToZip(
        context: Context,
        viewModel: StudioViewModel,
        onProgress: (Float) -> Unit
    ): File {
        val project = viewModel.currentProject ?: throw IllegalStateException("No hay proyecto activo")
        
        // Export folder in Downloads
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "ToonizExports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val cleanName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val zipFile = File(exportDir, "${cleanName}_Capas.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }

        val zipOut = ZipOutputStream(FileOutputStream(zipFile))
        val layers = viewModel.layers
        val maxFrame = viewModel.getMaxFrameWithStrokes()
        var processedCount = 0
        val totalTasks = (layers.size * maxFrame).coerceAtLeast(1)

        for (layer in layers) {
            val layerFolderName = layer.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            // Folder entry for layer
            try {
                zipOut.putNextEntry(ZipEntry("$layerFolderName/"))
                zipOut.closeEntry()
            } catch (_: Exception) {}

            for (f in 1..maxFrame) {
                val bmp = viewModel.drawingManager.findBitmap(layer.id.toLong(), f)
                if (bmp != null) {
                    val entryPath = "$layerFolderName/A$f.png"
                    val entry = ZipEntry(entryPath)
                    zipOut.putNextEntry(entry)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                    zipOut.closeEntry()
                }
                processedCount++
                onProgress(processedCount.toFloat() / totalTasks)
            }
        }

        zipOut.close()
        return zipFile
    }
}
