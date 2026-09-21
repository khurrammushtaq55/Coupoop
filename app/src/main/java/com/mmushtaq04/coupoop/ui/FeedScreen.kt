package com.mmushtaq04.coupoop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.LoggingManager
import com.mmushtaq04.coupoop.PairingManager
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.RatingManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoral
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.util.Log

@Composable
fun FeedScreen(
    onSignOut: () -> Unit = {},
    forcedUserId: String? = null,
    forcedPairingId: String? = null,
    onThemeChanged: (Int) -> Unit = {}
) {
    val user = if (LocalInspectionMode.current) null else AuthManager.currentUser()
    val userId = forcedUserId ?: user?.uid

    val isBypass = LocalInspectionMode.current || forcedUserId != null
    val usernameChecked = remember { mutableStateOf(isBypass) }
    val needsUsername = remember { mutableStateOf(false) }

    val pairingIdState = remember { mutableStateOf(forcedPairingId) }
    val pairingChecked = remember { mutableStateOf(forcedPairingId != null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val isPreview = LocalInspectionMode.current
    val logs = remember {
        mutableStateListOf<Map<String, Any>>().apply {
            if (isPreview && forcedPairingId != null) {
                add(mapOf(
                    "displayName" to "Alex",
                    "timestamp" to Timestamp.now(),
                    "bristolType" to 4L,
                    "color" to "brown",
                    "mood" to "good",
                    "reactions" to mapOf("uid1" to "❤️")
                ))
            }
        }
    }
    val status = remember { mutableStateOf<String?>(null) }
    val celebration = remember { mutableStateOf(false) }
    val showSettings = remember { mutableStateOf(false) }

    val currentStreak = remember { mutableIntStateOf(0) }
    val weeklyCount = remember { mutableIntStateOf(0) }

    val showSuccessOverlay = remember { mutableStateOf(false) }
    val successOverlayMessage = remember { mutableStateOf("") }
    
    val ctx = LocalContext.current

    DisposableEffect(userId, refreshTrigger, forcedPairingId) {
        if (isPreview) return@DisposableEffect onDispose {}
        var logsRegistration: ListenerRegistration? = null
        var celebrationsRegistration: ListenerRegistration? = null
        var streakRegistration: ListenerRegistration? = null

        if (forcedPairingId != null) {
            logsRegistration = LoggingManager.listenForLogs(forcedPairingId) { items ->
                logs.clear()
                logs.addAll(items)
                val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                weeklyCount.intValue = items.count {
                    (it["timestamp"] as? Timestamp)?.toDate()?.time ?: 0 >= sevenDaysAgo
                }
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
                        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                        weeklyCount.intValue = items.count {
                            (it["timestamp"] as? Timestamp)?.toDate()?.time ?: 0 >= sevenDaysAgo
                        }
                    }
                    var lastCelebrationId: String? = null
                    celebrationsRegistration = FirebaseFirestore.getInstance("coupoop")
                        .collection("pairings").document(pairingId)
                        .collection("celebrations")
                        .orderBy("at", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { snap, err ->
                            if (err != null || snap == null) return@addSnapshotListener
                            val doc = snap.documents.firstOrNull() ?: return@addSnapshotListener
                            
                            val isFirstRun = lastCelebrationId == null
                            if (doc.id != lastCelebrationId) {
                                lastCelebrationId = doc.id
                                if (!isFirstRun) {
                                    celebration.value = true
                                    successOverlayMessage.value = ctx.getString(R.string.sync_moment)
                                    showSuccessOverlay.value = true
                                }
                            }
                        }

                    streakRegistration = FirebaseFirestore.getInstance("coupoop")
                        .collection("pairings").document(pairingId)
                        .collection("streaks").document("sync")
                        .addSnapshotListener { snap, err ->
                            if (err != null || snap == null) return@addSnapshotListener
                            currentStreak.intValue = (snap.getLong("currentStreak") ?: 0L).toInt()
                        }
                }
            }
        }
        onDispose {
            logsRegistration?.remove()
            celebrationsRegistration?.remove()
            streakRegistration?.remove()
        }
    }

    LaunchedEffect(celebration.value) {
        if (celebration.value) {
            RatingManager.onCelebration(ctx)
            kotlinx.coroutines.delay(3000)
            celebration.value = false
        }
    }

    LaunchedEffect(showSuccessOverlay.value) {
        if (showSuccessOverlay.value) {
            kotlinx.coroutines.delay(2500)
            showSuccessOverlay.value = false
        }
    }

    LaunchedEffect(userId) {
        if (isBypass || userId == null) return@LaunchedEffect
        FirebaseFirestore.getInstance("coupoop").collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                needsUsername.value = doc.getBoolean("usernameSet") != true
                usernameChecked.value = true
            }
            .addOnFailureListener {
                // Don't block sign-in indefinitely over this check failing.
                usernameChecked.value = true
            }
    }

    if (userId != null && !usernameChecked.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightCoral)
        }
        return
    }

    if (userId != null && needsUsername.value) {
        NameEntryScreen(
            initialName = user?.displayName ?: "",
            onNameSet = { needsUsername.value = false }
        )
        return
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
            onAccountDeleted = onSignOut,
            onThemeChanged = onThemeChanged
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CoupoopTopBar(
                    title = stringResource(R.string.app_name),
                    onActionClick = { showSettings.value = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(painterResource(R.drawable.ic_poop_fill), contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.log_poop)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LightCoral,
                            selectedTextColor = LightCoral,
                            indicatorColor = LightChipBg
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        label = { Text(stringResource(R.string.recent_activity)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LightCoral,
                            selectedTextColor = LightCoral,
                            indicatorColor = LightChipBg
                        )
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (selectedTab == 0) {
                    LogScreen(
                        user = user,
                        userId = userId,
                        pairingId = pairingIdState.value,
                        currentStreak = currentStreak.intValue,
                        weeklyCount = weeklyCount.intValue,
                        lastLoggedTs = logs.firstOrNull()?.get("timestamp") as? Timestamp,
                        celebrationVisible = celebration.value,
                        onSuccess = { msg ->
                            successOverlayMessage.value = msg
                            showSuccessOverlay.value = true
                        },
                        status = status
                    )
                } else {
                    ActivityScreen(
                        logs = logs,
                        userId = userId,
                        pairingId = pairingIdState.value
                    )
                }
            }
        }

        SuccessOverlay(
            visible = showSuccessOverlay.value,
            message = successOverlayMessage.value
        )
    }
}

