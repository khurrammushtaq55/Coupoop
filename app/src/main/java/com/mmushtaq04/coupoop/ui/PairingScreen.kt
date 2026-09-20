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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.PairingManager
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoral
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark
import com.mmushtaq04.coupoop.ui.theme.LightTeal

@Composable
fun PairingScreen(onPaired: () -> Unit = {}, onSettingsClick: () -> Unit = {}) {
    val user = AuthManager.currentUser()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Invite, 1: Join
    val status = remember { mutableStateOf<String?>(null) }
    val joinCode = remember { mutableStateOf("") }
    val currentInvite = remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Decorative Blobs
        Box(modifier = Modifier
            .size(150.dp)
            .align(Alignment.TopStart)
            .offset(x = (-50).dp, y = (-50).dp)
            .alpha(0.1f)
            .background(LightCoral, CircleShape))
        Box(modifier = Modifier
            .size(150.dp)
            .align(Alignment.TopEnd)
            .offset(x = 50.dp, y = (-50).dp)
            .alpha(0.1f)
            .background(LightTeal, CircleShape))

        // Settings Button
        Surface(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(12.dp),
            color = LightChipBg,
            modifier = Modifier
                .padding(20.dp)
                .size(38.dp)
                .align(Alignment.TopEnd)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⚙", fontSize = 16.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user == null) {
                Text("Please sign in first")
                return@Column
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. Hero: 💩💕💩
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💩", fontSize = 40.sp)
                Text("💕", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Text("💩", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Title
            Text(
                text = "Find your poop buddy",
                style = MaterialTheme.typography.titleLarge,
                color = LightCoralDark,
                textAlign = TextAlign.Center
            )

            // 4. Subtitle
            Text(
                text = "Invite your partner or join with their code",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Segmented Control
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    TabItem(
                        text = "💌 Invite",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabItem(
                        text = "🔑 Join",
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
                        status.value = "Creating pairing..."
                        PairingManager.createPairing(user.uid) { success, invite, message ->
                            if (success) {
                                currentInvite.value = invite
                                status.value = null
                            } else {
                                status.value = message ?: "Failed to create pairing"
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
                        status.value = "Joining pairing..."
                        PairingManager.acceptPairingByCode(joinCode.value, user.uid) { success, message ->
                            if (success) {
                                status.value = "Joined pairing!"
                                onPaired()
                            } else {
                                status.value = message ?: "Failed to join"
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
                "Generate a one-tap invite and send it to your partner.",
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
                Text("Create invite code", style = MaterialTheme.typography.labelLarge)
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
                Text("Continue to Coupoop", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun InviteTicket(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
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
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
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
                "YOUR INVITE CODE",
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
                    Text("Copy")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { 
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Join me on Coupoop! Use my code: $code")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    shape = CircleShape
                ) {
                    Text("Share")
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
            "Waiting for your partner to join…",
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
            "Got a code from your partner? Enter it below.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = joinCode,
            onValueChange = { onCodeChange(it.trim().uppercase()) },
            placeholder = { Text("Enter invite code") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
        ) {
            Text("Join by code", style = MaterialTheme.typography.labelLarge)
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
