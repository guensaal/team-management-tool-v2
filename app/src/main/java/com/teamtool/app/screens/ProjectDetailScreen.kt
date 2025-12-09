package com.teamtool.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamtool.app.data.user.User
import com.teamtool.app.viewmodel.ProjectDetailViewModel
import com.teamtool.app.viewmodel.ProjectMemberDetails
import com.teamtool.app.viewmodel.UserViewModel
import com.teamtool.app.data.task.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: ProjectDetailViewModel,
    userViewModel: UserViewModel,
    onBack: () -> Unit,
    onNavigateToEditProject: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)

    val project = state.project
    val isCreator = remember(project, currentUser) {
        project?.creatorId == currentUser?.id
    }

    // Zustände für Dialoge
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf<ProjectMemberDetails?>(null) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) } // Für den Bearbeitungsdialog

    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { event ->
            if (event == "projectList") {
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Projekt-Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // VERWENDUNG DER KORRIGIERTEN ICONS
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (isCreator) {
                        IconButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "Mitglied hinzufügen")
                        }
                        IconButton(onClick = {
                            project?.id?.let(onNavigateToEditProject)
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Projekt bearbeiten")
                        }
                        IconButton(onClick = {
                            viewModel.deleteProject()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Projekt löschen")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (project != null) {
                FloatingActionButton(onClick = { showCreateTaskDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Neue Task erstellen")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && project == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null || project == null -> {
                    Text(
                        text = state.errorMessage ?: "Projekt konnte nicht geladen werden.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { ProjectOverview(project.description) }

                        item { TaskHeader(state.tasks.size) }
                        items(state.tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                members = state.members,
                                onTaskClick = { clickedTask -> taskToEdit = clickedTask }
                            )
                        }

                        item { MemberHeader(state.members.size) }
                        items(state.members, key = { it.id }) { member ->
                            MemberCard(
                                member = member,
                                isCreator = isCreator,
                                onRemove = { showRemoveConfirmation = it },
                                projectCreatorId = project.creatorId
                            )
                        }
                    }
                }
            }
        }
    } // ENDE SCAFFOLD

    // -------------------------------------------------------------------------
    // DIALOGE (MÜSSEN AUSSERHALB DES SCAFFOLDS PLATZIERT WERDEN)
    // -------------------------------------------------------------------------

    if (showAddMemberDialog) {
        AddMemberDialog(
            availableUsers = state.availableUsers,
            onDismiss = { showAddMemberDialog = false },
            onAddMember = { userId ->
                viewModel.addMemberToProject(userId)
                showAddMemberDialog = false
            },
            isAdding = state.isAddingMember
        )
    }

    showRemoveConfirmation?.let { member ->
        RemoveMemberConfirmationDialog(
            member = member,
            onConfirm = {
                viewModel.removeMemberFromProject(member.id)
                showRemoveConfirmation = null
            },
            onDismiss = { showRemoveConfirmation = null }
        )
    }

    if (showCreateTaskDialog && project != null) {
        CreateTaskDialog(
            projectMembers = state.members,
            onDismiss = { showCreateTaskDialog = false },
            onCreateTask = { title, description, assignedTo ->
                viewModel.createTask(title, description, assignedTo)
                showCreateTaskDialog = false
            },
            isCreating = state.isCreatingTask
        )
    }

    // Korrekt platzierter Bearbeitungsdialog
    taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            projectMembers = state.members,
            onDismiss = { taskToEdit = null },
            onUpdateTask = { updatedTask ->
                viewModel.updateTask(updatedTask)
                taskToEdit = null
            },
            onDeleteTask = { taskId ->
                viewModel.deleteTask(taskId) // HIER WIRD DIE VM-FUNKTION AUFGERUFEN
                taskToEdit = null
            },
            isUpdating = state.isLoading
        )
    }
} // ENDE ProjectDetailScreen Composable

// ----------------------------------------------------
// HILFS-COMPOSABLES (Alle als Top-Level-Funktionen)
// ----------------------------------------------------

