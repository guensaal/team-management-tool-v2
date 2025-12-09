package com.teamtool.app.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun registerUser(user: User, password: String) {
        val result = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = result.user?.uid ?: throw Exception("User creation failed")
        db.collection("users").document(uid).set(user.copy(id = uid)).await()
    }

    // FIX: login gibt nun Result<User> zurück
    suspend fun login(email: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Login failed")
        val user = db.collection("users").document(uid).get().await().toObject(User::class.java)
            ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // FIX: logout gibt Result<Boolean> zurück (bereits korrekt, hier zur Vollständigkeit)
    fun logout(): Result<Boolean> = try {
        auth.signOut()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // FIX: updateUser gibt nun Result<Boolean> zurück
    suspend fun updateUser(user: User): Result<Boolean> = try {
        db.collection("users").document(user.id).set(user).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // FIX: getUserById gibt nun Result<User> zurück
    suspend fun getUserById(userId: String): Result<User> = try {
        val user = db.collection("users").document(userId).get().await().toObject(User::class.java)
            ?: throw NoSuchElementException("User with ID $userId not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // FIX: getAllUsers gibt nun Result<List<User>> zurück
    suspend fun getAllUsers(): Result<List<User>> = try {
        val users = db.collection("users").get().await().documents.mapNotNull {
            it.toObject(User::class.java)
        }
        Result.success(users)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid
}