package dev.crushingdigits.quotanoia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.crushingdigits.quotanoia.ui.screens.AccountFormData
import dev.crushingdigits.quotanoia.ui.screens.AccountListScreen
import dev.crushingdigits.quotanoia.ui.screens.AddAccountDialog
import dev.crushingdigits.quotanoia.ui.theme.QuotanoiaTheme

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
                    val form = requireNotNull(editFormData)
                    AddAccountDialog(
                        onDismiss = {
                            showDialog = false
                            editFormData = null
                        },
                        onSave = { name, sessionKey, orgId ->
                            if (form.id != null) {
                                vm.updateAccount(form.id, name, sessionKey, orgId)
                            } else {
                                vm.addAccount(name, sessionKey, orgId)
                            }
                            showDialog = false
                            editFormData = null
                        },
                        initial = form,
                    )
                }
            }
        }
    }
}
