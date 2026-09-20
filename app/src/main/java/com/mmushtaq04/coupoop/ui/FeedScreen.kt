package com.mmushtaq04.coupoop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.LoggingManager
import com.mmushtaq04.coupoop.PairingManager
import com.mmushtaq04.coupoop.RatingManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoral
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark
import java.util.Date

@Composable
fun FeedScreen(
    onSignOut: () -> Unit = {},
    forcedUserId: String? = null,
    forcedPairingId: String? = null
) {
    val user = AuthManager.currentUser()
    val userId = forcedUserId ?: user?.uid

    val pairingIdState = remember { mutableStateOf(forcedPairingId) }
    val pairingChecked = remember { mutableStateOf(forcedPairingId != null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val logs = remember { mutableStateListOf<Map<String, Any>>() }
    val status = remember { mutableStateOf<String?>(null) }
    val celebration = remember { mutableStateOf(false) }
    val showSettings = remember { mutableStateOf(false) }
    
    val selectedBristol = remember { mutableStateOf<Int?>(null) }
    val selectedColor = remember { mutableStateOf<String?>(null) }
    val selectedMood = remember { mutableStateOf<String?>(null) }
    
    val ctx = LocalContext.current

    DisposableEffect(userId, refreshTrigger, forcedPairingId) {
        var logsRegistration: ListenerRegistration? = null
        var celebrationsRegistration: ListenerRegistration? = null

        if (forcedPairingId != null) {
            // Bypass mode: use the forced pairing ID immediately
            logsRegistration = LoggingManager.listenForLogs(forcedPairingId) { items ->
                logs.clear()
                logs.addAll(items)
            }
        } else if (userId != null) {
            pairingChecked.value = false
            PairingManager.getFirstPairingForUser(userId) { pairingId ->
                pairingIdState.value = pairingId
                pairingChecked.value = true
                if (pairingId != null) {
                    logsRegistration = LoggingManager.listenForLogs(pairingId) { items ->
                        logs.clear()
                        logs.addAll(items)
                    }
                    celebrationsRegistration = FirebaseFirestore.getInstance("coupoop")
                        .collection("pairings").document(pairingId)
                        .collection("celebrations")
                        .orderBy("at", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { snap, err ->
                            if (err != null || snap == null) return@addSnapshotListener
                            if (!snap.isEmpty) { celebration.value = true }
                        }
                }
            }
        }
        onDispose {
            logsRegistration?.remove()
            celebrationsRegistration?.remove()
        }
    }

    LaunchedEffect(celebration.value) {
        if (celebration.value) {
            RatingManager.onCelebration(ctx)
            kotlinx.coroutines.delay(3000)
            celebration.value = false
        }
    }

    if (userId != null && !pairingChecked.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightCoral)
        }
        return
    }

    if (userId != null && pairingChecked.value && pairingIdState.value == null) {
        PairingScreen(
            onPaired = { refreshTrigger++ },
            onSettingsClick = { showSettings.value = true }
        )
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

    Scaffold(
        topBar = {
            Header(onSettingsClick = { showSettings.value = true })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                CelebrationBanner(visible = celebration.value)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel("Type (optional)")
                BristolTypePicker(selectedBristol.value) { selectedBristol.value = it }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel("Color (optional)")
                PoopColorPicker(selectedColor.value) { selectedColor.value = it }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel("Mood (optional)")
                MoodPicker(selectedMood.value) { selectedMood.value = it }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val pid = pairingIdState.value ?: return@Button
                        val uid = userId ?: return@Button
                        LoggingManager.addLog(
                            pairingId = pid,
                            userId = user!!.uid,
                            bristol = selectedBristol.value,
                            mood = selectedMood.value,
                            color = selectedColor.value,
                            displayName = user.displayName
                        ) { success, msg ->
                            if (success) {
                                selectedBristol.value = null
                                selectedColor.value = null
                                selectedMood.value = null
                                status.value = "Logged! 💩"
                            } else {
                                status.value = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text("Log 💩", style = MaterialTheme.typography.labelLarge)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    onClick = {
                        pairingIdState.value?.let { RecapShare.shareWeeklyRecap(ctx, it) }
                    },
                    shape = CircleShape,
                    color = LightChipBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("Share weekly recap", style = MaterialTheme.typography.labelLarge, color = LightCoralDark)
                    }
                }

                status.value?.let {
                    Text(
                        it, 
                        modifier = Modifier.padding(top = 12.dp), 
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightCoralDark
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Recent activity", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp))
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (logs.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(items = logs) { log ->
                    LogCard(log = log, currentUserId = userId, pairingId = pairingIdState.value)
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun Header(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("coupoop", style = MaterialTheme.typography.headlineMedium, color = LightCoralDark)
        Surface(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(12.dp),
            color = LightChipBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⚙", fontSize = 18.sp, color = LightCoralDark)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun BristolTypePicker(selected: Int?, onSelect: (Int?) -> Unit) {
    val types = listOf(
        1 to ("🐐" to "Pellets"),
        2 to ("🌰" to "Lumpy"),
        3 to ("🌭" to "Cracked"),
        4 to ("🐍" to "Smooth"),
        5 to ("🫘" to "Soft blobs"),
        6 to ("☁️" to "Mushy"),
        7 to ("💦" to "Liquid")
    )
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.take(4).forEach { (id, data) ->
                BristolChip(id, data.first, data.second, selected == id) { onSelect(if (selected == id) null else id) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.drop(4).forEach { (id, data) ->
                BristolChip(id, data.first, data.second, selected == id) { onSelect(if (selected == id) null else id) }
            }
        }
    }
}

@Composable
fun BristolChip(id: Int, emoji: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(76.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) LightCoral else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(id.toString(), fontSize = 9.sp, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 20.sp)
                    Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 12.sp, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun PoopColorPicker(selected: String?, onSelect: (String?) -> Unit) {
    val colors = listOf("brown" to "🟤 Brown", "yellow" to "🟡 Yellow", "green" to "🟢 Green", "black" to "⚫ Black", "red" to "🔴 Red")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (key, label) ->
            val isSelected = selected == key
            Surface(
                onClick = { onSelect(if (isSelected) null else key) },
                shape = CircleShape,
                color = if (isSelected) LightCoral else MaterialTheme.colorScheme.surface,
                border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun MoodPicker(selected: String?, onSelect: (String?) -> Unit) {
    val moods = listOf("good" to "😊", "okay" to "😐", "rough" to "😣")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        moods.forEach { (key, emoji) ->
            val isSelected = selected == key
            Surface(
                onClick = { onSelect(if (isSelected) null else key) },
                modifier = Modifier.size(width = 46.dp, height = 40.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) LightCoral else MaterialTheme.colorScheme.surface,
                border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun CelebrationBanner(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = expandVertically(), exit = shrinkVertically()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            color = Color(0xFFFFC24B), // Amber
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "🎉 Sync moment! You two logged close together 🎉",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF3F2E00),
                modifier = Modifier.padding(14.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LogCard(log: Map<String, Any>, currentUserId: String?, pairingId: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val ts = log["timestamp"] as? Timestamp
            val displayName = log["displayName"] as? String
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(displayName ?: "Someone", style = MaterialTheme.typography.titleMedium)
                Text(relativeTime(ts?.toDate()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            val bristol = (log["bristolType"] as? Long)?.toInt()
            val color = log["color"] as? String
            val mood = log["mood"] as? String
            
            val meta = listOfNotNull(
                bristol?.let { "Type $it" },
                color?.replaceFirstChar { it.uppercase() },
                mood?.replaceFirstChar { it.uppercase() }
            ).joinToString(" • ")
            
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            
            val logId = log["id"] as? String
            val reactions = (log["reactions"] as? Map<*, *>) ?: emptyMap<String, String>()
            
            if (reactions.isNotEmpty()) {
                val typedReactions = reactions.mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { value -> key to value } } }.toMap()
                val summary = typedReactions.values.groupingBy { it }.eachCount()
                    .entries.joinToString(" ") { (emoji, count) -> if (count > 1) "$emoji×$count" else emoji }
                Text(summary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("❤️", "😂", "👍").forEach { emoji ->
                    val isMyReaction = currentUserId?.let { reactions[it] == emoji } ?: false
                    Surface(
                        onClick = {
                            if (pairingId != null && logId != null && currentUserId != null) {
                                LoggingManager.setReaction(pairingId, logId, currentUserId, if (isMyReaction) null else emoji) { _, _ -> }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMyReaction) LightChipBg else Color.Transparent,
                        border = BorderStroke(1.5.dp, if (isMyReaction) LightCoral else MaterialTheme.colorScheme.outline)
                    ) {
                        Text(emoji, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🫥", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("No logs yet — tap 'Log 💩' to start", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    CoupoopTheme {
        FeedScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun LogCardPreview() {
    CoupoopTheme {
        LogCard(
            log = mapOf(
                "displayName" to "Alex",
                "timestamp" to Timestamp.now(),
                "bristolType" to 4L,
                "color" to "brown",
                "mood" to "good",
                "reactions" to mapOf("uid1" to "❤️", "uid2" to "😂")
            ),
            currentUserId = "uid1",
            pairingId = "pair123"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BristolTypePickerPreview() {
    CoupoopTheme {
        BristolTypePicker(selected = 4, onSelect = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PoopColorPickerPreview() {
    CoupoopTheme {
        PoopColorPicker(selected = "brown", onSelect = {})
    }
}
