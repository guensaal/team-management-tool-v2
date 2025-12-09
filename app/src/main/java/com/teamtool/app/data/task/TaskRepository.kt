package com.teamtool.app.data.task

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.teamtool.app.data.task.Task // Import des Task-Datenmodells

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("tasks")

    /**
     * Erstellt eine neue Task in Firestore.
     * Generiert eine Firestore-ID und weist sie der Task zu.
     */
    suspend fun createTask(task: Task): Result<String> = try {
        val docRef = collection.document()
        val taskWithId = task.copy(id = docRef.id)
        docRef.set(taskWithId).await()
        Result.success(docRef.id) // Gibt die ID der neu erstellten Task zurück
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Ruft alle Tasks für ein bestimmtes Projekt ab.
     */
    suspend fun getTasksForProject(projectId: String): Result<List<Task>> = try {
        val tasks = collection
            .whereEqualTo("projectId", projectId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Task::class.java) }

        Result.success(tasks)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Ruft eine einzelne Task anhand ihrer ID ab.
     */
    suspend fun getTaskById(taskId: String): Result<Task> = try {
        val task = collection.document(taskId).get().await().toObject(Task::class.java)
            ?: throw NoSuchElementException("Task with ID $taskId not found")
        Result.success(task)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Aktualisiert eine bestehende Task.
     */
    suspend fun updateTask(updatedTask: Task): Result<Boolean> = try {
        // Die Task-ID wird als Dokument-ID verwendet
        collection.document(updatedTask.id).set(updatedTask).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Löscht eine Task anhand ihrer ID.
     */
    suspend fun deleteTask(taskId: String): Result<Boolean> = try {
        collection.document(taskId).delete().await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}