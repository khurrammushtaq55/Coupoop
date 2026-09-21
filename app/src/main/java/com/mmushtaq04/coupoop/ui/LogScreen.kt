package com.mmushtaq04.coupoop.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mmushtaq04.coupoop.LoggingManager
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.StorageManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoral
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark
import com.google.firebase.auth.FirebaseUser

@Composable
fun LogScreen(
    user: FirebaseUser?,
    userId: String?,
    pairingId: String?,
    currentStreak: Int,
    weeklyCount: Int,
    lastLoggedTs: Timestamp?,
    celebrationVisible: Boolean,
    onConfettiBurst: (List<Party>) -> Unit,
    status: MutableState<String?>
) {
    val selectedBristol = remember { mutableStateOf<Int?>(null) }
    val selectedColor = remember { mutableStateOf<String?>(null) }
    val selectedMood = remember { mutableStateOf<String?>(null) }
    val selectedVolume = remember { mutableStateOf<String?>(null) }
    val selectedConditions = remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedImageUri = remember { mutableStateOf<android.net.Uri?>(null) }
    
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
    val loggedSuccessMsg = stringResource(R.string.logged_success_1)

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri.value = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> }
    var tempImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

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
            .padding(horizontal = 20.dp)
    ) {
        item {
            StatsStrip(
                streak = currentStreak,
                weeklyCount = weeklyCount,
                lastLoggedTs = lastLoggedTs
            )

            CelebrationBanner(visible = celebrationVisible)

            Spacer(modifier = Modifier.height(8.dp))

            PoopFactCard(factText = stringResource(id = randomFactRes))

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.type_optional))
            BristolTypePicker(selectedBristol.value) { selectedBristol.value = it }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.volume_optional))
            VolumePicker(selectedVolume.value) { selectedVolume.value = it }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.color_optional))
            PoopColorPicker(selectedColor.value) { selectedColor.value = it }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.mood_optional))
            MoodPicker(selectedMood.value) { selectedMood.value = it }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.conditions_optional))
            ConditionsPicker(selectedConditions.value) { key ->
                selectedConditions.value = if (selectedConditions.value.contains(key)) {
                    selectedConditions.value - key
                } else {
                    selectedConditions.value + key
                }
            }

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
                    val pid = pairingId ?: return@Button
                    val uid = userId ?: return@Button

                    val onComplete: (String?) -> Unit = { photoUrl ->
                        LoggingManager.addLog(
                            pairingId = pid,
                            userId = uid,
                            bristol = selectedBristol.value,
                            mood = selectedMood.value,
                            color = selectedColor.value,
                            volume = selectedVolume.value,
                            conditions = selectedConditions.value.toList().ifEmpty { null },
                            photoUrl = photoUrl,
                            displayName = user?.displayName ?: "Debug User"
                        ) { success, msg ->
                            if (success) {
                                selectedBristol.value = null
                                selectedColor.value = null
                                selectedMood.value = null
                                selectedVolume.value = null
                                selectedConditions.value = emptySet()
                                selectedImageUri.value = null
                                status.value = ctx.getString(successMessages.random())

                                onConfettiBurst(
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
                    pairingId?.let { RecapShare.shareWeeklyRecap(ctx, it) }
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
        }
    }
}
