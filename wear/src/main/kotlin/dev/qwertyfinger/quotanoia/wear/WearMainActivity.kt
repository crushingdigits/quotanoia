@file:Suppress("MagicNumber", "FunctionSignature", "UnnecessaryParentheses")

package dev.qwertyfinger.quotanoia.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.core.coroutines.runCatchingCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val store = WearApp.instance.store
            var entries by remember { mutableStateOf(store.getAll()) }

            // Tick for live reset countdowns + pick up new data
            var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                logConnectedNodes()
                while (true) {
                    delay(POLL_INTERVAL_MS)
                    nowMs = System.currentTimeMillis()
                    entries = store.getAll()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(BG_COLOR)),
                contentAlignment = Alignment.Center,
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = "Open phone app\nto add accounts",
                        color = Color(MUTED_COLOR),
                        fontSize = FONT_SIZE_LARGE.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(CONTENT_PADDING_LARGE.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Text(
                                "quotanoia",
                                color = Color(BRAND_COLOR),
                                fontSize = FONT_SIZE_LARGE.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(SPACER_LARGE.dp))
                        }
                        items(entries) { entry ->
                            WearAccountItem(entry.account.name, entry.usage, nowMs)
                            Spacer(Modifier.height(SPACER_LARGE.dp))
                        }
                    }
                }
            }
        }
    }

    private suspend fun logConnectedNodes() {
        runCatchingCancellable {
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()
            Log.d(TAG, "Connected nodes: ${nodes.size}")
            nodes.forEach { node ->
                Log.d(TAG, "  Node: ${node.displayName} (id=${node.id}, nearby=${node.isNearby})")
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to get connected nodes", e)
        }
    }

    companion object {
        private const val TAG = "WearMain"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val BG_COLOR = 0xFF0F0F1AL
        private const val MUTED_COLOR = 0xFFAAAAAAL
        private const val BRAND_COLOR = 0xFF7B9CFFL
        private const val FONT_SIZE_LARGE = 16
        private const val CONTENT_PADDING_LARGE = 16
        private const val SPACER_LARGE = 10
    }
}

private const val ITEM_NAME_FONT_SIZE = 14
private const val ITEM_USAGE_FONT_SIZE = 14
private const val ITEM_ERROR_FONT_SIZE = 12
private const val ITEM_SPACER_DP = 4
private const val ITEM_RESET_SPACER_DP = 2
private const val ITEM_RESET_SPACER_WIDTH_DP = 12
private const val ERROR_COLOR = 0xFFE53935L
private const val TEXT_COLOR = 0xFFE0E0E0L
private const val DIM_COLOR = 0xFF888888L
private const val RESET_FONT_SIZE = 11

@Composable
private fun WearAccountItem(
    name: String,
    usage: WearAccountStore.WearUsage,
    nowMs: Long,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            color = if (usage.hasError) Color(ERROR_COLOR) else Color(TEXT_COLOR),
            fontSize = ITEM_NAME_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
        )
        if (usage.fiveHourPct >= 0) {
            Spacer(Modifier.height(ITEM_SPACER_DP.dp))
            Row {
                ColoredPct("5h:", usage.fiveHourPct)
                Spacer(Modifier.width(ITEM_RESET_SPACER_WIDTH_DP.dp))
                ColoredPct("7d:", usage.sevenDayPct)
            }
            Spacer(Modifier.height(ITEM_RESET_SPACER_DP.dp))
            Row {
                ResetTime(usage.fiveHourResetsAtMs, nowMs)
                Spacer(Modifier.width(ITEM_RESET_SPACER_WIDTH_DP.dp))
                ResetTime(usage.sevenDayResetsAtMs, nowMs)
            }
        } else if (usage.hasError) {
            Text(
                text = usage.errorMsg ?: "Error",
                color = Color(ERROR_COLOR),
                fontSize = ITEM_ERROR_FONT_SIZE.sp,
            )
        }
    }
}

@Composable
private fun ColoredPct(label: String, pct: Float) {
    Text(
        text = "$label${pct.toInt()}%",
        color = pctToColor(pct),
        fontSize = ITEM_USAGE_FONT_SIZE.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ResetTime(resetMs: Long, nowMs: Long) {
    val text = if (resetMs <= 0) {
        "?"
    } else {
        val diff = resetMs - nowMs
        if (diff <= 0) "now" else formatDuration(diff)
    }
    Text(
        text = text,
        color = Color(DIM_COLOR),
        fontSize = RESET_FONT_SIZE.sp,
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
