package com.teamtool.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamtool.app.data.project.Project // Stellt sicher, dass das Project-Objekt importiert wird
import com.teamtool.app.data.project.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

// Zustand (State) für die Bearbeiten-Ansicht
data class EditProjectState(
    // Speichert das Basisprojekt, um creatorId und memberIds beizubehalten
    val baseProject: Project? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class EditProjectViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val projectId: String? = savedStateHandle.get<String>("projectId")

    private val _state = MutableStateFlow(EditProjectState())
    val state: StateFlow<EditProjectState> = _state.asStateFlow()

    // Channel für die Navigation (nach erfolgreichem Speichern)
    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        // Prüft, ob eine ID aus der Navigation übergeben wurde
        if (!projectId.isNullOrBlank()) {
            loadProject(projectId)
        } else {
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = "Fehler: Projekt-ID für Bearbeitung fehlt.",
                baseProject = null
            )
        }
    }

    private fun loadProject(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val result = projectRepository.getProjectById(id)

            result.onSuccess { project ->
                // Projekt in den State laden und die Felder vorbefüllen
                _state.value = _state.value.copy(
                    baseProject = project,
                    projectName = project.name,
                    projectDescription = project.description,
                    isLoading = false
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Laden fehlgeschlagen: ${it.message}"
                )
            }
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(projectName = name, successMessage = null)
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(projectDescription = description, successMessage = null)
    }

    fun saveProject() {
        val currentState = _state.value

        if (currentState.baseProject == null || currentState.projectName.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Projektinformationen unvollständig.", successMessage = null)
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(isSaving = true, errorMessage = null, successMessage = null)

            // Das vollständige Projektobjekt aktualisieren, um creatorId und memberIds beizubehalten
            val projectToUpdate = currentState.baseProject.copy(
                name = currentState.projectName,
                description = currentState.projectDescription
            )

            val result = projectRepository.updateProject(projectToUpdate)

            result.onSuccess {
                _state.value = currentState.copy(
                    isSaving = false,
                    successMessage = "Projekt erfolgreich aktualisiert!",
                    baseProject = projectToUpdate // State mit neuem Projektobjekt aktualisieren
                )
                // Signal an den Screen senden, dass er zurücknavigieren kann.
                _navigationEvent.send("editSuccess")
            }.onFailure { e ->
                _state.value = currentState.copy(
                    isSaving = false,
                    errorMessage = "Speichern fehlgeschlagen: ${e.message}"
                )
            }
        }
    }
}