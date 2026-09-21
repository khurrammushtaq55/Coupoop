package com.mmushtaq04.coupoop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.R
import com.mmushtaq04.coupoop.ui.theme.CoupoopTheme

@Composable
fun ActivityScreen(
    logs: List<Map<String, Any>>,
    userId: String?,
    pairingId: String?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.recent_activity), 
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (logs.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(items = logs) { log ->
                LogCard(log = log, currentUserId = userId, pairingId = pairingId)
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
