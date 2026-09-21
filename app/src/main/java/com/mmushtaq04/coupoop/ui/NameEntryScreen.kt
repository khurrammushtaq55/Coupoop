package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.AuthManager
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme
import com.mmushtaq04.coupoop.ui.theme.LightCoralDark

/**
 * Shown once per account right after sign-in — for anonymous sign-in there's
 * no name at all yet, and even a Google-provided name isn't necessarily what
 * someone wants their partner to see, so this always lets them pick their own.
 * Pre-filled with whatever name the sign-in provider gave (blank for
 * anonymous), fully editable either way.
 */
@Composable
fun NameEntryScreen(initialName: String, onNameSet: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var status by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val nameRequiredMsg = stringResource(R.string.name_required)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👋", fontSize = 48.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.whats_your_name),
            style = MaterialTheme.typography.headlineSmall,
            color = LightCoralDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.name_helper),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                status = null
            },
            placeholder = { Text(stringResource(R.string.name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) {
                    status = nameRequiredMsg
                    return@Button
                }
                saving = true
                AuthManager.updateDisplayName(trimmed) { success, message ->
                    saving = false
                    if (success) {
                        onNameSet()
                    } else {
                        status = message
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(
                stringResource(R.string.continue_button),
                style = MaterialTheme.typography.labelLarge
            )
        }

        status?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                it,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NameEntryScreenPreview() {
    CoupoopTheme {
        NameEntryScreen(initialName = "", onNameSet = {})
    }
}
