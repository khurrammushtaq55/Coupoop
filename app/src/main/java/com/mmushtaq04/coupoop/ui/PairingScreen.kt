package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.PairingManager

@Composable
fun PairingScreen() {
    val user = AuthManager.currentUser()
    val status = remember { mutableStateOf<String?>(null) }
    val joinCode = remember { mutableStateOf("") }
    val currentInvite = remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        if (user == null) {
            Text("Please sign in first")
            return@Column
        }

        Button(onClick = {
            status.value = "Creating pairing..."
            PairingManager.createPairing(user.uid) { success, invite, message ->
                if (success) {
                    currentInvite.value = invite
                    status.value = "Invite code: $invite — share it with your partner! 🔗"
                } else {
                    status.value = message ?: "Failed to create pairing"
                }
            }
        }) {
            Text("Invite your partner (one-tap)")
        }

        OutlinedTextField(
            value = joinCode.value,
            onValueChange = { joinCode.value = it.trim().uppercase() },
            label = { Text("Enter invite code") },
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(onClick = {
            status.value = "Joining pairing..."
            PairingManager.acceptPairingByCode(joinCode.value, user.uid) { success, message ->
                if (success) {
                    status.value = "Joined pairing!"
                } else {
                    status.value = message ?: "Failed to join"
                }
            }
        }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Join by code")
        }

        currentInvite.value?.let { Text("Your invite code: $it", modifier = Modifier.padding(top = 12.dp)) }
        status.value?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
    }
}
