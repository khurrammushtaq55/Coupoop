package com.mmushtaq04.coupoop.data.purchase

/**
 * Lightweight purchase manager stub for gating premium features.
 * Replace with Play Billing integration for production.
 */
import com.google.firebase.firestore.SetOptions
import com.mmushtaq04.coupoop.data.firebase.FirestoreRepository

object PurchaseManager {
    // For MVP, premium flag is stored in users/{uid}.premium (boolean). This helper reads/writes that.
    fun isUserPremium(uid: String, onResult: (Boolean) -> Unit) {
        FirestoreRepository.userDoc(uid).get()
            .addOnSuccessListener { snap ->
                val isPrem = snap.exists() && (snap.data?.get("premium") as? Boolean == true)
                onResult(isPrem)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // Dev-only helper to mark user as premium (for testing). Real app should use Play Billing.
    fun markUserPremium(uid: String, onResult: (Boolean, String?) -> Unit) {
        FirestoreRepository.userDoc(uid)
            .set(mapOf("premium" to true), SetOptions.merge())
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }
}
