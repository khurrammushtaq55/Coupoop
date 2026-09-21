package com.mmushtaq04.coupoop.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.mmushtaq04.coupoop.StorageManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoral
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.PartySystem
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun FeedScreen(
    onSignOut: () -> Unit = {},
    forcedUserId: String? = null,
    forcedPairingId: String? = null,
    onThemeChanged: (Int) -> Unit = {}
) {
    val user = if (LocalInspectionMode.current) null else AuthManager.currentUser()
    val userId = forcedUserId ?: user?.uid

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
    
    val selectedBristol = remember { mutableStateOf<Int?>(null) }
    val selectedColor = remember { mutableStateOf<String?>(null) }
    val selectedMood = remember { mutableStateOf<String?>(null) }
    val selectedImageUri = remember { mutableStateOf<android.net.Uri?>(null) }
    
    val currentStreak = remember { mutableIntStateOf(0) }
    val weeklyCount = remember { mutableIntStateOf(0) }

    val confettiState = remember { mutableStateListOf<Party>() }
    
    val ctx = LocalContext.current
    val successMessages = listOf(
        R.string.logged_success_1,
        R.string.logged_success_2,
        R.string.logged_success_3,
        R.string.logged_success_4,
        R.string.logged_success_5,
        R.string.logged_success_6
    )
    val uploadingPhotoMsg = stringResource(R.string.uploading_photo)

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri.value = uri
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        // tempImageUri is already set, so if success we just keep it
    }
    
    var tempImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    DisposableEffect(userId, refreshTrigger, forcedPairingId) {
        if (isPreview) return@DisposableEffect onDispose {}
        var logsRegistration: ListenerRegistration? = null
        var celebrationsRegistration: ListenerRegistration? = null
        var streakRegistration: ListenerRegistration? = null

        if (forcedPairingId != null) {
            // Bypass mode: use the forced pairing ID immediately
            logsRegistration = LoggingManager.listenForLogs(forcedPairingId) { items ->
                logs.clear()
                logs.addAll(items)
                
                // Calculate weekly count locally from the logs we already have
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
                        
                        // Calculate weekly count locally
                        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                        weeklyCount.intValue = items.count { 
                            (it["timestamp"] as? Timestamp)?.toDate()?.time ?: 0 >= sevenDaysAgo 
                        }
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

    Scaffold(
        topBar = {
            CoupoopTopBar(
                title = stringResource(R.string.app_name),
                onActionClick = { showSettings.value = true }
            )
        }
    ) { padding ->
        val facts = remember {
            listOf(
                R.string.fact_1,
                R.string.fact_2,
                R.string.fact_3,
                R.string.fact_4,
                R.string.fact_5,
                R.string.fact_6,
                R.string.fact_7,
                R.string.fact_8,
                R.string.fact_9,
                R.string.fact_10,
                R.string.fact_11,
                R.string.fact_12
            )
        }
        val randomFactRes = remember { facts.random() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                StatsStrip(
                    streak = currentStreak.intValue,
                    weeklyCount = weeklyCount.intValue,
                    lastLoggedTs = logs.firstOrNull()?.get("timestamp") as? Timestamp
                )
                
                CelebrationBanner(visible = celebration.value)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                PoopFactCard(factText = stringResource(id = randomFactRes))

                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel(stringResource(R.string.type_optional))
                BristolTypePicker(selectedBristol.value) { selectedBristol.value = it }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel(stringResource(R.string.color_optional))
                PoopColorPicker(selectedColor.value) { selectedColor.value = it }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel(stringResource(R.string.mood_optional))
                MoodPicker(selectedMood.value) { selectedMood.value = it }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SectionLabel(stringResource(R.string.picture_optional))
                PhotoPicker(
                    selectedUri = selectedImageUri.value,
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onCameraClick = {
                        val file = File(ctx.cacheDir, "images/${UUID.randomUUID()}.jpg").apply {
                            parentFile?.mkdirs()
                        }
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                        tempImageUri = uri
                        selectedImageUri.value = uri
                        cameraLauncher.launch(uri)
                    },
                    onRemove = { selectedImageUri.value = null }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val pid = pairingIdState.value ?: return@Button
                        val uid = userId ?: return@Button
                        
                        val onComplete: (String?) -> Unit = { photoUrl ->
                            LoggingManager.addLog(
                                pairingId = pid,
                                userId = uid,
                                bristol = selectedBristol.value,
                                mood = selectedMood.value,
                                color = selectedColor.value,
                                photoUrl = photoUrl,
                                displayName = user?.displayName ?: "Debug User"
                            ) { success, msg ->
                                if (success) {
                                    selectedBristol.value = null
                                    selectedColor.value = null
                                    selectedMood.value = null
                                    selectedImageUri.value = null
                                    status.value = ctx.getString(successMessages.random())

                                    // Trigger confetti burst 💩🎉
                                    confettiState.addAll(
                                        listOf(
                                            Party(
                                                speed = 0f,
                                                maxSpeed = 30f,
                                                damping = 0.9f,
                                                spread = 360,
                                                colors = listOf(0xFFB94A31.toInt(), 0xFFFF6B4A.toInt(), 0xFF2AB6A6.toInt()),
                                                position = Position.Relative(0.5, 0.7),
                                                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(30)
                                            )
                                        )
                                    )
                                } else {
                                    status.value = msg
                                }
                            }
                        }

                        if (selectedImageUri.value != null) {
                            status.value = uploadingPhotoMsg
                            StorageManager.uploadPhoto(selectedImageUri.value!!) { ok, url ->
                                if (ok) onComplete(url)
                                else status.value = url
                            }
                        } else {
                            onComplete(null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text(stringResource(R.string.log_poop), style = MaterialTheme.typography.labelLarge)
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
                        Text(stringResource(R.string.share_recap), style = MaterialTheme.typography.labelLarge, color = LightCoralDark)
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
                Text(stringResource(R.string.recent_activity), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp))
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

        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = confettiState,
            updateListener = object : OnParticleSystemUpdateListener {
                override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                    if (activeSystems == 0) {
                        confettiState.clear()
                    }
                }
            }
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
fun PhotoPicker(
    selectedUri: android.net.Uri?,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selectedUri == null) {
            Surface(
                onClick = onCameraClick,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.take_picture), fontSize = 9.sp, modifier = Modifier.padding(top = 32.dp), textAlign = TextAlign.Center)
                }
            }
            Surface(
                onClick = onGalleryClick,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.pick_from_gallery), fontSize = 9.sp, modifier = Modifier.padding(top = 32.dp), textAlign = TextAlign.Center)
                }
            }
        } else {
            Box(modifier = Modifier.size(80.dp)) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 2.dp
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
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
            val mood = log["mood"] as? String
            
            val meta = listOfNotNull(
                bristol?.let { stringResource(R.string.bristol_type, it) },
                color?.replaceFirstChar { it.uppercase() },
                mood?.replaceFirstChar { it.uppercase() }
            ).joinToString(" • ")
            
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            
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
