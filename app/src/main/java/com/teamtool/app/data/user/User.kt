package com.teamtool.app.data.user

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val skills: List<String> = emptyList(),
    val availability: String? = null
)
