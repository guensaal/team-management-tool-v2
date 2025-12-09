package com.teamtool.app.screens.project

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.teamtool.app.data.member.Member
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun ProjectMemberRoleItem(member: Member, onRoleChanged: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(member.role) }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(member.userId)
        Box {
            Button(onClick = { expanded = true }) { Text(role) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Admin", "Developer", "QA").forEach { r ->
                    DropdownMenuItem(text = { Text(r) }, onClick = {
                        expanded = false
                        scope.launch {
                            try {
                                db.collection("members").document("${member.userId}_${member.projectId}").update("role", r).await()
                                role = r
                                onRoleChanged()
                                Toast.makeText(context, "Rolle geändert", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }
        }
    }
}
