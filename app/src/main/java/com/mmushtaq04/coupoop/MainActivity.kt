package com.mmushtaq04.coupoop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.mmushtaq04.coupoop.ui.LoginScreen
import com.mmushtaq04.coupoop.ui.FeedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CoupoopApp()
        }

        handleQuickLogIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Widget tap reuses this instance (launchMode="singleTop"), so onCreate
        // won't run again — keep getIntent() in sync and re-check the extra here.
        setIntent(intent)
        handleQuickLogIntent(intent)
    }

    private fun handleQuickLogIntent(intent: Intent?) {
        val quickLog = intent?.getBooleanExtra("quick_log", false) ?: false
        if (!quickLog) return

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

        // Clear the extra so navigating away and back (e.g. via recents) doesn't re-fire it.
        intent?.removeExtra("quick_log")
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