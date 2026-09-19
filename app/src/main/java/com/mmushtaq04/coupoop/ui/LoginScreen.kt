package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.FcmManager

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val status = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💩", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sync", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "The playful way to keep track together",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            status.value = "Signing in..."
            AuthManager.signInAnonymously { success, message ->
                if (success) {
                    // Register FCM token for this user so server functions can send pushes
                    FcmManager.registerTokenForCurrentUser { regOk, regMsg ->
                        status.value = if (!regOk) {
                            "Signed in (FCM reg failed: ${regMsg ?: "unknown"})"
                        } else {
                            "You're in — ready to sync! 🎉"
                        }
                        onSignedIn()
                    }
                } else {
                    status.value = message ?: "Sign in failed — try again"
                }
            }
        }) {
            Text("Quick anonymous sign-in — start syncing 💨")
        }

        Spacer(modifier = Modifier.height(12.dp))
        status.value?.let { Text(it, textAlign = TextAlign.Center) }
    }
}
