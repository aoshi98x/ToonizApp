package com.colombianapps.toonizapp.data.model

data class AnimationProject(
    val id: String,
    val name: String,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val fps: Int,
    val dpi: Int,
    val lastModified: Long = System.currentTimeMillis()
)
