package com.mmushtaq04.coupoop.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.mmushtaq04.coupoop.data.firebase.AuthManager
import com.mmushtaq04.coupoop.data.repository.ThemeRepositoryImpl
import com.mmushtaq04.coupoop.domain.model.AppThemeMode
import com.mmushtaq04.coupoop.presentation.auth.LoginScreen
import com.mmushtaq04.coupoop.presentation.feed.FeedScreen
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme

@Composable
fun CoupoopApp() {
    val context = LocalContext.current
    val themeRepository = remember(context) { ThemeRepositoryImpl(context.applicationContext) }
    var themeMode by remember { mutableIntStateOf(themeRepository.getThemeMode().value) }
    var currentUser by remember { mutableStateOf(AuthManager.currentUser()) }
    var shouldRequestUsernameAfterSignIn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        AuthManager.addAuthStateListener(listener)
        onDispose { AuthManager.removeAuthStateListener(listener) }
    }

    CoupoopTheme(themeMode = themeMode) {
        if (currentUser == null) {
            LoginScreen(
                onSignedIn = {
                    shouldRequestUsernameAfterSignIn = true
                }
            )
        } else {
            FeedScreen(
                onSignOut = {
                    shouldRequestUsernameAfterSignIn = false
                    AuthManager.signOut()
                },
                shouldPromptForUsername = shouldRequestUsernameAfterSignIn,
                onUsernamePromptHandled = { shouldRequestUsernameAfterSignIn = false },
                onThemeChanged = { selectedTheme ->
                    themeMode = selectedTheme
                    themeRepository.setThemeMode(AppThemeMode.fromValue(selectedTheme))
                }
            )
        }
    }
}
