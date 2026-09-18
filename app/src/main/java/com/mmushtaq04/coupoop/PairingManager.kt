package com.mmushtaq04.coupoop

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object PairingManager {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun generateInviteCode(length: Int = 6): String {
        val chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun createPairing(inviterUid: String, onResult: (success: Boolean, inviteCode: String?, message: String?) -> Unit) {
        // Enforce free tier: only 1 active pairing per non-premium user
        db.collection("users").document(inviterUid).get()
            .addOnSuccessListener { uSnap ->
                val isPremium = uSnap.exists() && (uSnap.data?.get("premium") as? Boolean == true)
                // count existing pairings
                db.collection("pairings").whereArrayContains("memberIds", inviterUid).get()
                    .addOnSuccessListener { pSnap ->
                        val count = pSnap.size()
                        if (!isPremium && count >= 1) {
                            onResult(false, null, "Free tier allows only 1 active pairing. Upgrade to premium for multiple pairings.")
                            return@addOnSuccessListener
                        }
                        val invite = generateInviteCode()
                        val docRef = db.collection("pairings").document()
                        val payload = mapOf(
                            "inviteCode" to invite,
                            "memberIds" to listOf(inviterUid),
                            "createdAt" to Timestamp.now()
                        )
                        docRef.set(payload)
                            .addOnSuccessListener { onResult(true, invite, null) }
                            .addOnFailureListener { e -> onResult(false, null, e.localizedMessage) }
                    }
                    .addOnFailureListener { e -> onResult(false, null, e.localizedMessage) }
            }
            .addOnFailureListener { e -> onResult(false, null, e.localizedMessage) }
    }

    fun acceptPairingByCode(code: String, userUid: String, onResult: (success: Boolean, message: String?) -> Unit) {
        db.collection("pairings")
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onResult(false, "Invite code not found")
                    return@addOnSuccessListener
                }
                val doc = snap.documents[0]
                // Add user to memberIds using arrayUnion
                doc.reference.update("memberIds", FieldValue.arrayUnion(userUid))
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
            }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun getFirstPairingForUser(userUid: String, onResult: (pairingId: String?) -> Unit) {
        db.collection("pairings")
            .whereArrayContains("memberIds", userUid)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onResult(null)
                } else {
                    onResult(snap.documents[0].id)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun leavePairing(pairingId: String, userUid: String, onResult: (success: Boolean, message: String?) -> Unit) {
        db.collection("pairings").document(pairingId)
            .update("memberIds", FieldValue.arrayRemove(userUid))
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }
}
