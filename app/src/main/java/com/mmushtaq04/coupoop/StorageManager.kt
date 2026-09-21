package com.mmushtaq04.coupoop

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.UUID

object StorageManager {
    private val storage = Firebase.storage

    fun uploadPhoto(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("photos/$fileName")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    onResult(true, downloadUri.toString())
                }.addOnFailureListener { e ->
                    onResult(false, e.localizedMessage)
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }
}
