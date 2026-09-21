package com.mmushtaq04.coupoop.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirestoreRepository {
    const val DATABASE_ID = "coupoop"
    const val DEFAULT_QUERY_LIMIT = 25

    fun db(): FirebaseFirestore = FirebaseFirestore.getInstance(DATABASE_ID)

    fun userDoc(userId: String) = db().collection("users").document(userId)

    fun pairingsForUser(userId: String): Query =
        db().collection("pairings").whereArrayContains("memberIds", userId)

    fun logsForPairing(pairingId: String, limit: Int = DEFAULT_QUERY_LIMIT): Query =
        db().collection("pairings").document(pairingId)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
}
