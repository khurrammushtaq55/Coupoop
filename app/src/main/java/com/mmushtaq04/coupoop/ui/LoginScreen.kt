package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.FcmManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark

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
        // 1. 💩 emoji
        Text("💩", fontSize = 56.sp)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 2. "Coupoop" wordmark
        Text(
            text = "Coupoop",
            style = MaterialTheme.typography.headlineLarge,
            color = LightCoralDark
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 3. Tagline
        Text(
            text = "The playful way to keep track together",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Primary Button
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
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge // Using round shape
        ) {
            Text(
                "Quick anonymous sign-in — start syncing 💨",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // 5. Divider
        Text(
            text = "— or —",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // 6. Google Sign-In Integration
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

        OutlinedButton(
            onClick = {
                status.value = "Opening Google Sign-In..."
                launcher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF3C4043)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDADCE0))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Placeholder for Google "G" logo
                Text("G ", fontWeight = FontWeight.Bold, color = Color.Blue) 
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sign in with Google",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        status.value?.let { 
            Text(
                it, 
                textAlign = TextAlign.Center, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CoupoopTheme {
        LoginScreen(onSignedIn = {})
    }
}
