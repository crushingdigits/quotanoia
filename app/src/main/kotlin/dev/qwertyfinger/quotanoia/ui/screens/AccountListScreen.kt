package dev.qwertyfinger.quotanoia.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.qwertyfinger.quotanoia.data.AccountState
import dev.qwertyfinger.quotanoia.notification.NotificationPrefsStore
import dev.qwertyfinger.quotanoia.ui.components.CircularUsageBar
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    states: List<AccountState>,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onShowHistory: () -> Unit = {},
    notificationPrefs: NotificationPrefsStore? = null,
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
                    IconButton(onClick = onShowHistory) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "History")
                    }
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
            val hasAnySuccessfulFetch = states.any { it.usage != null }
            var hintDismissed by remember {
                mutableStateOf(notificationPrefs?.isHintCardDismissed() ?: true)
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hintDismissed && hasAnySuccessfulFetch && notificationPrefs != null) {
                    item(key = "hint_card") {
                        HintCard(
                            onDismiss = {
                                notificationPrefs.dismissHintCard()
                                hintDismissed = true
                            },
                            onNavigate = {
                                val first = states.firstOrNull()
                                if (first != null) onEditAccount(first.account.id)
                            },
                        )
                    }
                }
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

@Composable
private fun HintCard(
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onNavigate,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    "Get alerted when usage is high \u2192 Set up notifications",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
