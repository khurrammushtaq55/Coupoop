package com.mmushtaq04.coupoop

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object LoggingManager {
    private val db = FirebaseFirestore.getInstance()

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
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val items = snap.documents.map { it.data ?: emptyMap<String, Any>() }
                onChange(items)
            }
    }
}
