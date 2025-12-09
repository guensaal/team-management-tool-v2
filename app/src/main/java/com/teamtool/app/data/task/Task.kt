package com.teamtool.app.data.task

data class Task(
    val id: String = "",
    val projectId: String = "",
    var title: String = "",
    var description: String = "",
    var assignedTo: String? = null,
    var status: String = "ToDo",
    var priority: String? = null
)
