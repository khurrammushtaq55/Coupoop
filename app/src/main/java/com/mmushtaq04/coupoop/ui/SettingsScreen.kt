package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmushtaq04.coupoop.AccountManager
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.NotificationPrefsManager
import com.mmushtaq04.coupoop.PairingManager

@Composable
fun SettingsScreen(
    pairingId: String?,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onLeftPairing: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    var status by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }

    LaunchedEffect(pairingId) {
        val user = AuthManager.currentUser()
        if (user != null && pairingId != null) {
            NotificationPrefsManager.isPairingMuted(user.uid, pairingId) { isMuted ->
                muted = isMuted
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Button(onClick = onBack) {
            Text("← Back")
        }

        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))

        Button(onClick = onSignOut, modifier = Modifier.padding(top = 16.dp)) {
            Text("Sign out")
        }

        if (pairingId != null) {
            Button(
                onClick = {
                    val user = AuthManager.currentUser() ?: return@Button
                    val newValue = !muted
                    NotificationPrefsManager.setPairingMuted(user.uid, pairingId, newValue) { success, message ->
                        if (success) {
                            muted = newValue
                        } else {
                            status = message ?: "Failed to update notification setting"
                        }
                    }
                },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(if (muted) "Unmute notifications" else "Mute notifications")
            }

            Button(
                onClick = {
                    val user = AuthManager.currentUser() ?: return@Button
                    working = true
                    status = "Leaving pairing..."
                    PairingManager.leavePairing(pairingId, user.uid) { success, message ->
                        working = false
                        if (success) {
                            onLeftPairing()
                        } else {
                            status = message ?: "Failed to leave pairing"
                        }
                    }
                },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Leave pairing")
            }
        }

        Button(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Delete my account")
        }

        status?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!working) showDeleteConfirm = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and removes you from your pairing. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    working = true
                    status = "Deleting account..."
                    AccountManager.deleteAccount { success, message ->
                        working = false
                        showDeleteConfirm = false
                        if (success) {
                            onAccountDeleted()
                        } else {
                            status = message ?: "Failed to delete account"
                        }
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
