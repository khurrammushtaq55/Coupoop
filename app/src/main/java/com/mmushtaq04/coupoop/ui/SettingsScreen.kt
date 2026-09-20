package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mmushtaq04.coupoop.AccountManager
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.NotificationPrefsManager
import com.mmushtaq04.coupoop.PairingManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.Danger
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark

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
        .padding(20.dp)) {
        
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Back", style = MaterialTheme.typography.labelLarge)
        }

        Text(
            "Settings", 
            style = MaterialTheme.typography.headlineMedium, 
            color = LightCoralDark,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
        ) {
            Text("Sign out", style = MaterialTheme.typography.labelLarge)
        }

        if (pairingId != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val user = AuthManager.currentUser() ?: return@OutlinedButton
                    val newValue = !muted
                    NotificationPrefsManager.setPairingMuted(user.uid, pairingId, newValue) { success, message ->
                        if (success) {
                            muted = newValue
                        } else {
                            status = message ?: "Failed to update notification setting"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Text(if (muted) "Unmute notifications" else "Mute notifications", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "Applies to this pairing only",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val user = AuthManager.currentUser() ?: return@OutlinedButton
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
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Text("Leave pairing", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Danger)
        ) {
            Text("Delete my account", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }

        status?.let { 
            Text(
                it, 
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            ) 
        }
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
                    Text("Delete", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    CoupoopTheme {
        SettingsScreen(
            pairingId = "pair123",
            onBack = {},
            onSignOut = {},
            onLeftPairing = {},
            onAccountDeleted = {}
        )
    }
}
