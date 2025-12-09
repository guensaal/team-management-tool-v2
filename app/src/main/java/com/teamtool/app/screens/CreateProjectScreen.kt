package com.teamtool.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamtool.app.viewmodel.CreateProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    viewModel: CreateProjectViewModel,
    onProjectCreated: () -> Unit, // Callback für die Navigation
    onBack: () -> Unit
) {
    // Zustände aus dem ViewModel
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val successEvent by viewModel.successEvent.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Lokale Zustände für die Eingabefelder
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    // Reagiere auf den Erfolgs-Event
    LaunchedEffect(successEvent) {
        if (successEvent) {
            onProjectCreated() // Navigiere zurück zur Liste
            viewModel.clearSuccessEvent()
        }
    }

    // Zeige Toast/Snackbar bei Fehler
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Neues Projekt erstellen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Projektname Eingabe
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Projektname *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Beschreibung Eingabe
            OutlinedTextField(
                value = projectDescription,
                onValueChange = { projectDescription = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                minLines = 3,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Speichern-Button
            Button(
                onClick = {
                    viewModel.createProject(projectName, projectDescription)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = projectName.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Projekt erstellen")
                }
            }
        }
    }
}