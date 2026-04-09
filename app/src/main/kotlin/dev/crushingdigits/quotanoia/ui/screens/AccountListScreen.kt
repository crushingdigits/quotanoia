package dev.crushingdigits.quotanoia.ui.screens

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
import dev.crushingdigits.quotanoia.data.model.AccountState
import dev.crushingdigits.quotanoia.ui.components.CircularUsageBar
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val COUNTDOWN_TICK_MS = 1_000L
private const val FONT_SIZE_EMPTY_STATE = 16
private const val CONTENT_PADDING = 16
private const val CARD_SPACING = 12
private const val ERROR_COLOR = 0xFFE53935L
private const val STALE_COLOR = 0xFFFFCA28L
private const val CARD_CORNER_RADIUS = 16
private const val ERROR_BORDER_WIDTH = 2
private const val CARD_INNER_PADDING = 16
private const val FONT_SIZE_ACCOUNT_NAME = 18
private const val ICON_BUTTON_SIZE = 32
private const val ICON_SIZE = 18
private const val SPACER_MEDIUM = 12
private const val USAGE_BAR_SIZE = 90
private const val USAGE_BAR_STROKE = 10
private const val SPACER_SMALL = 8
private const val FONT_SIZE_SECONDARY = 13
private const val FONT_SIZE_SMALL = 11
private const val SPACER_XSMALL = 4

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "UnstableCollections")
@Composable
fun AccountListScreen(
    states: List<AccountState>,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    // Tick every second for live countdowns
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(COUNTDOWN_TICK_MS)
            now = Instant.now()
        }
    }

    Scaffold(
        modifier = modifier,
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
                    fontSize = FONT_SIZE_EMPTY_STATE.sp,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(CONTENT_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
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
    val borderColor = if (state.hasError) Color(ERROR_COLOR) else Color.Transparent
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (state.hasError) ERROR_BORDER_WIDTH.dp else 0.dp, color = borderColor, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(CARD_INNER_PADDING.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.account.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = FONT_SIZE_ACCOUNT_NAME.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(ICON_BUTTON_SIZE.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ICON_SIZE.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(ICON_BUTTON_SIZE.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(ICON_SIZE.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(SPACER_MEDIUM.dp))

            // Usage bars
            val usage = state.usage
            if (usage != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CircularUsageBar(
                        utilization = usage.fiveHour.utilization,
                        label = "5h",
                        size = USAGE_BAR_SIZE.dp,
                        strokeWidth = USAGE_BAR_STROKE.dp,
                    )
                    CircularUsageBar(
                        utilization = usage.sevenDay.utilization,
                        label = "7d",
                        size = USAGE_BAR_SIZE.dp,
                        strokeWidth = USAGE_BAR_STROKE.dp,
                    )
                }

                Spacer(Modifier.height(SPACER_SMALL.dp))

                // Reset times (live countdown)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResetLabel("5h resets", usage.fiveHour.resetsAt, now)
                    ResetLabel("7d resets", usage.sevenDay.resetsAt, now)
                }
            } else if (!state.hasError) {
                Text(
                    "No data yet — will fetch shortly",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = FONT_SIZE_SECONDARY.sp,
                )
            }

            // Error display
            if (state.hasError) {
                Spacer(Modifier.height(SPACER_SMALL.dp))
                Text(
                    text = state.error?.displayMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = FONT_SIZE_SECONDARY.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (state.error?.isTransient == true && state.nextRetryAt != null) {
                    val remaining = Duration.between(now, state.nextRetryAt)
                    val retryText = if (remaining.isNegative) {
                        "Retrying…"
                    } else {
                        "Next retry in ${formatDuration(remaining)}"
                    }
                    Text(
                        text = retryText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = FONT_SIZE_SMALL.sp,
                    )
                }
            }

            // Stale indicator
            if (state.isStale && !state.hasError) {
                Spacer(Modifier.height(SPACER_XSMALL.dp))
                Text(
                    text = "Data may be stale",
                    color = Color(STALE_COLOR),
                    fontSize = FONT_SIZE_SMALL.sp,
                )
            }
        }
    }
}

@Composable
private fun ResetLabel(
    prefix: String,
    resetsAt: Instant?,
    now: Instant,
) {
    if (resetsAt == null) return
    val duration = Duration.between(now, resetsAt)
    val text = if (duration.isNegative) "resetting…" else formatDuration(duration)

    Text(
        text = "$prefix: $text",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
}

private const val SECONDS_PER_DAY = 86400L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L

private fun formatDuration(d: Duration): String {
    val totalSec = d.seconds
    val days = totalSec / SECONDS_PER_DAY
    val hours = totalSec % SECONDS_PER_DAY / SECONDS_PER_HOUR
    val mins = totalSec % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val secs = totalSec % SECONDS_PER_MINUTE
    return when {
        days > 0 -> "${days}d ${hours}h ${mins}m ${secs}s"
        hours > 0 -> "${hours}h ${mins}m ${secs}s"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}
