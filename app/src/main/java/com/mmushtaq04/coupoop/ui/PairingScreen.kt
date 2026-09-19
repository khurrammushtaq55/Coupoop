package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
fun PairingScreen(onPaired: () -> Unit = {}) {
    val user = AuthManager.currentUser()
    val status = remember { mutableStateOf<String?>(null) }
    val joinCode = remember { mutableStateOf("") }
    val currentInvite = remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {

        if (user == null) {
            Text("Please sign in first")
            return@Column
        }

        Text("Pair up", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Invite your partner or join with their code to start syncing",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

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

        Spacer(modifier = Modifier.height(24.dp))
        Text("— or —", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = joinCode.value,
            onValueChange = { joinCode.value = it.trim().uppercase() },
            label = { Text("Enter invite code") },
            modifier = Modifier.padding(top = 12.dp)
        )

        Button(onClick = {
            status.value = "Joining pairing..."
            PairingManager.acceptPairingByCode(joinCode.value, user.uid) { success, message ->
                if (success) {
                    status.value = "Joined pairing!"
                    onPaired()
                } else {
                    status.value = message ?: "Failed to join"
                }
            }
        }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Join by code")
        }

        currentInvite.value?.let {
            Text("Your invite code: $it", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
            Button(onClick = onPaired, modifier = Modifier.padding(top = 8.dp)) {
                Text("Continue to Sync")
            }
        }
        status.value?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
    }
}
