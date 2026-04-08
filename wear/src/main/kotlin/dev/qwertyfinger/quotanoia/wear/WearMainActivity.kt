package dev.qwertyfinger.quotanoia.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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

            // Read cached data + poll for updates from MessageClient
            LaunchedEffect(Unit) {
                logConnectedNodes()
                entries = store.getAll()

                // Re-read store periodically in case DataLayerListenerService updates it
                while (true) {
                    delay(POLL_INTERVAL_MS)
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
                        fontSize = FONT_SIZE_NORMAL.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(CONTENT_PADDING.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Text(
                                "quotanoia",
                                color = Color(BRAND_COLOR),
                                fontSize = FONT_SIZE_NORMAL.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(SPACER_NORMAL.dp))
                        }
                        items(entries) { entry ->
                            WearAccountItem(entry.account.name, entry.usage)
                            Spacer(Modifier.height(SPACER_NORMAL.dp))
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
        private const val POLL_INTERVAL_MS = 3_000L
        private const val BG_COLOR = 0xFF0F0F1AL
        private const val MUTED_COLOR = 0xFFAAAAAAL
        private const val BRAND_COLOR = 0xFF7B9CFFL
        private const val FONT_SIZE_NORMAL = 14
        private const val CONTENT_PADDING = 12
        private const val SPACER_NORMAL = 8
    }
}

private const val ITEM_NAME_FONT_SIZE = 12
private const val ITEM_USAGE_FONT_SIZE = 11
private const val ITEM_ERROR_FONT_SIZE = 10
private const val ITEM_SPACER_DP = 4
private const val ERROR_COLOR = 0xFFE53935L
private const val TEXT_COLOR = 0xFFE0E0E0L
private const val SECONDARY_TEXT_COLOR = 0xFFB0B0B0L

@Composable
private fun WearAccountItem(
    name: String,
    usage: WearAccountStore.WearUsage,
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
            Text(
                text = "5h: ${usage.fiveHourPct.toInt()}%  7d: ${usage.sevenDayPct.toInt()}%",
                color = Color(SECONDARY_TEXT_COLOR),
                fontSize = ITEM_USAGE_FONT_SIZE.sp,
            )
        } else if (usage.hasError) {
            Text(
                text = usage.errorMsg ?: "Error",
                color = Color(ERROR_COLOR),
                fontSize = ITEM_ERROR_FONT_SIZE.sp,
            )
        }
    }
}
