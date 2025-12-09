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
import com.teamtool.app.viewmodel.EditProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectScreen(
    viewModel: EditProjectViewModel,
    onBack: () -> Unit,
    onEditSuccess: () -> Unit // Callback, um nach Erfolg zur Detailseite zurückzukehren
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigations-Event nach SPEICHERN abfangen
    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { event ->
            if (event == "editSuccess") {
                onEditSuccess()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projekt bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Abbrechen")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.baseProject != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Projektname
                        OutlinedTextField(
                            value = state.projectName,
                            onValueChange = viewModel::updateName,
                            label = { Text("Projektname") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        // 2. Beschreibung
                        OutlinedTextField(
                            value = state.projectDescription,
                            onValueChange = viewModel::updateDescription,
                            label = { Text("Beschreibung (Optional)") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(bottom = 16.dp),
                            singleLine = false
                        )

                        // 3. Speichern/Aktualisieren Button
                        Button(
                            onClick = viewModel::saveProject,
                            enabled = state.projectName.isNotBlank() && !state.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Änderungen speichern")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Statusanzeige
                        state.errorMessage?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }
                        state.successMessage?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                else -> {
                    Text(
                        text = state.errorMessage ?: "Projekt konnte nicht geladen werden.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }
        }
    }
}