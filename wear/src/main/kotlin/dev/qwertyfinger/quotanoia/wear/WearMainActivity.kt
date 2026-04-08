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

            // Read cached data + poll for updates from MessageClient
            LaunchedEffect(Unit) {
                logConnectedNodes()
                entries = store.getAll()

                // Re-read store periodically in case DataLayerListenerService updates it
                while (true) {
                    delay(3_000)
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
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Text(
                                "quotanoia",
                                color = Color(0xFF7B9CFF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        items(entries) { entry ->
                            WearAccountItem(entry.account.name, entry.usage)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
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
private fun WearAccountItem(name: String, usage: WearAccountStore.WearUsage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            color = if (usage.hasError) Color(0xFFE53935) else Color(0xFFE0E0E0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        if (usage.fiveHourPct >= 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "5h: ${usage.fiveHourPct.toInt()}%  7d: ${usage.sevenDayPct.toInt()}%",
                color = Color(0xFFB0B0B0),
                fontSize = 11.sp,
            )
        } else if (usage.hasError) {
            Text(
                text = usage.errorMsg ?: "Error",
                color = Color(0xFFE53935),
                fontSize = 10.sp,
            )
        }
    }
}
