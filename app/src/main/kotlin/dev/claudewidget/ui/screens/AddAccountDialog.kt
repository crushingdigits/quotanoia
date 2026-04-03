package dev.claudewidget.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AccountFormData(
    val id: String? = null,    // null for new account
    val name: String = "",
    val token: String = "",
)

@Composable
fun AddAccountDialog(
    initial: AccountFormData = AccountFormData(),
    onDismiss: () -> Unit,
    onSave: (name: String, token: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var token by remember { mutableStateOf(initial.token) }

    val isEdit = initial.id != null
    val title = if (isEdit) "Edit Account" else "Add Account"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account name") },
                    placeholder = { Text("e.g. Personal Pro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("OAuth token") },
                    placeholder = { Text("Paste from ~/.claude/.credentials.json") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )

                Text(
                    text = "Find your token in ~/.claude/.credentials.json " +
                        "→ claudeAiOauth → accessToken",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), token.trim()) },
                enabled = name.isNotBlank() && token.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
