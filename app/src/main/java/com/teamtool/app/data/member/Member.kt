package com.teamtool.app.data.member

data class Member(
    val id: String = "", // NEU: Muss für Firestore existieren
    val userId: String = "",
    val projectId: String = "",
    var role: String = "Member" // "Admin", "Developer", "QA"
)