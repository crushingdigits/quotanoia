package dev.qwertyfinger.quotanoia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qwertyfinger.quotanoia.ui.screens.AccountFormData
import dev.qwertyfinger.quotanoia.ui.screens.AccountListScreen
import dev.qwertyfinger.quotanoia.ui.screens.AddAccountDialog
import dev.qwertyfinger.quotanoia.ui.theme.QuotanoiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuotanoiaTheme {
                val vm: MainViewModel = viewModel()
                val states by vm.states.collectAsState()
                val isRefreshing by vm.isRefreshing.collectAsState()

                var showDialog by remember { mutableStateOf(false) }
                var editFormData by remember { mutableStateOf<AccountFormData?>(null) }

                AccountListScreen(
                    states = states,
                    onAddAccount = {
                        editFormData = AccountFormData()
                        showDialog = true
                    },
                    onEditAccount = { id ->
                        val state = states.find { it.account.id == id }
                        if (state != null) {
                            editFormData = AccountFormData(
                                id = state.account.id,
                                name = state.account.name,
                                sessionKey = state.account.sessionKey,
                                orgId = state.account.orgId,
                            )
                            showDialog = true
                        }
                    },
                    onDeleteAccount = { id -> vm.deleteAccount(id) },
                    onRefresh = { vm.refresh() },
                    isRefreshing = isRefreshing,
                )

                if (showDialog && editFormData != null) {
                    AddAccountDialog(
                        initial = editFormData!!,
                        onDismiss = {
                            showDialog = false
                            editFormData = null
                        },
                        onSave = { name, sessionKey, orgId ->
                            val form = editFormData!!
                            if (form.id != null) {
                                vm.updateAccount(form.id, name, sessionKey, orgId)
                            } else {
                                vm.addAccount(name, sessionKey, orgId)
                            }
                            showDialog = false
                            editFormData = null
                        },
                    )
                }
            }
        }
    }
}