@Composable
fun StatsStrip(streak: Int, weeklyCount: Int, lastLoggedTs: Timestamp?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.streak_count, streak),
                style = MaterialTheme.typography.titleMedium,
                color = LightCoralDark
            )
            Text(
                text = stringResource(R.string.this_week_count, weeklyCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        lastLoggedTs?.let {
            Text(
                text = stringResource(R.string.last_logged, relativeTime(it.toDate())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        1 to (R.drawable.ic_bristol_type1 to "Pellets"),
        2 to (R.drawable.ic_bristol_type2 to "Lumpy"),
        3 to (R.drawable.ic_bristol_type3 to "Cracked"),
        4 to (R.drawable.ic_bristol_type4 to "Smooth"),
        5 to (R.drawable.ic_bristol_type5 to "Soft blobs"),
        6 to (R.drawable.ic_bristol_type6 to "Mushy"),
        7 to (R.drawable.ic_bristol_type7 to "Liquid")
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
fun BristolChip(id: Int, iconRes: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(76.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) LightCoral else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) Color.White else Color(0xFF8B4513)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PoopColorPicker(selected: String?, onSelect: (String?) -> Unit) {
    val colors = listOf(
        "brown" to ("Brown" to Color(0xFF8B4513)),
        "yellow" to ("Yellow" to Color(0xFFFFEB3B)),
        "green" to ("Green" to Color(0xFF4CAF50)),
        "black" to ("Black" to Color(0xFF212121)),
        "red" to ("Red" to Color(0xFFF44336))
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.take(3).forEach { (key, data) ->
                PoopColorChip(key, data.first, data.second, selected == key) { onSelect(if (selected == key) null else key) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.drop(3).forEach { (key, data) ->
                PoopColorChip(key, data.first, data.second, selected == key) { onSelect(if (selected == key) null else key) }
            }
        }
    }
}

@Composable
fun PoopColorChip(key: String, label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) LightCoral else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_poop_fill),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
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
fun VolumePicker(selected: String?, onSelect: (String?) -> Unit) {
    val volumes = listOf(
        "small" to (stringResource(R.string.volume_small) to 14.dp),
        "normal" to (stringResource(R.string.volume_normal) to 20.dp),
        "huge" to (stringResource(R.string.volume_huge) to 26.dp),
        "gigantic" to (stringResource(R.string.volume_gigantic) to 32.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        volumes.forEach { (key, data) ->
            VolumeChip(data.first, data.second, selected == key) { onSelect(if (selected == key) null else key) }
        }
    }
}

@Composable
fun RowScope.VolumeChip(label: String, iconSize: androidx.compose.ui.unit.Dp, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) LightCoral else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .height(56.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Same shared "poop pile" glyph as the Hue picker, just rendered at a
            // different size per option — same trick the reference app uses for volume.
            Icon(
                painter = painterResource(id = R.drawable.ic_poop_fill),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = if (isSelected) Color.White else Color(0xFF8B4513)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ConditionsPicker(selected: Set<String>, onToggle: (String) -> Unit) {
    val conditions = listOf(
        "burning" to (R.drawable.ic_condition_burning to stringResource(R.string.condition_burning)),
        "crampy" to (R.drawable.ic_condition_crampy to stringResource(R.string.condition_crampy)),
        "double_flush" to (R.drawable.ic_condition_double_flush to stringResource(R.string.condition_double_flush)),
        "floating" to (R.drawable.ic_condition_floating to stringResource(R.string.condition_floating)),
        "hard_to_pass" to (R.drawable.ic_condition_hard_to_pass to stringResource(R.string.condition_hard_to_pass)),
        "gassy" to (R.drawable.ic_condition_gassy to stringResource(R.string.condition_gassy))
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            conditions.take(3).forEach { (key, data) ->
                ConditionChip(data.first, data.second, selected.contains(key)) { onToggle(key) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            conditions.drop(3).forEach { (key, data) ->
                ConditionChip(data.first, data.second, selected.contains(key)) { onToggle(key) }
            }
        }
    }
}

@Composable
fun RowScope.ConditionChip(iconRes: Int, label: String, isChecked: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = if (isChecked) LightChipBg else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (isChecked) LightCoral else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed multi-color icons (not tinted) — Image() rather than Icon()
            // so the vector's own baked-in colors render as drawn, matching how
            // the Google logo is handled in LoginScreen.
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PoopFactCard(factText: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.fact_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = factText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}


//@Composable
//fun PhotoPicker(
//    selectedUri: android.net.Uri?,
//    onGalleryClick: () -> Unit,
//    onCameraClick: () -> Unit,
//    onRemove: () -> Unit
//) {
//    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//        if (selectedUri == null) {
//            Surface(
//                onClick = onCameraClick,
//                modifier = Modifier.size(64.dp),
//                shape = RoundedCornerShape(14.dp),
//                color = MaterialTheme.colorScheme.surface,
//                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
//                    Text(stringResource(R.string.take_picture), fontSize = 9.sp, modifier = Modifier.padding(top = 32.dp), textAlign = TextAlign.Center)
//                }
//            }
//            Surface(
//                onClick = onGalleryClick,
//                modifier = Modifier.size(64.dp),
//                shape = RoundedCornerShape(14.dp),
//                color = MaterialTheme.colorScheme.surface,
//                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
//                    Text(stringResource(R.string.pick_from_gallery), fontSize = 9.sp, modifier = Modifier.padding(top = 32.dp), textAlign = TextAlign.Center)
//                }
//            }
//        } else {
//            Box(modifier = Modifier.size(80.dp)) {
//                AsyncImage(
//                    model = selectedUri,
//                    contentDescription = null,
//                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
//                    contentScale = ContentScale.Crop
//                )
//                Surface(
//                    onClick = onRemove,
//                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(24.dp),
//                    shape = CircleShape,
//                    color = MaterialTheme.colorScheme.errorContainer,
//                    shadowElevation = 2.dp
//                ) {
//                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
//                }
//            }
//        }
//    }
//}

@Composable
fun CelebrationBanner(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = expandVertically(), exit = shrinkVertically()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            color = Color(0xFFFFC24B), // Amber
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                stringResource(R.string.sync_moment),
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
                Text(displayName ?: stringResource(R.string.someone), style = MaterialTheme.typography.titleMedium)
                Text(relativeTime(ts?.toDate()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val bristol = (log["bristolType"] as? Long)?.toInt()
            val color = log["color"] as? String
            val volume = log["volume"] as? String
            val mood = log["mood"] as? String

            val meta = listOfNotNull(
                bristol?.let { stringResource(R.string.bristol_type, it) },
                color?.replaceFirstChar { it.uppercase() },
                volume?.replaceFirstChar { it.uppercase() },
                mood?.replaceFirstChar { it.uppercase() }
            ).joinToString(" • ")

            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }

            @Suppress("UNCHECKED_CAST")
            val conditions = (log["conditions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (conditions.isNotEmpty()) {
                val conditionIcons = mapOf(
                    "burning" to R.drawable.ic_condition_burning,
                    "crampy" to R.drawable.ic_condition_crampy,
                    "double_flush" to R.drawable.ic_condition_double_flush,
                    "floating" to R.drawable.ic_condition_floating,
                    "hard_to_pass" to R.drawable.ic_condition_hard_to_pass,
                    "gassy" to R.drawable.ic_condition_gassy
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    conditions.forEach { key ->
                        conditionIcons[key]?.let { iconRes ->
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = key,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Photo Logic Phase 2
            /*
            val photoUrl = log["photoUrl"] as? String
            if (!photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            */

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
        Text(stringResource(R.string.no_logs_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun relativeTime(date: Date?): String {
    if (date == null) return ""
    val diffMinutes = (System.currentTimeMillis() - date.time) / 60000
    return when {
        diffMinutes < 1 -> stringResource(R.string.just_now)
        diffMinutes < 60 -> stringResource(R.string.minutes_ago, diffMinutes.toInt())
        diffMinutes < 60 * 24 -> stringResource(R.string.hours_ago, (diffMinutes / 60).toInt())
        else -> stringResource(R.string.days_ago, (diffMinutes / (60 * 24)).toInt())
    }
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    CoupoopTheme {
        FeedScreen(
            forcedUserId = "preview_user",
            forcedPairingId = "preview_pairing"
        )
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
                "photoUrl" to "https://placehold.co/600x400/png",
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

@Preview(showBackground = true)
@Composable
fun VolumePickerPreview() {
    CoupoopTheme {
        VolumePicker(selected = "huge", onSelect = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ConditionsPickerPreview() {
    CoupoopTheme {
        ConditionsPicker(selected = setOf("burning", "floating"), onToggle = {})
    }
}
