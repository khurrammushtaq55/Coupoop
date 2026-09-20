package com.mmushtaq04.coupoop

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object AuthManager {
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun signInAnonymously(onResult: (success: Boolean, message: String?) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    fun signInWithGoogle(idToken: String, onResult: (success: Boolean, message: String?) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        
        // If an anonymous account is already active, link it instead of creating a fresh one!
        val anonymousUser = auth.currentUser
        if (anonymousUser != null && anonymousUser.isAnonymous) {
            anonymousUser.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        // If linking fails because this Google account already exists, fall back to standard sign-in
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener { loginTask ->
                                if (loginTask.isSuccessful) onResult(true, null)
                                else onResult(false, loginTask.exception?.localizedMessage)
                            }
                    }
                }
        } else {
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage)
                    }
                }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Lets UI observe sign-in/sign-out reactively instead of reading
    // currentUser() once at composition time.
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
