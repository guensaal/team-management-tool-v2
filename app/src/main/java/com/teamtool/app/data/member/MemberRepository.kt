package com.teamtool.app.data.member

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MemberRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("members")

    suspend fun addMember(member: Member): Result<Boolean> = try {
        val docRef = collection.document()
        // Speichere das Member-Objekt mit der generierten ID
        docRef.set(member.copy(id = docRef.id)).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMembersForProject(projectId: String): Result<List<Member>> = try {
        val members = collection.whereEqualTo("projectId", projectId).get().await().documents.mapNotNull {
            // FIX: Füge die Firestore-Dokument-ID zum Member-Objekt hinzu
            it.toObject(Member::class.java)?.copy(id = it.id)
        }
        Result.success(members)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateMember(member: Member): Result<Boolean> = try {
        // Nutze die Member-ID (Dokument-ID) zum Aktualisieren
        collection.document(member.id).set(member).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeMember(userId: String, projectId: String): Result<Boolean> = try {
        // Suche das Dokument über userId und projectId, da der Member-ID unbekannt sein könnte
        val docSnapshot = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("projectId", projectId)
            .get().await()

        docSnapshot.documents.forEach { doc ->
            doc.reference.delete().await()
        }
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeAllMembersFromProject(projectId: String): Result<Boolean> = try {
        val docSnapshot = collection.whereEqualTo("projectId", projectId).get().await()
        docSnapshot.documents.forEach { doc ->
            doc.reference.delete().await()
        }
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}