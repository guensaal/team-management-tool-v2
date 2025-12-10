package com.teamtool.app.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamtool.app.MainActivity
import com.teamtool.app.data.project.Project
import com.teamtool.app.viewmodel.ProjectListViewModel
import com.teamtool.app.viewmodel.UserViewModel
import android.app.Activity
import android.content.ContextWrapper
import android.content.Context


// Füge diese Funktion irgendwo in ProjectListScreen.kt außerhalb des Composables ein
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectListViewModel,
    userViewModel: UserViewModel,
    onNavigateToProjectDetail: (String) -> Unit, // Callback
    onNavigateToUserScreen: () -> Unit,
    onNavigateToCreateProject: () -> Unit
) {
    // 1. ZUSTÄNDE VOM PROJECT-VIEWMODEL
    val projects by viewModel.projects.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = false)

    // HIER wird der Context geholt und für die Activity-Suche verwendet.
    val context = LocalContext.current // <<-- Erste und korrekte Deklaration
    val activity = context.findActivity()

    val projectListErrorMessage by viewModel.errorMessage.collectAsStateWithLifecycle(initialValue = null)

    // 2. ZUSTÄNDE VOM USER-VIEWMODEL
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)
    val userErrorMessage by userViewModel.errorMessage.collectAsStateWithLifecycle(initialValue = null)

    val snackbarHostState = remember { SnackbarHostState() }

    // !!! DIESE ZEILE HAT DEN FEHLER VERURSACHT UND WIRD ENTFERNT !!!
    // val context = LocalContext.current
    // !!! -------------------------------------------------------- !!!


    // Lädt die Projekte, wenn der Screen zum ersten Mal sichtbar wird
    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    // Fehlerbehandlung (Zeigt Snackbar, wenn Fehler im User- oder Projekt-ViewModel auftreten)
    LaunchedEffect(userErrorMessage, projectListErrorMessage) {
        val message = userErrorMessage ?: projectListErrorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Neues Projekt")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Projekte") },
                actions = {
                    // Button zum User Profil
                    IconButton(onClick = onNavigateToUserScreen) {
                        Icon(Icons.Filled.Person, contentDescription = "Mein Profil")
                    }

                    // Button zum Ausloggen
                    IconButton(
                        onClick = {
                            userViewModel.logout(onSuccess = {
                                // 'context' wird korrekt verwendet
                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                context.startActivity(intent)
                            })
                        }
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Abmelden")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Der Haupt-Content-Bereich mit allen Zuständen:
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 1. Laden
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                // 2. Fehler
                projectListErrorMessage != null -> Text(
                    text = "Fehler beim Laden der Projekte: $projectListErrorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )

                // 3. Leerer Zustand (Empty State)
                projects.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Willkommen, ${currentUser?.name ?: "Gast"}!")
                    Text("Noch keine Projekte vorhanden.", modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = onNavigateToCreateProject, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Jetzt Projekt erstellen")
                    }
                    // Auch im Empty State ist ein Backup möglich!
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = {
                        // Sicherer Aufruf der Funktion
                        if (activity is MainActivity) {
                            activity.triggerBackup()
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("⚙️ Backup zu OneDrive starten")
                    }
                }

                // 4. Anzeige der Projekte (SUCCESS)
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // OPTIONALER HEADER MIT BACKUP-BUTTON
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(
                                text = "Hallo ${currentUser?.name ?: "Gast"}, hier sind deine Projekte:",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // *** HIER IST DER BUTTON KORREKT PLATZIERT ***
                            Button(onClick = {
                                if (activity is MainActivity) {
                                    activity.triggerBackup()
                                }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("Backup zu OneDrive starten")
                            }
                            // **********************************************

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Die eigentliche Projektliste
                    items(projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = {
                                // WICHTIG: Übergibt die Projekt-ID an den Callback
                                onNavigateToProjectDetail(project.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectItemCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Anzeige der Mitgliederanzahl (kleines Icon + Zahl)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Mitglieder",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${project.memberIds.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            if (project.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}