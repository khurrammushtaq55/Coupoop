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

    fun signOut() {
        auth.signOut()
    }
}
