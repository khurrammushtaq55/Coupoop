package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

        Button(
            onClick = {
                status.value = "Signing in..."
                AuthManager.signInAnonymously { success, message ->
                    if (success) {
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
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Quick anonymous sign-in — start syncing 💨")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Google Sign-In Integration
        val context = androidx.compose.ui.platform.LocalContext.current
        val webClientId = "713853475701-h93q4arti6aiaiukfg9vr6om6ev757d7.apps.googleusercontent.com"
        
        val gso = remember {
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        }
        val googleSignInClient = remember { com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso) }

        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    status.value = "Authenticating with Google..."
                    AuthManager.signInWithGoogle(idToken) { success, msg ->
                        if (success) {
                            FcmManager.registerTokenForCurrentUser { _, _ ->
                                status.value = "Google account linked successfully! 🎉"
                                onSignedIn()
                            }
                        } else {
                            status.value = msg ?: "Firebase authentication failed"
                        }
                    }
                } else {
                    status.value = "Google login error: Could not retrieve ID Token."
                }
            } catch (e: Exception) {
                status.value = "Google login canceled or failed: ${e.localizedMessage}"
            }
        }

        androidx.compose.material3.OutlinedButton(
            onClick = {
                status.value = "Opening Google Sign-In..."
                launcher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Sign in with Google (Secure Backup) 🔐")
        }

        Spacer(modifier = Modifier.height(16.dp))
        status.value?.let { Text(it, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall) }
    }
}
