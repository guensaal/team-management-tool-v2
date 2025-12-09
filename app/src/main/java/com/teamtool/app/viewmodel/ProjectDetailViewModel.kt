package com.teamtool.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamtool.app.data.member.Member
import com.teamtool.app.data.member.MemberRepository
import com.teamtool.app.data.project.Project
import com.teamtool.app.data.project.ProjectRepository
import com.teamtool.app.data.user.User
import com.teamtool.app.data.user.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.channels.Channel // NEU
import kotlinx.coroutines.flow.receiveAsFlow // NEU
import com.teamtool.app.data.task.Task
import com.teamtool.app.data.task.TaskRepository

// Hilfs-Modelle
data class ProjectMemberDetails(
    val id: String,
    val name: String,
    val role: String,
    val skills: List<String>,
    val availability: String?
)

data class ProjectDetailState(
    val project: Project? = null,
    val members: List<ProjectMemberDetails> = emptyList(),
    val availableUsers: List<User> = emptyList(), // HIER: Alle registrierten User
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val isAddingMember: Boolean = false, // NEU: Für den Ladezustand beim Hinzufügen
    val isCreatingTask: Boolean = false,
    val errorMessage: String? = null
)

class ProjectDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val projectId: String? = savedStateHandle.get<String>("projectId")

    private val _state = MutableStateFlow(ProjectDetailState())
    val state: StateFlow<ProjectDetailState> = _state.asStateFlow()

    // FIX: Definiert den Navigations-Channel für Events (z.B. nach erfolgreichem Löschen)
    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()


    init {
        if (!projectId.isNullOrBlank()) {
            loadProjectDetails()
        } else {
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = "Fehler: keine Projekt-ID übergeben"
            )
        }
    }

    fun loadProjectDetails() {
        if (projectId == null) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, availableUsers = emptyList(), tasks = emptyList())

            supervisorScope {
                // 1. Projekt laden
                val projectResult = projectRepository.getProjectById(projectId)
                val project = projectResult.getOrElse {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Projekt nicht gefunden: ${it.message}"
                    )
                    return@supervisorScope
                }

                // 2. Mitglieder laden
                val membersResult = memberRepository.getMembersForProject(projectId)
                val members = membersResult.getOrElse { emptyList() }

                // NEU: 3. Alle registrierten Benutzer laden
                val allUsersResult = userRepository.getAllUsers()
                val allUsers = allUsersResult.getOrElse { emptyList() }

                // NEU: 3. Alle Tasks für das Projekt laden
                val tasksResult = async { taskRepository.getTasksForProject(projectId) }
                val tasks = tasksResult.await().getOrElse { emptyList() }

                // NEU: 4. Nicht-Projektmitglieder identifizieren
                val memberIds = project.memberIds.toSet()
                val availableUsers = allUsers.filter { user -> !memberIds.contains(user.id) }

                // 3. User-Daten laden
                val roleMap = members.associateBy { it.userId }.mapValues { it.value.role }

                val memberDetails = project.memberIds.mapNotNull { uid ->
                    val userResult = async { userRepository.getUserById(uid) }
                    userResult.await().getOrNull()?.let { user ->
                        ProjectMemberDetails(
                            id = user.id,
                            name = user.name,
                            role = roleMap[user.id] ?: "Member",
                            skills = user.skills,
                            availability = user.availability
                        )
                    }
                }

                _state.value = ProjectDetailState(
                    project = project,
                    members = memberDetails,
                    availableUsers = availableUsers,
                    tasks = tasks,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Löst eine vollständige Löschung des Projekts aus und navigiert dann zurück.
     */
    fun deleteProject() {
        val id = projectId
        if (id.isNullOrBlank()) {
            _state.value = _state.value.copy(errorMessage = "Löschfehler: Projekt-ID fehlt.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val deleteResult = executeFullProjectDeletion(id)

            if (deleteResult.isSuccess) {
                // FIX: Sende das Navigations-Event an den Screen
                _navigationEvent.send("projectList")
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = deleteResult.exceptionOrNull()?.message ?: "Unbekannter Löschfehler."
                )
            }
        }
    }

    /**
     * Führt eine vollständige Löschung des Projekts durch (Projekt + alle Member-Einträge).
     */
    private suspend fun executeFullProjectDeletion(projectId: String): Result<Boolean> {
        // 1. Alle Member aus der members-Collection löschen (MemberRepository verwenden)
        val memberDeleteResult = memberRepository.removeAllMembersFromProject(projectId)

        if (memberDeleteResult.isFailure) {
            return Result.failure(memberDeleteResult.exceptionOrNull() ?: Exception("Fehler beim Löschen der Projektmitglieder."))
        }

        // 2. Das Projekt-Dokument löschen
        val projectDeleteResult = projectRepository.deleteProject(projectId)

        return projectDeleteResult
    }


    fun updateProject(updatedName: String, updatedDescription: String) {
        val currentProject = _state.value.project ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val projectToUpdate = currentProject.copy(
                name = updatedName,
                description = updatedDescription,
                id = currentProject.id
            )

            val updateResult = projectRepository.updateProject(projectToUpdate)

            if (updateResult.isSuccess) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    project = projectToUpdate
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = updateResult.exceptionOrNull()?.message ?: "Unbekannter Update-Fehler."
                )
            }
        }
    }


    /**
     * Fügt einen Benutzer als Mitglied zum Projekt hinzu.
     */
    fun addMemberToProject(userId: String) {
        val currentProject = _state.value.project
        if (currentProject == null || projectId.isNullOrBlank()) {
            _state.value = _state.value.copy(errorMessage = "Projekt nicht verfügbar.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingMember = true, errorMessage = null)

            // 1. Member-Eintrag in der 'members'-Collection erstellen
            val memberResult = memberRepository.addMember(
                Member(
                    userId = userId,
                    projectId = projectId,
                    role = "Member" // Standardrolle
                )
            )

            // 2. userId zur 'memberIds'-Liste des Projektdokuments hinzufügen
            val projectUpdateResult = projectRepository.addMemberToProject(projectId, userId)

            if (memberResult.isSuccess && projectUpdateResult.isSuccess) {
                // Bei Erfolg: Projekt-Details neu laden, um die UI zu aktualisieren
                loadProjectDetails()
                // Setzt isAddingMember zurück, wird aber durch loadProjectDetails() überschrieben.
                // Trotzdem als Rückversicherung:
                _state.value = _state.value.copy(isAddingMember = false)

            } else {
                _state.value = _state.value.copy(
                    isAddingMember = false,
                    errorMessage = "Mitglied konnte nicht hinzugefügt werden: ${memberResult.exceptionOrNull()?.message ?: projectUpdateResult.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Entfernt einen Benutzer als Mitglied vom Projekt.
     * Muss sowohl den Member-Eintrag als auch die memberIds-Liste des Projekts aktualisieren.
     */
    fun removeMemberFromProject(userId: String) {
        val currentProject = _state.value.project
        if (currentProject == null || projectId.isNullOrBlank()) {
            _state.value = _state.value.copy(errorMessage = "Projekt nicht verfügbar.")
            return
        }

        // Der Ersteller kann sich nicht selbst entfernen (das wäre Projektlöschung)
        if (userId == currentProject.creatorId) {
            _state.value = _state.value.copy(errorMessage = "Der Ersteller kann sich nicht selbst aus dem Projekt entfernen.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null) // Setze isLoading, um UI zu blockieren

            // 1. Member-Eintrag aus der 'members'-Collection löschen
            val memberResult = memberRepository.removeMember(
                userId = userId,
                projectId = projectId
            )

            // 2. userId aus der 'memberIds'-Liste des Projektdokuments entfernen
            val projectUpdateResult = projectRepository.removeMemberFromProject(projectId, userId)

            if (memberResult.isSuccess && projectUpdateResult.isSuccess) {
                // Bei Erfolg: Projekt-Details neu laden, um die UI zu aktualisieren
                loadProjectDetails()

            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Mitglied konnte nicht entfernt werden: ${memberResult.exceptionOrNull()?.message ?: projectUpdateResult.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Erstellt eine neue Task für das aktuelle Projekt.
     */
    fun createTask(title: String, description: String, assignedToUserId: String?) {
        val currentProject = _state.value.project
        if (currentProject == null || projectId.isNullOrBlank()) {
            _state.value = _state.value.copy(errorMessage = "Projekt nicht verfügbar.")
            return
        }
        if (title.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Titel darf nicht leer sein.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isCreatingTask = true, errorMessage = null)

            val newTask = Task(
                projectId = projectId,
                title = title,
                description = description,
                assignedTo = assignedToUserId,
                status = "ToDo", // Standardstatus
                priority = "Medium" // Standardpriorität
            )

            val result = taskRepository.createTask(newTask)

            _state.value = _state.value.copy(isCreatingTask = false)

            result.onSuccess {
                // Bei Erfolg: Projektdetails neu laden, um die neue Task anzuzeigen
                loadProjectDetails()
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    errorMessage = "Task konnte nicht erstellt werden: ${e.message}"
                )
            }
        }
    }


    fun updateTask(updatedTask: Task) {
        val currentProject = _state.value.project
        if (currentProject == null || projectId.isNullOrBlank()) {
            _state.value = _state.value.copy(errorMessage = "Projekt nicht verfügbar.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null) // Setze isLoading, um UI zu blockieren

            val result = taskRepository.updateTask(updatedTask)

            if (result.isSuccess) {
                // Bei Erfolg: Projektdetails neu laden, um die UI zu aktualisieren
                loadProjectDetails()

            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Task konnte nicht aktualisiert werden: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }


    fun updateTaskStatus(task: Task, newStatus: String) {
        val updatedTask = task.copy(status = newStatus)
        updateTask(updatedTask)
    }

    // ProjectDetailViewModel.kt (Am Ende der Klasse einfügen)

    /**
     * Löscht eine Task anhand ihrer ID.
     */
    fun deleteTask(taskId: String) {
        if (taskId.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Ungültige Task ID.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val result = taskRepository.deleteTask(taskId)

            if (result.isSuccess) {
                // Bei Erfolg: Tasks neu laden
                loadProjectDetails()
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Task konnte nicht gelöscht werden: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

}