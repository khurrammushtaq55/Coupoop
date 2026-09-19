package com.mmushtaq04.coupoop

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object LoggingManager {
    private val db = FirebaseFirestore.getInstance("coupoop")

    fun addLog(
        pairingId: String,
        userId: String,
        bristol: Int? = null,
        mood: String? = null,
        note: String? = null,
        displayName: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        val payload = hashMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName,
            "timestamp" to Timestamp.now(),
            "bristolType" to bristol,
            "mood" to mood,
            "note" to note
        )
        db.collection("pairings").document(pairingId)
            .collection("logs").add(payload)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    fun listenForLogs(pairingId: String, onChange: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        return db.collection("pairings").document(pairingId)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                // Include the doc id (needed for reactions) alongside the stored fields.
                val items = snap.documents.map { doc -> (doc.data ?: emptyMap()) + ("id" to doc.id) }
                onChange(items)
            }
    }

    /** Sets (or clears, with emoji = null) the calling user's reaction on a log entry. */
    fun setReaction(pairingId: String, logId: String, userId: String, emoji: String?, onResult: (Boolean, String?) -> Unit) {
        val value: Any = emoji ?: com.google.firebase.firestore.FieldValue.delete()
        db.collection("pairings").document(pairingId)
            .collection("logs").document(logId)
            .update("reactions.$userId", value)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }
}
