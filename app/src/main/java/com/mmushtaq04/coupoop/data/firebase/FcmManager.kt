package com.mmushtaq04.coupoop.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FcmManager {
    private val db = FirestoreRepository.db()

    fun registerTokenForCurrentUser(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val user = AuthManager.currentUser() ?: run {
            onResult(false, "Not signed in")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(false, task.exception?.localizedMessage)
                    return@addOnCompleteListener
                }
                val token = task.result
                if (token.isNullOrBlank()) {
                    onResult(false, "Empty token")
                    return@addOnCompleteListener
                }
                if (!FirestoreCostGuard.canWrite("fcm-token:${user.uid}", 15000L)) {
                    onResult(false, "Token registration is already in progress.")
                    return@addOnCompleteListener
                }
                val userRef = FirestoreRepository.userDoc(user.uid)
                userRef.set(mapOf("displayName" to (user.displayName ?: "")), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        userRef.update("fcmTokens", FieldValue.arrayUnion(token))
                            .addOnSuccessListener { onResult(true, null) }
                            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
                    }
                    .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
            }
    }
}
