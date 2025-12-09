package com.teamtool.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamtool.app.data.user.User
import com.teamtool.app.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel // <- NEU
import kotlinx.coroutines.flow.receiveAsFlow // <- NEU
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess
    private val _navigationEvent = Channel<String>()
    val navigationEvent = _navigationEvent.receiveAsFlow() // StateFlow zur Beobachtung

    init {
        if (repository.isUserLoggedIn()) {
            repository.getCurrentUserId()?.let { loadUserProfile(it) }
        }
    }

    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.getUserById(userId).getOrThrow()
                _currentUser.value = user
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.registerUser(User(name = name, email = email), password)
                login(email, password, onSuccess)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = repository.login(email, password).getOrThrow()
                _currentUser.value = user
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // FIX: Korrigierte Logout-Logik – löscht _errorMessage bei Erfolg
    fun logout(onSuccess: () -> Unit) {
        val result = repository.logout()

        if (result.isSuccess) {
            // State wird wie gewohnt bereinigt
            _currentUser.value = null
            _errorMessage.value = null

            // FIX: Sende den Navigationsbefehl als Event über den Channel
            viewModelScope.launch {
                _navigationEvent.send("login")
            }

            onSuccess() // Kann leer bleiben, wird aber gerufen.
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Unbekannter Fehler beim Abmelden."
        }
    }

    fun isUserLoggedIn(): Boolean = repository.isUserLoggedIn()

    fun updateProfile(name: String, skills: List<String>, availability: String?) {
        val currentUserData = _currentUser.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _updateSuccess.value = false

            val updatedUser = currentUserData.copy(
                name = name,
                skills = skills,
                availability = availability
            )

            try {
                repository.updateUser(updatedUser).getOrThrow()
                _currentUser.value = updatedUser
                _updateSuccess.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Profil-Update fehlgeschlagen: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}