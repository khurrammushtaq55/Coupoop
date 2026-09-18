package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.LoggingManager
import com.mmushtaq04.coupoop.PairingManager

@Composable
fun FeedScreen(onSignOut: () -> Unit = {}) {
    val user = AuthManager.currentUser()
    val pairingIdState = remember { mutableStateOf<String?>(null) }
    val pairingChecked = remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val logs = remember { mutableStateListOf<Map<String, Any>>() }
    val status = remember { mutableStateOf<String?>(null) }
    val celebration = remember { mutableStateOf(false) }
    val showSettings = remember { mutableStateOf(false) }
    val selectedBristol = remember { mutableStateOf<Int?>(null) }
    val selectedMood = remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    DisposableEffect(user, refreshTrigger) {
        var logsRegistration: ListenerRegistration? = null
        var celebrationsRegistration: ListenerRegistration? = null

        if (user != null) {
            pairingChecked.value = false
            PairingManager.getFirstPairingForUser(user.uid) { pairingId ->
                pairingIdState.value = pairingId
                pairingChecked.value = true
                if (pairingId == null) {
                    status.value = "No pairing found — create or join one first."
                } else {
                    status.value = "Connected to pairing: $pairingId"

                    // start listening for logs
                    logsRegistration = LoggingManager.listenForLogs(pairingId) { items ->
                        logs.clear()
                        logs.addAll(items)
                    }

                    // start listening for celebrations
                    celebrationsRegistration = FirebaseFirestore.getInstance()
                        .collection("pairings").document(pairingId)
                        .collection("celebrations")
                        .orderBy("at", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { snap, err ->
                            if (err != null || snap == null) return@addSnapshotListener
                            if (!snap.isEmpty) {
                                // Trigger a celebration UI state
                                celebration.value = true
                            }
                        }
                }
            }
        }

        onDispose {
            // Detach both listeners whenever the user/pairing changes or this
            // screen leaves composition — previously these were never removed.
            logsRegistration?.remove()
            celebrationsRegistration?.remove()
        }
    }

    // Auto-clear celebration after a short time
    LaunchedEffect(celebration.value) {
        if (celebration.value) {
            kotlinx.coroutines.delay(3000)
            celebration.value = false
        }
    }

    if (user != null && !pairingChecked.value) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Checking your pairing...")
        }
        return
    }

    if (user != null && pairingChecked.value && pairingIdState.value == null) {
        // No pairing yet — send them to create/join instead of a dead-end status line.
        PairingScreen(onPaired = { refreshTrigger++ })
        return
    }

    if (showSettings.value) {
        SettingsScreen(
            pairingId = pairingIdState.value,
            onBack = { showSettings.value = false },
            onSignOut = onSignOut,
            onLeftPairing = {
                showSettings.value = false
                refreshTrigger++
            },
            onAccountDeleted = onSignOut
        )
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Button(onClick = { showSettings.value = true }) {
            Text("⚙ Settings")
        }

        status.value?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

        Text("Bristol type (optional)", modifier = Modifier.padding(top = 12.dp))
        Row(modifier = Modifier.padding(top = 4.dp)) {
            for (i in 1..7) {
                Button(
                    onClick = { selectedBristol.value = if (selectedBristol.value == i) null else i },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(if (selectedBristol.value == i) "✓$i" else "$i")
                }
            }
        }

        Text("Mood (optional)", modifier = Modifier.padding(top = 12.dp))
        Row(modifier = Modifier.padding(top = 4.dp)) {
            val moods = listOf("😊" to "good", "😐" to "okay", "😣" to "rough")
            moods.forEach { (emoji, key) ->
                Button(
                    onClick = { selectedMood.value = if (selectedMood.value == key) null else key },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(if (selectedMood.value == key) "✓$emoji" else emoji)
                }
            }
        }

        Button(onClick = {
            val pid = pairingIdState.value
            if (pid == null || user == null) {
                status.value = "No pairing available"
                return@Button
            }
            LoggingManager.addLog(pid, user.uid, selectedBristol.value, selectedMood.value, null, user.displayName) { success, message ->
                status.value = if (success) "Logged!" else (message ?: "Log failed")
                if (success) {
                    selectedBristol.value = null
                    selectedMood.value = null
                }
            }
        }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Log 💩")
        }

        Button(onClick = {
            // Share weekly recap image (pulls real stats for this pairing)
            val pid = pairingIdState.value
            if (pid == null) {
                status.value = "No pairing available"
            } else {
                RecapShare.shareWeeklyRecap(ctx, pid)
            }
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
