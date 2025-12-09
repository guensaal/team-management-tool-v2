package com.teamtool.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamtool.app.data.project.Project
import com.teamtool.app.data.project.ProjectRepository
import com.teamtool.app.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateProjectViewModel(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successEvent = MutableStateFlow(false)
    val successEvent: StateFlow<Boolean> = _successEvent.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun createProject(name: String, description: String) {
        _successEvent.value = false // Event zurücksetzen
        _errorMessage.value = null

        val currentUserId = userRepository.getCurrentUserId()
        if (currentUserId == null) {
            _errorMessage.value = "Benutzer nicht authentifiziert."
            return
        }

        if (name.isBlank()) {
            _errorMessage.value = "Der Projektname darf nicht leer sein."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            // 1. Erstelle das neue Projekt-Objekt
            val newProject = Project(
                name = name.trim(),
                description = description.trim(),
                creatorId = currentUserId,
                memberIds = listOf(currentUserId) // Ersteller ist immer der erste Member
            )

            // 2. Speichere das Projekt über das Repository
            val result = projectRepository.createProject(newProject)

            _isLoading.value = false

            if (result.isSuccess) {
                // Erfolg: Sende das Success-Event, um die Navigation auszulösen
                _successEvent.value = true
            } else {
                // Fehler
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Fehler beim Erstellen des Projekts."
            }
        }
    }

    fun clearSuccessEvent() {
        _successEvent.value = false
    }
}