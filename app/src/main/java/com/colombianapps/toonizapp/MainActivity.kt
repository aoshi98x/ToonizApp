package com.colombianapps.toonizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colombianapps.toonizapp.data.repository.ProjectRepository
import com.colombianapps.toonizapp.ui.components.CreateProjectDialog
import com.colombianapps.toonizapp.ui.screen.AnimationStudioScreen
import com.colombianapps.toonizapp.ui.screen.ExportScreen
import com.colombianapps.toonizapp.ui.screen.ProjectManagerScreen
import com.colombianapps.toonizapp.ui.screen.SplashScreen
import com.colombianapps.toonizapp.ui.theme.ToonizAppTheme
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel

enum class AppScreen {
    SPLASH,
    PROJECT_MANAGER,
    EDITOR,
    EXPORT
}

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("rust_core")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        setContent {
            val repository = remember { ProjectRepository(this) }
            var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
            var projects by remember { mutableStateOf(repository.getProjects()) }
            var showCreateDialog by remember { mutableStateOf(false) }

            val viewModel: StudioViewModel = viewModel()

            ToonizAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (currentScreen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(onTimeout = {
                                currentScreen = AppScreen.PROJECT_MANAGER
                            })
                        }
                        AppScreen.PROJECT_MANAGER -> {
                            ProjectManagerScreen(
                                projects = projects,
                                repository = repository,
                                onCreateProjectClick = { showCreateDialog = true },
                                onProjectClick = { proj ->
                                    currentScreen = AppScreen.EDITOR
                                    viewModel.loadProject(proj)
                                },
                                onDeleteProject = { id ->
                                    repository.deleteProject(id)
                                    projects = repository.getProjects()
                                },
                                onProjectsUpdated = {
                                    projects = repository.getProjects(forceRefresh = true)
                                }
                            )

                            if (showCreateDialog) {
                                CreateProjectDialog(
                                    onDismiss = { showCreateDialog = false },
                                    onCreate = { newProj ->
                                        repository.saveProject(newProj)
                                        projects = repository.getProjects()
                                        showCreateDialog = false
                                        currentScreen = AppScreen.EDITOR
                                        viewModel.loadProject(newProj)
                                    }
                                )
                            }
                        }
                        AppScreen.EDITOR -> {
                            AnimationStudioScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    viewModel.saveProject {
                                        projects = repository.getProjects()
                                        currentScreen = AppScreen.PROJECT_MANAGER
                                    }
                                },
                                onExportClick = {
                                    currentScreen = AppScreen.EXPORT
                                }
                            )
                        }
                        AppScreen.EXPORT -> {
                            ExportScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    currentScreen = AppScreen.EDITOR
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
