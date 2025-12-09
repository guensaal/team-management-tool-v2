package com.teamtool.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle // WICHTIG: Dieser Import wird nun benötigt
import androidx.lifecycle.viewmodel.CreationExtras
import com.teamtool.app.data.member.MemberRepository
import com.teamtool.app.data.project.ProjectRepository
import com.teamtool.app.data.task.TaskRepository
import com.teamtool.app.data.user.UserRepository
import com.teamtool.app.viewmodel.*

class AppViewModelFactory(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val memberRepository: MemberRepository,
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {

        // KRITISCHER FIX: Wir rufen das SavedStateHandle aus den CreationExtras ab.
        // DIESES Handle enthält die Navigations-Argumente wie "projectId".
        val savedStateHandle = extras.createSavedStateHandle()

        return when {
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(userRepository) as T
            }
            modelClass.isAssignableFrom(ProjectListViewModel::class.java) -> {
                ProjectListViewModel(projectRepository, userRepository) as T
            }
            modelClass.isAssignableFrom(CreateProjectViewModel::class.java) -> {
                CreateProjectViewModel(projectRepository, userRepository) as T
            }
            // NEU: Für die Bearbeitungsseite
            modelClass.isAssignableFrom(EditProjectViewModel::class.java) -> {
                EditProjectViewModel(
                    savedStateHandle,
                    projectRepository
                ) as T
            }
            // Das einzige ViewModel, das SavedStateHandle benötigt
            modelClass.isAssignableFrom(ProjectDetailViewModel::class.java) -> {
                ProjectDetailViewModel(
                    savedStateHandle,
                    projectRepository,
                    memberRepository,
                    userRepository,
                    taskRepository // WICHTIG: TaskRepository hinzugefügt
                ) as T

            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}