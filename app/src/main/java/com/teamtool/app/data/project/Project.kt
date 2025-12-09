package com.teamtool.app.data.project

data class Project(
    val id: String = "",
    var name: String = "",
    var description: String = "",
    val creatorId: String = "",
    val memberIds: List<String> = emptyList()
)
