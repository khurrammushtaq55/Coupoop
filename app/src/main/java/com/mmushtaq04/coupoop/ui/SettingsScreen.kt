package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.AccountManager
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.BuildConfig
import com.mmushtaq04.coupoop.NotificationPrefsManager
import com.mmushtaq04.coupoop.PairingManager
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.Danger
import com.mmushtaq04.coupoop.ui.theme.LightChipBg
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark

@Composable
fun SettingsScreen(
    pairingId: String?,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onLeftPairing: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    var status by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }

    val leavingPairingMsg = stringResource(R.string.leaving_pairing)
    val deletingAccountMsg = stringResource(R.string.deleting_account)
    val failMuteMsg = stringResource(R.string.failed_update_mute)
    val failLeaveMsg = stringResource(R.string.failed_leave_pairing)
    val failDeleteMsg = stringResource(R.string.failed_delete_account)

    val isPreview = LocalInspectionMode.current

    LaunchedEffect(pairingId) {
        if (isPreview) return@LaunchedEffect
        val user = AuthManager.currentUser()
        if (user != null && pairingId != null) {
            NotificationPrefsManager.isPairingMuted(user.uid, pairingId) { isMuted ->
                muted = isMuted
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = LightCoralDark
                )
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = LightChipBg,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(44.dp) // Slightly larger than header
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", fontSize = 20.sp, color = LightCoralDark)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.labelLarge)
            }

            if (pairingId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val user = AuthManager.currentUser() ?: return@OutlinedButton
                        val newValue = !muted
                        NotificationPrefsManager.setPairingMuted(
                            user.uid,
                            pairingId,
                            newValue
                        ) { success, message ->
                            if (success) {
                                muted = newValue
                            } else {
                                status = message ?: failMuteMsg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text(
                        if (muted) stringResource(R.string.unmute_notifications) else stringResource(
                            R.string.mute_notifications
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    stringResource(R.string.applies_to_pairing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val user = AuthManager.currentUser() ?: return@OutlinedButton
                        working = true
                        status = leavingPairingMsg
                        PairingManager.leavePairing(pairingId, user.uid) { success, message ->
                            working = false
                            if (success) {
                                onLeftPairing()
                            } else {
                                status = message ?: failLeaveMsg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text(
                        stringResource(R.string.leave_pairing),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Danger)
            ) {
                Text(
                    stringResource(R.string.delete_account),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    stringResource(R.string.debug_options),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* Debug skip login logic here */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text(stringResource(R.string.debug_skip_auth), style = MaterialTheme.typography.labelLarge)
                }
            }

            status?.let {
                Text(
                    it,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { if (!working) showDeleteConfirm = false },
                title = { Text(stringResource(R.string.delete_account_title)) },
                text = { Text(stringResource(R.string.delete_account_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        working = true
                        status = deletingAccountMsg
                        AccountManager.deleteAccount { success, message ->
                            working = false
                            showDeleteConfirm = false
                            if (success) {
                                onAccountDeleted()
                            } else {
                                status = message ?: failDeleteMsg
                            }
                        }
                    }) {
                        Text(stringResource(R.string.delete), color = Danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    CoupoopTheme {
        SettingsScreen(
            pairingId = "pair123",
            onBack = {},
            onSignOut = {},
            onLeftPairing = {},
            onAccountDeleted = {}
        )
    }
}
