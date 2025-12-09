package com.teamtool.app.screens.task

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.teamtool.app.data.task.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController, projectId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }

    LaunchedEffect(projectId) {
        try {
            val snapshot = db.collection("tasks")
                .whereEqualTo("projectId", projectId)
                .get().await()
            tasks = snapshot.toObjects(Task::class.java)
        } catch (e: Exception) {
            Toast.makeText(context, "Fehler beim Laden: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tasks", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            label = { Text("Task Titel") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newTaskDesc,
            onValueChange = { newTaskDesc = it },
            label = { Text("Beschreibung") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (newTaskTitle.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val newTaskRef = db.collection("tasks").document()
                        val task = Task(
                            id = newTaskRef.id,
                            projectId = projectId,
                            title = newTaskTitle,
                            description = newTaskDesc,
                            status = "ToDo"
                        )
                        newTaskRef.set(task).await()
                        tasks = tasks + task
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Task erstellt!", Toast.LENGTH_SHORT).show()
                            newTaskTitle = ""
                            newTaskDesc = ""
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Task hinzufügen")
        }

        Spacer(Modifier.height(16.dp))

        if (loading) CircularProgressIndicator()
        LazyColumn {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val newStatus = when (task.status) {
                                    "ToDo" -> "Doing"
                                    "Doing" -> "Done"
                                    else -> "ToDo"
                                }
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.collection("tasks").document(task.id)
                                        .update("status", newStatus).await()
                                    tasks = tasks.map {
                                        if (it.id == task.id) it.copy(status = newStatus) else it
                                    }
                                }
                            }) {
                                Text("Status ändern")
                            }
                        }
                    }
                }
            }
        }
    }
}
