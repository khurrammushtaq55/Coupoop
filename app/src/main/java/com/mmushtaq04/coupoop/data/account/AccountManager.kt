package com.mmushtaq04.coupoop.data.account

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mmushtaq04.coupoop.data.firebase.AuthManager

/**
 * Implements the account-deletion flow privacy_policy.md already promises
 * ("request full deletion") but that previously had no code behind it at all:
 * best-effort removal from any pairing, deletion of the users/{uid} profile
 * doc, then deletion of the Firebase Auth account itself.
 */
object AccountManager {
    private val db: FirebaseFirestore by lazy { com.mmushtaq04.coupoop.data.firebase.FirestoreRepository.db() }

    fun deleteAccount(onResult: (success: Boolean, message: String?) -> Unit) {
        val user = AuthManager.currentUser()
        if (user == null) {
            onResult(false, "Not signed in")
            return
        }
        val uid = user.uid

        db.collection("pairings").whereArrayContains("memberIds", uid).get()
            .addOnSuccessListener { pairingsSnap ->
                val leaveTasks = pairingsSnap.documents.map { doc ->
                    doc.reference.update("memberIds", FieldValue.arrayRemove(uid))
                }
                Tasks.whenAllComplete(leaveTasks).addOnCompleteListener {
                    finishDeletion(uid, user, onResult)
                }
            }
            .addOnFailureListener {
                // Still attempt the rest of the deletion even if the pairing lookup failed —
                // don't block the user from leaving entirely over a best-effort cleanup step.
                finishDeletion(uid, user, onResult)
            }
    }

    private fun finishDeletion(
        uid: String,
        user: com.google.firebase.auth.FirebaseUser,
        onResult: (Boolean, String?) -> Unit
    ) {
        db.collection("users").document(uid).delete()
            .addOnCompleteListener {
                user.delete()
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
            }
    }
}
