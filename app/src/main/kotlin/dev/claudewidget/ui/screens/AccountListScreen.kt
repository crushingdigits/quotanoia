package dev.claudewidget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.claudewidget.data.AccountState
import dev.claudewidget.ui.components.CircularUsageBar
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AccountListScreen(
    states: List<AccountState>,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claude Usage") },
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

                // Reset times
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResetLabel("5h resets", state.usage.fiveHour.resetsAt)
                    ResetLabel("7d resets", state.usage.sevenDay.resetsAt)
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
                if (state.error.isTransient) {
                    Text(
                        text = "Next retry in ${state.backoffMs / 1000}s",
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
private fun ResetLabel(prefix: String, resetsAt: Instant) {
    val now = Instant.now()
    val duration = Duration.between(now, resetsAt)
    val text = if (duration.isNegative) {
        "resetting…"
    } else {
        val hours = duration.toHours()
        val mins = duration.toMinutes() % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    Text(
        text = "$prefix: $text",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
}
