package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.FcmManager

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val status = remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Button(onClick = {
            status.value = "Signing in..."
            AuthManager.signInAnonymously { success, message ->
                if (success) {
                    status.value = "You're in — ready to sync! 🎉"
                    // Register FCM token for this user so server functions can send pushes
                    FcmManager.registerTokenForCurrentUser { regOk, regMsg ->
                        if (!regOk) {
                            // non-fatal, just surface status
                            status.value = "Signed in (FCM reg failed: ${regMsg ?: "unknown"})"
                        } else {
                            status.value = "You're in — ready to sync! 🎉"
                        }
                        onSignedIn()
                    }
                } else {
                    status.value = message ?: "Sign in failed — try again"
                }
            }
        }) {
            Text("Quick anonymous sign-in")
        }

        status.value?.let { Text(it) }
    }
}
