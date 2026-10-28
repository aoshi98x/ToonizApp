package com.colombianapps.toonizapp.data.repository

import android.content.Context
import com.colombianapps.toonizapp.data.model.AnimationProject
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectRepository(private val context: Context) {
    private val file = File(context.filesDir, "tooniz_projects.json")
    private var cachedProjects: List<AnimationProject>? = null

    fun getProjects(forceRefresh: Boolean = false): List<AnimationProject> {
        if (!forceRefresh && cachedProjects != null) {
            return cachedProjects!!
        }
        if (!file.exists()) {
            cachedProjects = emptyList()
            return emptyList()
        }
        try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<AnimationProject>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AnimationProject(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        canvasWidth = obj.getInt("canvasWidth"),
                        canvasHeight = obj.getInt("canvasHeight"),
                        fps = obj.getInt("fps"),
                        dpi = obj.getInt("dpi"),
                        lastModified = obj.getLong("lastModified")
                    )
                )
            }
            val sorted = list.sortedByDescending { it.lastModified }
            cachedProjects = sorted
            return sorted
        } catch (e: Exception) {
            cachedProjects = emptyList()
            return emptyList()
        }
    }

    fun saveProject(project: AnimationProject) {
        val projects = getProjects().toMutableList()
        val index = projects.indexOfFirst { it.id == project.id }
        val updated = project.copy(lastModified = System.currentTimeMillis())
        if (index >= 0) {
            projects[index] = updated
        } else {
            projects.add(0, updated)
        }
        cachedProjects = projects.sortedByDescending { it.lastModified }
        writeProjects(cachedProjects!!)
    }

    fun deleteProject(id: String) {
        val projects = getProjects().filter { it.id != id }
        cachedProjects = projects
        writeProjects(projects)

        val projectDir = File(context.filesDir, "project_drawings_$id")
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
    }

    fun exportProjectToPackage(project: AnimationProject): File {
        val packageDir = File(context.cacheDir, "tooniz_packages").apply { mkdirs() }
        val cleanName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val packageFile = File(packageDir, "${cleanName}_${project.id}.tooniz")
        if (packageFile.exists()) packageFile.delete()

        val zipOut = java.util.zip.ZipOutputStream(java.io.FileOutputStream(packageFile))

        // 1. Write project.json
        val projJson = JSONObject().apply {
            put("id", project.id)
            put("name", project.name)
            put("canvasWidth", project.canvasWidth)
            put("canvasHeight", project.canvasHeight)
            put("fps", project.fps)
            put("dpi", project.dpi)
            put("lastModified", project.lastModified)
        }.toString()

        zipOut.putNextEntry(java.util.zip.ZipEntry("project.json"))
        zipOut.write(projJson.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // 2. Write drawings directory if exists
        val projectDir = File(context.filesDir, "project_drawings_${project.id}")
        if (projectDir.exists() && projectDir.isDirectory) {
            projectDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    zipOut.putNextEntry(java.util.zip.ZipEntry("drawings/${file.name}"))
                    file.inputStream().use { input -> input.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }

        zipOut.close()
        return packageFile
    }

    fun importProjectFromPackage(packageFile: File): AnimationProject? {
        if (!packageFile.exists()) return null
        var project: AnimationProject? = null

        // Pass 1: find project.json first to know the target directory
        java.util.zip.ZipInputStream(java.io.FileInputStream(packageFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == "project.json") {
                    val jsonString = zipIn.readBytes().toString(Charsets.UTF_8)
                    val obj = JSONObject(jsonString)
                    project = AnimationProject(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        canvasWidth = obj.getInt("canvasWidth"),
                        canvasHeight = obj.getInt("canvasHeight"),
                        fps = obj.getInt("fps"),
                        dpi = obj.getInt("dpi"),
                        lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                    )
                    break
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        val proj = project ?: return null
        val targetDir = File(context.filesDir, "project_drawings_${proj.id}").apply { mkdirs() }

        // Pass 2: extract drawings into target directory
        java.util.zip.ZipInputStream(java.io.FileInputStream(packageFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("drawings/") && !entry.isDirectory) {
                    val fileName = entry.name.removePrefix("drawings/")
                    if (fileName.isNotBlank()) {
                        val outFile = File(targetDir, fileName)
                        java.io.FileOutputStream(outFile).use { out ->
                            zipIn.copyTo(out)
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        saveProject(proj)
        return proj
    }

    private fun writeProjects(projects: List<AnimationProject>) {
        try {
            val jsonArray = JSONArray()
            for (p in projects) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("canvasWidth", p.canvasWidth)
                    put("canvasHeight", p.canvasHeight)
                    put("fps", p.fps)
                    put("dpi", p.dpi)
                    put("lastModified", p.lastModified)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
