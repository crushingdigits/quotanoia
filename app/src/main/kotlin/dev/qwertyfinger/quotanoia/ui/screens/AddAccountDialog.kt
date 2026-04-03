package dev.qwertyfinger.quotanoia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AccountFormData(
    val id: String? = null,    // null for new account
    val name: String = "",
    val sessionKey: String = "",
    val orgId: String = "",
)

@Composable
fun AddAccountDialog(
    initial: AccountFormData = AccountFormData(),
    onDismiss: () -> Unit,
    onSave: (name: String, sessionKey: String, orgId: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var sessionKey by remember { mutableStateOf(initial.sessionKey) }
    var orgId by remember { mutableStateOf(initial.orgId) }

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
                    value = orgId,
                    onValueChange = { orgId = it },
                    label = { Text("Organization ID") },
                    placeholder = { Text("From lastActiveOrg cookie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = sessionKey,
                    onValueChange = { sessionKey = it },
                    label = { Text("Session key") },
                    placeholder = { Text("sk-ant-sid01-…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )

                Text(
                    text = "Browser DevTools → Application → Cookies → claude.ai\n" +
                        "• sessionKey → paste above\n" +
                        "• lastActiveOrg → Organization ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), sessionKey.trim(), orgId.trim()) },
                enabled = name.isNotBlank() && sessionKey.isNotBlank() && orgId.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
