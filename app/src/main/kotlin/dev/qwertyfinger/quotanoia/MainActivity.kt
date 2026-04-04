package dev.qwertyfinger.quotanoia

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.qwertyfinger.quotanoia.notification.NotificationPermissionHelper
import dev.qwertyfinger.quotanoia.ui.screens.AccountFormData
import dev.qwertyfinger.quotanoia.ui.screens.AccountListScreen
import dev.qwertyfinger.quotanoia.ui.screens.AddAccountDialog
import dev.qwertyfinger.quotanoia.ui.screens.HistoryScreen
import dev.qwertyfinger.quotanoia.ui.theme.QuotanoiaTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op, toggle stays */ }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationPermissionHelper.hasPermission(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuotanoiaTheme {
                val vm: MainViewModel = viewModel()
                val states by vm.states.collectAsState()
                val isRefreshing by vm.isRefreshing.collectAsState()
                val navController = rememberNavController()

                var showDialog by remember { mutableStateOf(false) }
                var editFormData by remember { mutableStateOf<AccountFormData?>(null) }

                NavHost(navController = navController, startDestination = "accounts") {
                    composable("accounts") {
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
                            onShowHistory = { navController.navigate("history") },
                            notificationPrefs = QuotanoiaApp.instance.notificationPrefs,
                        )
                    }

                    composable(
                        route = "history?accountId={accountId}",
                        arguments = listOf(
                            navArgument("accountId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { backStackEntry ->
                        val accountId = backStackEntry.arguments?.getString("accountId")
                        HistoryScreen(
                            accountId = accountId,
                            accountStates = states,
                            historyDao = QuotanoiaApp.instance.historyDao,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable("history") {
                        HistoryScreen(
                            accountId = null,
                            accountStates = states,
                            historyDao = QuotanoiaApp.instance.historyDao,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

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
                        notificationPrefs = QuotanoiaApp.instance.notificationPrefs,
                        onRequestNotificationPermission = { requestNotificationPermission() },
                    )
                }
            }
        }
    }
}
