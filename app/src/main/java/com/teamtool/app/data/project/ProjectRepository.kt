package com.teamtool.app.data.project

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("projects")

    suspend fun createProject(project: Project): Result<Boolean> = try {
        val docRef = collection.document()
        // Hier wird die ID direkt beim Erstellen gesetzt
        collection.document(docRef.id).set(project.copy(id = docRef.id)).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getProjectsForUser(userId: String): Result<List<Project>> = try {
        val projects = collection.whereArrayContains("memberIds", userId).get().await().documents.mapNotNull { doc ->
            // ⚠️ WICHTIG: Stellt sicher, dass die Firestore-Dokument-ID (doc.id)
            // in das 'id'-Feld Ihres Project-Objekts kopiert wird!
            doc.toObject(Project::class.java)?.copy(id = doc.id)
        }
        Result.success(projects)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getProjectById(projectId: String): Result<Project> = try {
        val doc = collection.document(projectId).get().await()
        // ⚠️ AUCH HIER KORRIGIERT: Stellt sicher, dass die ID übernommen wird
        val project = doc.toObject(Project::class.java)?.copy(id = doc.id)
            ?: throw NoSuchElementException("Project with ID $projectId not found")
        Result.success(project)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateProject(updatedProject: Project): Result<Boolean> = try {
        collection.document(updatedProject.id).set(updatedProject).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteProject(projectId: String): Result<Boolean> = try {
        collection.document(projectId).delete().await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addMemberToProject(projectId: String, userId: String): Result<Boolean> = try {
        collection.document(projectId).update("memberIds", FieldValue.arrayUnion(userId)).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeMemberFromProject(projectId: String, userId: String): Result<Boolean> = try {
        collection.document(projectId).update("memberIds", FieldValue.arrayRemove(userId)).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}