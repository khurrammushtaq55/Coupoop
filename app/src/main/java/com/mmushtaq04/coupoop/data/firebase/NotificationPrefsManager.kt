package com.mmushtaq04.coupoop.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Per-user notification preference: users/{uid}.mutedPairings (array of
 * pairingId). Checked by the onLogCreate Cloud Function before sending a
 * push to a given member. Uses set(merge) rather than update() since the
 * users/{uid} doc may not exist yet the very first time this is touched.
 */
object NotificationPrefsManager {
    private val db = FirestoreRepository.db()

    fun isPairingMuted(userUid: String, pairingId: String, onResult: (Boolean) -> Unit) {
        FirestoreRepository.userDoc(userUid).get()
            .addOnSuccessListener { doc ->
                val muted = doc.get("mutedPairings") as? List<*>
                onResult(muted?.contains(pairingId) == true)
            }
            .addOnFailureListener { onResult(false) }
    }

    fun setPairingMuted(userUid: String, pairingId: String, muted: Boolean, onResult: (Boolean, String?) -> Unit) {
        val guardKey = "mute:$userUid:$pairingId"
        if (!FirestoreCostGuard.canWrite(guardKey, 1000L)) {
            onResult(false, "Please wait before updating this mute preference.")
            return
        }

        val update = if (muted) FieldValue.arrayUnion(pairingId) else FieldValue.arrayRemove(pairingId)
        FirestoreRepository.userDoc(userUid)
            .set(mapOf("mutedPairings" to update), SetOptions.merge())
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }
}
