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
