package com.teamtool.app

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.teamtool.app.viewmodel.*
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Compose-Inhalt festlegen
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

        val crashButton = Button(this).apply {
            text = "Test Crash"
            setOnClickListener {
                throw RuntimeException("Test Crash")
            }
        }

        // Layout-Parameter für Platzierung unten
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM
        addContentView(crashButton, params)
    }

    // --- HIER BEGINNEN DIE ÖFFENTLICHEN BACKUP-FUNKTIONEN ---

    /**
     * Öffentliche Funktion, die vom Composable (z.B. ProjectListScreen) aufgerufen wird.
     */
    fun triggerBackup() {
        performBackupToOneDrive()
    }

    /**
     * Holt Daten aus Firestore, baut sie zu einem JSON-String und speichert sie lokal.
     */
    fun performBackupToOneDrive() {
        val db = FirebaseFirestore.getInstance()

        // 1. Daten holen (Beispiel: Alle Projects holen)
        db.collection("projects").get()
            .addOnSuccessListener { result ->
                // Wir bauen einen einfachen String, der wie JSON aussieht
                val stringBuilder = StringBuilder()
                stringBuilder.append("[\n")

                for (document in result) {
                    stringBuilder.append("  ${document.data},\n")
                }
                stringBuilder.append("]")

                val backupData = stringBuilder.toString()

                // 2. Datei lokal auf dem Handy erstellen
                try {
                    val fileName = "backup_teamtool_${System.currentTimeMillis()}.json"
                    val file = File(this.filesDir, fileName)
                    file.writeText(backupData)

                    // 3. Die Datei an OneDrive "teilen"
                    shareFile(file)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .addOnFailureListener { exception ->
                println("Fehler beim Backup: $exception")
            }
    }

    /**
     * Startet den Android Share Intent, um die Datei an OneDrive zu übergeben.
     */
    fun shareFile(file: File) {
        // Damit andere Apps (OneDrive) auf die Datei zugreifen dürfen
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider", // Muss in AndroidManifest.xml definiert sein
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Öffnet den Dialog, wo du OneDrive auswählen kannst
        startActivity(Intent.createChooser(intent, "Backup speichern in..."))
    }
}

// ------------------------------------------------------------------------------------------------
// Der Composable-Block bleibt unverändert.
@Composable
fun TeamToolApp() {
    // 1. Instanziierung der Repositories (Manuelle Dependency Injection)
    val userRepository = remember { UserRepository() }
    val projectRepository = remember { ProjectRepository() }
    val memberRepository = remember { MemberRepository() }
    val taskRepository = remember { TaskRepository() }

    // 2. Instanziierung der Factory
    val factory = remember {
        AppViewModelFactory(
            userRepository = userRepository,
            projectRepository = projectRepository,
            memberRepository = memberRepository,
            taskRepository = taskRepository
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
        // ... (Der gesamte NavHost-Code bleibt unverändert)
        composable("login") {
            LoginScreen(
                viewModel = userViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("projectList") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = userViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("projectList") { popUpTo("login") { inclusive = true } }
                }
            )
        }
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
        composable("userScreen") {
            UserScreen(
                viewModel = userViewModel,
                onBack = { navController.popBackStack() }
            )
        }
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