package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.LoggingManager
import com.mmushtaq04.coupoop.PairingManager

@Composable
fun FeedScreen() {
    val user = AuthManager.currentUser()
    val pairingIdState = remember { mutableStateOf<String?>(null) }
    val logs = remember { mutableStateListOf<Map<String, Any>>() }
    var listenerRegistration by remember { mutableStateOf<Any?>(null) }
    val status = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        if (user == null) return@LaunchedEffect
        PairingManager.getFirstPairingForUser(user.uid) { pairingId ->
            pairingIdState.value = pairingId
            if (pairingId == null) {
                status.value = "No pairing found — create or join one first."
            } else {
                status.value = "Connected to pairing: $pairingId"
                // start listening for logs
                val reg = LoggingManager.listenForLogs(pairingId) { items ->
                    logs.clear()
                    logs.addAll(items)
                }
                listenerRegistration = reg

                // start listening for celebrations
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val celebReg = db.collection("pairings").document(pairingId)
                    .collection("celebrations")
                    .orderBy("at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .addSnapshotListener { snap, err ->
                        if (err != null || snap == null) return@addSnapshotListener
                        if (!snap.isEmpty) {
                            // Trigger a celebration UI state
                            celebration.value = true
                        }
                    }
                // store the registration so we can remove it later if needed
                // listenerRegistration is for logs; we don't keep the celeb reg reference strongly here for brevity
            }
        }
    }

    // Auto-clear celebration after a short time
    LaunchedEffect(celebration.value) {
        if (celebration.value) {
            kotlinx.coroutines.delay(3000)
            celebration.value = false
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        status.value?.let { Text(it) }

        Button(onClick = {
            val pid = pairingIdState.value
            if (pid == null || user == null) {
                status.value = "No pairing available"
                return@Button
            }
            // Quick one-tap log: minimal fields
            LoggingManager.addLog(pid, user.uid, null, null, null) { success, message ->
                status.value = if (success) "Logged!" else (message ?: "Log failed")
            }
        }, modifier = Modifier.padding(top = 12.dp)) {
            Text("One-tap log 💩")
        }

        Button(onClick = {
            // Share weekly recap image
            val ctx = androidx.compose.ui.platform.LocalContext.current
            RecapShare.shareWeeklyRecap(ctx)
        }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Share weekly recap")
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(items = logs) { item ->
                Card(modifier = Modifier.padding(6.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        val ts = item["timestamp"] as? Timestamp
                        val userId = item["userId"] as? String
                        Text(text = "${userId ?: "unknown"} — ${ts?.toDate() ?: ""}")
                        val note = item["note"] as? String
                        note?.let { Text(it) }
                    }
                }
            }
        }
    }
}
