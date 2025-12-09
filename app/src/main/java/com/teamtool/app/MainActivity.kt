package com.teamtool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teamtool.app.data.member.MemberRepository
import com.teamtool.app.data.project.ProjectRepository
import com.teamtool.app.data.task.TaskRepository
import com.teamtool.app.data.user.UserRepository
import com.teamtool.app.screens.CreateProjectScreen
import com.teamtool.app.screens.EditProjectScreen
import com.teamtool.app.screens.ProjectDetailScreen
import com.teamtool.app.screens.ProjectListScreen
import com.teamtool.app.screens.UserScreen
import com.teamtool.app.screens.auth.LoginScreen
import com.teamtool.app.screens.auth.RegisterScreen
import com.teamtool.app.viewmodel.* class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeamToolApp()
                }
            }
        }
    }
}

@Composable
fun TeamToolApp() {
    // 1. Instanziierung der Repositories (Manuelle Dependency Injection)
    val userRepository = remember { UserRepository() }
    val projectRepository = remember { ProjectRepository() }
    val memberRepository = remember { MemberRepository() }
    val taskRepository = remember { TaskRepository() } // NEU: TaskRepository korrekt instanziiert

    // 2. Instanziierung der Factory
    val factory = remember {
        AppViewModelFactory(
            userRepository = userRepository,
            projectRepository = projectRepository,
            memberRepository = memberRepository,
            taskRepository = taskRepository // WICHTIG: TaskRepository übergeben
        )
    }

    val navController = rememberNavController()

    // 3. UserViewModel für den globalen Login-Status abrufen
    val userViewModel: UserViewModel = viewModel(factory = factory)

    val isUserLoggedIn = userViewModel.isUserLoggedIn()

    // Bestimme die Start-Destination basierend auf dem Login-Status
    val startDestination = remember(isUserLoggedIn) {
        if (isUserLoggedIn) "projectList" else "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // LOGIN
        composable("login") {
            LoginScreen(
                viewModel = userViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("projectList") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        // REGISTER
        composable("register") {
            RegisterScreen(
                viewModel = userViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("projectList") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        // PROJECT_LIST (Liste der Projekte)
        composable("projectList") {
            val projectListViewModel: ProjectListViewModel = viewModel(factory = factory)

            ProjectListScreen(
                viewModel = projectListViewModel,
                userViewModel = userViewModel,
                onNavigateToProjectDetail = { projectId ->
                    navController.navigate("projectDetail/$projectId")
                },
                onNavigateToUserScreen = {
                    navController.navigate("userScreen")
                },
                onNavigateToCreateProject = {
                    navController.navigate("createProject")
                }
            )
        }

        // CREATE_PROJECT (Neues Projekt erstellen)
        composable("createProject") {
            val createProjectViewModel: CreateProjectViewModel = viewModel(factory = factory)

            CreateProjectScreen(
                viewModel = createProjectViewModel,
                onProjectCreated = {
                    navController.popBackStack("projectList", false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // USER_PROFILE (Profil bearbeiten)
        composable("userScreen") {
            UserScreen(
                viewModel = userViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // PROJECT_DETAIL (Projektdetails anzeigen)
        composable(
            route = "projectDetail/{projectId}",
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) {
            val projectDetailViewModel: ProjectDetailViewModel = viewModel(factory = factory)

            ProjectDetailScreen(
                viewModel = projectDetailViewModel,
                userViewModel = userViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToEditProject = { projectId ->
                    navController.navigate("editProject/$projectId")
                }
            )
        }

        // PROJECT_EDIT (Projekt bearbeiten)
        composable(
            route = "editProject/{projectId}",
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) {
            val editProjectViewModel: EditProjectViewModel = viewModel(factory = factory)

            EditProjectScreen(
                viewModel = editProjectViewModel,
                onBack = { navController.popBackStack() },
                onEditSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}