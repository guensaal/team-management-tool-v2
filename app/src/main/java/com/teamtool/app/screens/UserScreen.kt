package com.teamtool.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamtool.app.viewmodel.UserViewModel
// Import für FlowRow, welches für die Skills-Chips benötigt wird
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit
) {
    // Zustände aus dem ViewModel laden
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val updateSuccess by viewModel.updateSuccess.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // 1. Lokale Zustände für die Eingabefelder (Als nicht-nullbarer String initialisiert)
    var name by remember { mutableStateOf("") }
    var skillsInput by remember { mutableStateOf("") } // Skills als kommagetrennter String
    var availability by remember { mutableStateOf("") }

    // 2. Initialisierung / Aktualisierung der lokalen Zustände, wenn currentUser geladen/geändert wird
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // SICHERE ZUWEISUNG: Die Zustände werden nur aktualisiert, wenn 'user' nicht null ist.
            // Der Null-Coalescing-Operator (?: "") wird verwendet, falls die Felder im User-Model nullable sind.
            name = user.name ?: ""

            // Skills (List<String>) in einen kommagetrennten String umwandeln
            skillsInput = user.skills.joinToString(", ")

            availability = user.availability ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mein Profil") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Anzeige der E-Mail (nicht änderbar)
            Text(
                text = "E-Mail: ${currentUser?.email ?: "Nicht verfügbar"}",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
            )

            // Name Feld
            OutlinedTextField(
                value = name, // Zeigt den geladenen Wert an
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Verfügbarkeit Feld
            OutlinedTextField(
                value = availability, // Zeigt den geladenen Wert an
                onValueChange = { availability = it },
                label = { Text("Verfügbarkeit") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("z.B. 10h/Woche") }
            )

            // Skills als Chips anzeigen
            Text(
                text = "Aktuelle Skills (Anzeige):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // FlowRow ermöglicht das Umbrechen der Chips in die nächste Zeile
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Skills-String in einzelne Elemente aufteilen und als Chip anzeigen
                skillsInput
                    .split(",")
                    .map { it.trim() } // Leerzeichen entfernen
                    .filter { it.isNotEmpty() } // Leere Strings ignorieren
                    .forEach { skill ->
                        AssistChip(
                            onClick = { /* Interaktiver Button-Effekt */ },
                            label = { Text(skill) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Eingabefeld für Skills (zur Bearbeitung)
            OutlinedTextField(
                value = skillsInput, // Zeigt den aktuellen/geladenen String an
                onValueChange = { skillsInput = it },
                label = { Text("Skills bearbeiten (durch Komma trennen)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("z.B. Kotlin, Java, Design") }
            )

            // Speicher-Button
            Button(
                onClick = {
                    // Skills String wieder in eine Liste umwandeln, bevor er gesendet wird
                    val skillsList = skillsInput
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    viewModel.updateProfile(
                        name = name,
                        skills = skillsList,
                        availability = availability
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Speichern")
                }
            }

            // Anzeige des Update-Status
            if (updateSuccess) {
                Text("Profil erfolgreich aktualisiert! ✅", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}