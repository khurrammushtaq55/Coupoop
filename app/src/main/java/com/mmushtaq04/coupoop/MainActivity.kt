package com.mmushtaq04.coupoop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.mmushtaq04.coupoop.ui.LoginScreen
import com.mmushtaq04.coupoop.ui.PairingScreen
import com.mmushtaq04.coupoop.ui.FeedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle quick-log intent from widget deep-link
        val quickLog = intent?.getBooleanExtra("quick_log", false) ?: false

        setContent {
            CoupoopApp()
        }

        if (quickLog) {
            // Attempt background quick log if user is signed in and has a pairing
            val user = AuthManager.currentUser()
            if (user != null) {
                PairingManager.getFirstPairingForUser(user.uid) { pairingId ->
                    if (pairingId != null) {
                        LoggingManager.addLog(pairingId, user.uid, null, null, "Widget quick-log") { success, _ ->
                            // no-op; UI will reflect logs when app opens
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoupoopApp() {
    MaterialTheme {
        Surface {
            val user = AuthManager.currentUser()
            if (user == null) {
                LoginScreen(onSignedIn = { /* recomposition will show pairing screen */ })
            } else {
                // Show feed; if no pairing exists the FeedScreen will prompt to create/join
                FeedScreen()
            }
        }
    }
}