@Composable
fun TaskHeader(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // VERWENDUNG DER KORRIGIERTEN ICONS
        Icon(
            Icons.AutoMirrored.Filled.List,
            contentDescription = "Tasks",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Tasks ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // KORRIGIERTER DIVIDER
}

@Composable
fun TaskCard(task: Task, members: List<ProjectMemberDetails>, onTaskClick: (Task) -> Unit) {
    val assignedMemberName = remember(task.assignedTo, members) {
        members.find { it.id == task.assignedTo }?.name ?: "Nicht zugewiesen"
    }

    val taskColor = when (task.status) {
        "Done" -> MaterialTheme.colorScheme.surfaceVariant
        "InProgress" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = taskColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("Priorität: ${task.priority ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Zugewiesen an: $assignedMemberName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateTaskDialog(
    projectMembers: List<ProjectMemberDetails>,
    onDismiss: () -> Unit,
    onCreateTask: (title: String, description: String, assignedTo: String?) -> Unit,
    isCreating: Boolean
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }
    var selectedPriority by remember { mutableStateOf("Medium") }

    val priorities = listOf("Low", "Medium", "High")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Task erstellen") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (Optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(bottom = 8.dp)
                )

                MemberAssignmentDropdown(
                    members = projectMembers,
                    selectedMemberId = selectedMemberId,
                    onMemberSelected = { selectedMemberId = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PriorityDropdown(
                    selectedPriority = selectedPriority,
                    priorities = priorities,
                    onPrioritySelected = { selectedPriority = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateTask(title, description, selectedMemberId) },
                enabled = title.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Task erstellen")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: Task,
    projectMembers: List<ProjectMemberDetails>,
    onDismiss: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    isUpdating: Boolean
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var selectedMemberId by remember { mutableStateOf(task.assignedTo) }
    var selectedPriority by remember { mutableStateOf(task.priority ?: "Medium") }
    var selectedStatus by remember { mutableStateOf(task.status) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val priorities = listOf("Low", "Medium", "High")
    val statuses = listOf("ToDo", "InProgress", "Done")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task bearbeiten") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    enabled = !isUpdating
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (Optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(bottom = 8.dp),
                    enabled = !isUpdating
                )

                MemberAssignmentDropdown(
                    members = projectMembers,
                    selectedMemberId = selectedMemberId,
                    onMemberSelected = { selectedMemberId = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                StatusDropdown(
                    selectedStatus = selectedStatus,
                    statuses = statuses,
                    onStatusSelected = { selectedStatus = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PriorityDropdown(
                    selectedPriority = selectedPriority,
                    priorities = priorities,
                    onPrioritySelected = { selectedPriority = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedTask = task.copy(
                        title = title,
                        description = description,
                        assignedTo = selectedMemberId,
                        priority = selectedPriority,
                        status = selectedStatus
                    )
                    onUpdateTask(updatedTask)
                },
                enabled = title.isNotBlank() && !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Speichern")
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Löschen-Button (links)
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Task löschen")
                    Spacer(Modifier.width(4.dp))
                    Text("Löschen")
                }

                // Abbrechen-Button (rechts)
                TextButton(onClick = onDismiss, enabled = !isUpdating) {
                    Text("Abbrechen")
                }
            }
        }
    )

    // NEU: Löschbestätigungs-Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Task löschen") },
            text = { Text("Sind Sie sicher, dass Sie diese Task unwiderruflich löschen möchten?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(task.id) // Ruft die VM-Funktion auf
                        showDeleteConfirmation = false
                        onDismiss() // Schließt den Edit Dialog
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isUpdating
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

}

@Composable
fun StatusDropdown(
    selectedStatus: String,
    statuses: List<String>,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Status: $selectedStatus")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MemberAssignmentDropdown(
    members: List<ProjectMemberDetails>,
    selectedMemberId: String?,
    onMemberSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMemberName = members.find { it.id == selectedMemberId }?.name ?: "Nicht zugewiesen"

    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Zuweisung: $selectedMemberName")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Nicht zugewiesen") },
                onClick = {
                    onMemberSelected(null)
                    expanded = false
                }
            )
            members.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.name) },
                    onClick = {
                        onMemberSelected(member.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PriorityDropdown(
    selectedPriority: String,
    priorities: List<String>,
    onPrioritySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Priorität: $selectedPriority")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            priorities.forEach { priority ->
                DropdownMenuItem(
                    text = { Text(priority) },
                    onClick = {
                        onPrioritySelected(priority)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    availableUsers: List<User>,
    onDismiss: () -> Unit,
    onAddMember: (String) -> Unit,
    isAdding: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mitglied hinzufügen") },
        text = {
            Column {
                if (availableUsers.isEmpty()) {
                    Text("Keine weiteren registrierten Benutzer verfügbar.")
                } else {
                    Text("Wählen Sie einen Benutzer aus, der dem Projekt hinzugefügt werden soll:")
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(availableUsers) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAdding) {
                                        onAddMember(user.id)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(user.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    user.skills.take(2).joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            HorizontalDivider() // KORRIGIERTER DIVIDER
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAdding) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}

@Composable
fun RemoveMemberConfirmationDialog(
    member: ProjectMemberDetails,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Mitglied entfernen") },
        text = { Text("Sind Sie sicher, dass Sie ${member.name} aus diesem Projekt entfernen möchten?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Entfernen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ProjectOverview(description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Beschreibung",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description.ifEmpty { "Keine Beschreibung vorhanden." },
            style = MaterialTheme.typography.bodyLarge
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // KORRIGIERTER DIVIDER
}

@Composable
fun MemberHeader(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.People,
            contentDescription = "Mitglieder",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Teammitglieder ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // KORRIGIERTER DIVIDER
}

@Composable
fun MemberCard(
    member: ProjectMemberDetails,
    isCreator: Boolean,
    onRemove: (ProjectMemberDetails) -> Unit,
    projectCreatorId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Rolle: ${member.role}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Verfügbarkeit: ${member.availability ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Skills: ${member.skills.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isCreator && member.id != projectCreatorId) {
                IconButton(onClick = { onRemove(member) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Mitglied entfernen")
                }
            }
        }
    }
}