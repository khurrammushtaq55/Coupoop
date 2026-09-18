package com.mmushtaq04.coupoop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import java.util.Date

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
                    status.value = null

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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Sync", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { showSettings.value = true }) {
                Text("⚙ Settings")
            }
        }

        AnimatedVisibility(
            visible = celebration.value,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    "🎉 Sync moment! You two logged close together 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        status.value?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

        Text(
            "Bristol type (optional)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
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

        Text(
            "Mood (optional)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
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
        }, modifier = Modifier.padding(top = 16.dp)) {
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

        Text(
            "Recent activity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🫥", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No logs yet — tap \"Log 💩\" to start", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn {
                items(items = logs) { item ->
                    ElevatedCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val ts = item["timestamp"] as? Timestamp
                            val userId = item["userId"] as? String
                            val displayName = item["displayName"] as? String
                            Text(
                                text = "${displayName ?: userId ?: "unknown"} — ${relativeTime(ts?.toDate())}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val bristol = (item["bristolType"] as? Long)?.toInt()
                            val mood = item["mood"] as? String
                            if (bristol != null || mood != null) {
                                Text(
                                    listOfNotNull(
                                        bristol?.let { "Type $it" },
                                        mood?.let { it.replaceFirstChar { c -> c.uppercase() } }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            val note = item["note"] as? String
                            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}

/** Short "Xm/Xh/Xd ago" label — friendlier than a raw Date.toString(). */
private fun relativeTime(date: Date?): String {
    if (date == null) return ""
    val diffMinutes = (System.currentTimeMillis() - date.time) / 60000
    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 60 * 24 -> "${diffMinutes / 60}h ago"
        else -> "${diffMinutes / (60 * 24)}d ago"
    }
}
