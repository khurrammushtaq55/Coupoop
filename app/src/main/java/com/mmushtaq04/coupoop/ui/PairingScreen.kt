package com.mmushtaq04.coupoop.ui

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.PairingManager
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoral
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark
import com.mmushtaq04.coupoop.ui.theme.LightTeal

@Composable
fun PairingScreen(onPaired: () -> Unit = {}, onSettingsClick: () -> Unit = {}) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val user = if (isPreview) null else AuthManager.currentUser()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Invite, 1: Join
    val status = remember { mutableStateOf<String?>(null) }
    val joinCode = remember { mutableStateOf("") }
    val currentInvite = remember { mutableStateOf<String?>(null) }
    
    val creatingPairingMsg = stringResource(R.string.creating_pairing)
    val failedCreateMsg = stringResource(R.string.failed_create_pairing)
    val joiningPairingMsg = stringResource(R.string.joining_pairing)
    val joinedPairingMsg = stringResource(R.string.joined_pairing)
    val failedJoinMsg = stringResource(R.string.failed_join)

    Scaffold(
        topBar = {
            CoupoopTopBar(
                title = stringResource(R.string.app_name),
                onActionClick = null
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-50).dp)
                .alpha(0.1f)
                .background(LightCoral, CircleShape))
            Box(modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .alpha(0.1f)
                .background(LightTeal, CircleShape))

            // 2. Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user == null && !isPreview) {
                    Text(stringResource(R.string.sign_in_first))
                    return@Column
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Hero: 💩💕💩
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💩", fontSize = 40.sp)
                    Text("💕", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    Text("💩", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = stringResource(R.string.find_buddy),
                    style = MaterialTheme.typography.titleLarge,
                    color = LightCoralDark,
                    textAlign = TextAlign.Center
                )

                // Subtitle
                Text(
                    text = stringResource(R.string.invite_or_join),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Segmented Control
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        TabItem(
                            text = stringResource(R.string.tab_invite),
                            isSelected = selectedTab == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 0 }
                        )
                        TabItem(
                            text = stringResource(R.string.tab_join),
                            isSelected = selectedTab == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 1 }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (selectedTab == 0) {
                    InvitePanel(
                        currentInvite = currentInvite.value,
                        onCreateClick = {
                            status.value = creatingPairingMsg
                            PairingManager.createPairing(user?.uid.orEmpty()) { success, invite, message ->
                                if (success) {
                                    currentInvite.value = invite
                                    status.value = null
                                } else {
                                    status.value = message ?: failedCreateMsg
                                }
                            }
                        },
                        onContinueClick = onPaired
                    )
                } else {
                    JoinPanel(
                        joinCode = joinCode.value,
                        onCodeChange = { joinCode.value = it },
                        onJoinClick = {
                            status.value = joiningPairingMsg
                            PairingManager.acceptPairingByCode(joinCode.value, user?.uid.orEmpty()) { success, message ->
                                if (success) {
                                    status.value = joinedPairingMsg
                                    onPaired()
                                } else {
                                    status.value = message ?: failedJoinMsg
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                status.value?.let { 
                    Text(
                        it, 
                        textAlign = TextAlign.Center, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    ) 
                }
            }
        }
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InvitePanel(
    currentInvite: String?,
    onCreateClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    if (currentInvite == null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.invite_helper),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.create_invite_code), style = MaterialTheme.typography.labelLarge)
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InviteTicket(code = currentInvite)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            WaitingIndicator()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = LightTeal)
            ) {
                Text(stringResource(R.string.continue_to_coupoop), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun InviteTicket(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val shareText = stringResource(R.string.share_text, code)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LightChipBg)
            .drawBehind {
                drawRoundRect(
                    color = LightCoral,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    ),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.your_invite_code),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = code,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    letterSpacing = 4.sp
                ),
                color = LightCoralDark
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                OutlinedButton(
                    onClick = { clipboardManager.setText(AnnotatedString(code)) },
                    shape = CircleShape
                ) {
                    Text(stringResource(R.string.copy))
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { 
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    shape = CircleShape
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}

@Composable
fun WaitingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .alpha(alpha)
                .background(LightTeal, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            stringResource(R.string.waiting_for_partner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun JoinPanel(
    joinCode: String,
    onCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.join_helper),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = joinCode,
            onValueChange = { onCodeChange(it.trim().uppercase()) },
            placeholder = { Text(stringResource(R.string.enter_invite_code)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
        ) {
            Text(stringResource(R.string.join_by_code), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PairingScreenPreview() {
    CoupoopTheme {
        PairingScreen(onPaired = {}, onSettingsClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun InviteTicketPreview() {
    CoupoopTheme {
        InviteTicket(code = "ABC123")
    }
}
