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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.FcmManager
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val status = remember { mutableStateOf<String?>(null) }
    val signingInMsg = stringResource(R.string.signing_in)
    val signInSuccessMsg = stringResource(R.string.sign_in_success)
    val signInFailedMsg = stringResource(R.string.sign_in_failed)
    val googleAuthMsg = stringResource(R.string.google_authenticating)
    val googleLinkMsg = stringResource(R.string.google_link_success)
    val fcmFailMsg = stringResource(R.string.fcm_reg_failed)

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
        
        // 2. "coupoop" wordmark
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = LightCoralDark
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 3. Tagline
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Primary Button
        Button(
            onClick = {
                status.value = signingInMsg
                AuthManager.signInAnonymously { success, message ->
                    if (success) {
                        FcmManager.registerTokenForCurrentUser { regOk, regMsg ->
                            status.value = if (!regOk) {
                                fcmFailMsg.format(regMsg ?: "unknown")
                            } else {
                                signInSuccessMsg
                            }
                            onSignedIn()
                        }
                    } else {
                        status.value = message ?: signInFailedMsg
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge // Using round shape
        ) {
            Text(
                stringResource(R.string.quick_sign_in),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // 5. Divider
        Text(
            text = stringResource(R.string.or_divider),
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
                    status.value = googleAuthMsg
                    AuthManager.signInWithGoogle(idToken) { success, msg ->
                        if (success) {
                            FcmManager.registerTokenForCurrentUser { _, _ ->
                                status.value = googleLinkMsg
                                onSignedIn()
                            }
                        } else {
                            status.value = msg ?: context.getString(R.string.firebase_auth_failed)
                        }
                    }
                } else {
                    status.value = context.getString(R.string.google_token_error)
                }
            } catch (e: Exception) {
                status.value = context.getString(R.string.google_login_failed, e.localizedMessage)
            }
        }

        OutlinedButton(
            onClick = {
                status.value = context.getString(R.string.opening_google_sign_in)
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
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    stringResource(R.string.sign_in_with_google),
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
