package com.mmushtaq04.coupoop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.Surface
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.mmushtaq04.coupoop.ui.LoginScreen
import com.mmushtaq04.coupoop.ui.FeedScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
            val logNote = getString(R.string.widget_log_note)
            PairingManager.getFirstPairingForUser(user.uid) { pairingId ->
                if (pairingId != null) {
                    LoggingManager.addLog(
                        pairingId = pairingId,
                        userId = user.uid,
                        note = "Widget quick-log",
                        displayName = user.displayName
                    ) { success, _ ->
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
    // --- DEBUG BYPASS ---
    // Set this to true to skip Login and Pairing screens during development.
    val isDebugBypass = false
    val debugUserId = "DEBUG_USER_123"
    val debugPairingId = "DEBUG_PAIR_456"
    // --------------------

    val context = androidx.compose.ui.platform.LocalContext.current
    var themeMode by remember { mutableIntStateOf(PrefsManager.getThemeMode(context)) }
    var currentUser by remember { mutableStateOf(AuthManager.currentUser()) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        AuthManager.addAuthStateListener(listener)
        onDispose { AuthManager.removeAuthStateListener(listener) }
    }

    CoupoopTheme(themeMode = themeMode) {
        androidx.compose.material3.Scaffold { innerPadding ->
            Surface(
                modifier = androidx.compose.ui.Modifier.padding(innerPadding)
            ) {
                if (isDebugBypass) {
                    FeedScreen(
                        onSignOut = { /* no-op in bypass */ },
                        forcedUserId = debugUserId,
                        forcedPairingId = debugPairingId,
                        onThemeChanged = { themeMode = it }
                    )
                } else if (currentUser == null) {
                    LoginScreen(onSignedIn = { /* AuthStateListener above updates currentUser */ })
                } else {
                    FeedScreen(
                        onSignOut = { AuthManager.signOut() },
                        onThemeChanged = { themeMode = it }
                    )
                }
            }
        }
    }
}