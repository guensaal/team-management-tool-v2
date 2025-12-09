package com.teamtool.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamtool.app.data.user.User

@Composable
fun ProjectAddMemberDialog(
    availableUsers: List<User>,
    onDismiss: () -> Unit,
    onMemberAdded: (userId: String, role: String) -> Unit
) {
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedRole by remember { mutableStateOf("Developer") }
    val roles = listOf("Developer", "QA", "Admin")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mitglied hinzufügen") },
        text = {
            Column {
                Text("Verfügbare Benutzer:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(Modifier.heightIn(max = 200.dp)) {
                    items(availableUsers) { user ->
                        val isSelected = user == selectedUser
                        ListItem(
                            headlineContent = { Text(user.name) },
                            supportingContent = { Text(user.email) },
                            modifier = Modifier.clickable { selectedUser = user },
                            colors = if (isSelected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors()
                        )
                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Rolle zuweisen:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roles.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedUser?.let { user ->
                        onMemberAdded(user.id, selectedRole)
                        onDismiss()
                    }
                },
                enabled = selectedUser != null
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}