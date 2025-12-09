package com.teamtool.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamtool.app.data.project.Project
import com.teamtool.app.data.project.ProjectRepository
import com.teamtool.app.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository // Wird benötigt, um die ID des aktuellen Benutzers abzurufen
) : ViewModel() {

    // 1. ZUSTAND: Liste der Projekte
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    // 2. ZUSTAND: Ladeindikator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 3. ZUSTAND: Fehlermeldungen
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Funktion zum Laden der Projekte
    fun loadProjects() {
        // Sicherstellen, dass der Benutzer eingeloggt ist
        val currentUserId = userRepository.getCurrentUserId() ?: run {
            _errorMessage.value = "Benutzer nicht eingeloggt. Projekte können nicht geladen werden."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _projects.value = emptyList() // Alten Zustand löschen

            val result = projectRepository.getProjectsForUser(currentUserId)

            result.fold(
                onSuccess = { projectList ->
                    _projects.value = projectList
                },
                onFailure = { exception ->
                    // Die Fehlermeldung wird an den Screen gesendet
                    _errorMessage.value = "Laden der Projekte fehlgeschlagen: ${exception.message}"
                }
            )

            _isLoading.value = false
        }
    }
}