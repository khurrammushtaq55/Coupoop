package com.mmushtaq04.coupoop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mmushtaq04.coupoop.data.firebase.AuthManager
import com.mmushtaq04.coupoop.data.firebase.LoggingManager
import com.mmushtaq04.coupoop.data.firebase.PairingManager
import com.mmushtaq04.coupoop.presentation.CoupoopApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
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

        // Attempt background quick log if user is signed in and has a pairing.
        val user = AuthManager.currentUser()
        if (user != null) {
            PairingManager.getFirstPairingForUser(user.uid) { pairingId ->
                if (pairingId != null) {
                    LoggingManager.addLog(
                        pairingId = pairingId,
                        userId = user.uid,
                        note = "Widget quick-log",
                        displayName = user.displayName
                    ) { _, _ ->
                        // no-op; UI will reflect logs when app opens
                    }
                }
            }
        }

        // Clear the extra so navigating away and back (e.g. via recents) doesn't re-fire it.
        intent?.removeExtra("quick_log")
    }
}
