package com.teamtool.app.screens

import androidx.compose.foundation.layout.Column // FIX: Unresolved reference 'Column'
import androidx.compose.foundation.layout.Spacer // FIX: Unresolved reference 'Spacer'
import androidx.compose.foundation.layout.fillMaxWidth // FIX: Unresolved reference 'fillMaxWidth'
import androidx.compose.foundation.layout.height // FIX: Unresolved reference 'height'
import androidx.compose.material3.*
import androidx.compose.runtime.* // FIX: @Composable invocations, remember, mutableStateOf
import androidx.compose.ui.Modifier // FIX: Unresolved reference 'Modifier'
import androidx.compose.ui.unit.dp // FIX: Unresolved reference 'dp'

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Projekt erstellen") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Projektname") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank() && description.isNotBlank()
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}