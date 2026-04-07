package dev.qwertyfinger.quotanoia.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.qwertyfinger.quotanoia.data.AccountState
import dev.qwertyfinger.quotanoia.ui.components.CircularUsageBar
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun AccountListScreen(
    states: List<AccountState>,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    // Tick every second for live countdowns
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = Instant.now()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("quotanoia") },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        },
    ) { padding ->
        if (states.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No accounts yet.\nTap + to add your Claude account.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(states, key = { it.account.id }) { state ->
                    AccountCard(
                        state = state,
                        now = now,
                        onEdit = { onEditAccount(state.account.id) },
                        onDelete = { onDeleteAccount(state.account.id) },
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun AccountCard(
    state: AccountState,
    now: Instant,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val borderColor = if (state.hasError) Color(0xFFE53935) else Color.Transparent
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (state.hasError) 2.dp else 0.dp, color = borderColor, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.account.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Usage bars
            if (state.usage != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CircularUsageBar(
                        utilization = state.usage.fiveHour.utilization,
                        label = "5h",
                        size = 90.dp,
                        strokeWidth = 10.dp,
                    )
                    CircularUsageBar(
                        utilization = state.usage.sevenDay.utilization,
                        label = "7d",
                        size = 90.dp,
                        strokeWidth = 10.dp,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Reset times (live countdown)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResetLabel("5h resets", state.usage.fiveHour.resetsAt, now)
                    ResetLabel("7d resets", state.usage.sevenDay.resetsAt, now)
                }
            } else if (!state.hasError) {
                Text(
                    "No data yet — will fetch shortly",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }

            // Error display
            if (state.hasError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error!!.displayMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (state.error.isTransient && state.nextRetryAt != null) {
                    val remaining = Duration.between(now, state.nextRetryAt)
                    val retryText = if (remaining.isNegative) {
                        "Retrying…"
                    } else {
                        "Next retry in ${formatDuration(remaining)}"
                    }
                    Text(
                        text = retryText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            // Stale indicator
            if (state.isStale && !state.hasError) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Data may be stale",
                    color = Color(0xFFFFCA28),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ResetLabel(prefix: String, resetsAt: Instant?, now: Instant) {
    if (resetsAt == null) return
    val duration = Duration.between(now, resetsAt)
    val text = if (duration.isNegative) "resetting…" else formatDuration(duration)

    Text(
        text = "$prefix: $text",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
}

private fun formatDuration(d: Duration): String {
    val totalSec = d.seconds
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${mins}m ${secs}s"
        hours > 0 -> "${hours}h ${mins}m ${secs}s"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}
