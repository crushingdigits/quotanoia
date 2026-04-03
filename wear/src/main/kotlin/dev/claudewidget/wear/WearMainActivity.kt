package dev.claudewidget.wear

import android.os.Bundle
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

/**
 * Minimal watch app — shows cached usage data.
 * In a full version, this would support account management
 * and sync via Wearable DataClient from the phone app.
 */
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val store = WearApp.instance.store
            val accounts = remember { store.loadAccounts() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F1A)),
                contentAlignment = Alignment.Center,
            ) {
                if (accounts.isEmpty()) {
                    Text(
                        text = "Add accounts\non phone app",
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
                                "Claude Usage",
                                color = Color(0xFF7B9CFF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        items(accounts) { account ->
                            val usage = store.getCachedUsage(account.id)
                            WearAccountItem(account.name, usage)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
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
