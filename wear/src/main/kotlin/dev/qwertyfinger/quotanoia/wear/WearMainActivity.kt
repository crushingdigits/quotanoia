package dev.qwertyfinger.quotanoia.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val store = WearApp.instance.store
            var entries by remember { mutableStateOf(store.getAll()) }

            // Tick for live reset countdowns + pick up new data
            var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                logConnectedNodes()
                while (true) {
                    delay(1_000)
                    nowMs = System.currentTimeMillis()
                    entries = store.getAll()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F1A)),
                contentAlignment = Alignment.Center,
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = "Open phone app\nto add accounts",
                        color = Color(0xFFAAAAAA),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Text(
                                "quotanoia",
                                color = Color(0xFF7B9CFF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        items(entries) { entry ->
                            WearAccountItem(entry.account.name, entry.usage, nowMs)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }

    private suspend fun logConnectedNodes() {
        try {
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            Log.d(TAG, "Connected nodes: ${nodes.size}")
            nodes.forEach { node ->
                Log.d(TAG, "  Node: ${node.displayName} (id=${node.id}, nearby=${node.isNearby})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connected nodes", e)
        }
    }

    companion object {
        private const val TAG = "WearMain"
    }
}

@Composable
private fun WearAccountItem(name: String, usage: WearAccountStore.WearUsage, nowMs: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            color = if (usage.hasError) Color(0xFFE53935) else Color(0xFFE0E0E0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        if (usage.fiveHourPct >= 0) {
            Spacer(Modifier.height(4.dp))
            Row {
                ColoredPct("5h:", usage.fiveHourPct)
                Spacer(Modifier.width(12.dp))
                ColoredPct("7d:", usage.sevenDayPct)
            }
            Spacer(Modifier.height(2.dp))
            Row {
                ResetTime("↻", usage.fiveHourResetsAtMs, nowMs)
                Spacer(Modifier.width(12.dp))
                ResetTime("↻", usage.sevenDayResetsAtMs, nowMs)
            }
        } else if (usage.hasError) {
            Text(
                text = usage.errorMsg ?: "Error",
                color = Color(0xFFE53935),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ColoredPct(label: String, pct: Float) {
    Text(
        text = "$label${pct.toInt()}%",
        color = pctToColor(pct),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ResetTime(prefix: String, resetMs: Long, nowMs: Long) {
    val text = if (resetMs <= 0) {
        "$prefix?"
    } else {
        val diff = resetMs - nowMs
        if (diff <= 0) "${prefix}now" else "$prefix${formatDuration(diff)}"
    }
    Text(
        text = text,
        color = Color(0xFF888888),
        fontSize = 11.sp,
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${mins}m"
        hours > 0 -> "${hours}h ${mins}m ${secs}s"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}

private fun pctToColor(pct: Float): Color {
    val clamped = pct.coerceIn(0f, 100f)
    return when {
        clamped <= 40f -> Color(0xFF4CAF50)
        clamped <= 70f -> Color(0xFFFFCA28)
        clamped <= 85f -> Color(0xFFFF9800)
        else -> Color(0xFFE53935)
    }
}
