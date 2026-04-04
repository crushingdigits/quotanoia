package dev.qwertyfinger.quotanoia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.qwertyfinger.quotanoia.notification.NotificationPrefsStore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationPrefsSection(
    accountId: String,
    prefs: NotificationPrefsStore,
    onRequestPermission: () -> Unit,
) {
    var alertsEnabled by remember { mutableStateOf(prefs.isAlertsEnabled(accountId)) }
    var thresholds5h by remember { mutableStateOf(prefs.getThresholds(accountId, "5h")) }
    var thresholds7d by remember { mutableStateOf(prefs.getThresholds(accountId, "7d")) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogWindow by remember { mutableStateOf("5h") }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Notifications",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Usage Alerts", fontSize = 14.sp)
            Switch(
                checked = alertsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestPermission()
                    prefs.setAlertsEnabled(accountId, enabled)
                    alertsEnabled = enabled
                    if (enabled) {
                        thresholds5h = prefs.getThresholds(accountId, "5h")
                        thresholds7d = prefs.getThresholds(accountId, "7d")
                    }
                },
            )
        }

        if (alertsEnabled) {
            Spacer(Modifier.height(8.dp))

            // 5-Hour Window
            Text(
                text = "5-Hour Window",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                thresholds5h.forEach { threshold ->
                    FilterChip(
                        selected = true,
                        onClick = {
                            thresholds5h = thresholds5h - threshold
                            prefs.setThresholds(accountId, "5h", thresholds5h)
                        },
                        label = { Text("${threshold}%", fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                }
                AssistChip(
                    onClick = {
                        addDialogWindow = "5h"
                        showAddDialog = true
                    },
                    label = { Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp)) },
                )
            }

            Spacer(Modifier.height(8.dp))

            // 7-Day Window
            Text(
                text = "7-Day Window",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                thresholds7d.forEach { threshold ->
                    FilterChip(
                        selected = true,
                        onClick = {
                            thresholds7d = thresholds7d - threshold
                            prefs.setThresholds(accountId, "7d", thresholds7d)
                        },
                        label = { Text("${threshold}%", fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                }
                AssistChip(
                    onClick = {
                        addDialogWindow = "7d"
                        showAddDialog = true
                    },
                    label = { Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddThresholdDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { value ->
                showAddDialog = false
                if (addDialogWindow == "5h") {
                    if (value !in thresholds5h) {
                        thresholds5h = (thresholds5h + value).sorted()
                        prefs.setThresholds(accountId, "5h", thresholds5h)
                    }
                } else {
                    if (value !in thresholds7d) {
                        thresholds7d = (thresholds7d + value).sorted()
                        prefs.setThresholds(accountId, "7d", thresholds7d)
                    }
                }
            },
        )
    }
}

@Composable
private fun AddThresholdDialog(
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val value = text.toIntOrNull()
    val isValid = value != null && value in 1..100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Threshold") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                label = { Text("Threshold (1-100)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onAdd(value!!) },
                enabled = isValid,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
