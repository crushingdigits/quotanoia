package dev.qwertyfinger.quotanoia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.qwertyfinger.quotanoia.data.AccountState
import dev.qwertyfinger.quotanoia.widget.UsageWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = QuotanoiaApp.instance.repository

    val states: StateFlow<List<AccountState>> = repo.states

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repo.refreshAll()
            _isRefreshing.value = false
            UsageWidgetReceiver.updateAll(getApplication())
        }
    }

    fun addAccount(
        name: String,
        sessionKey: String,
        orgId: String,
    ) {
        repo.addAccount(name, sessionKey, orgId)
        refresh()
    }

    fun updateAccount(
        id: String,
        name: String,
        sessionKey: String,
        orgId: String,
    ) {
        repo.updateAccount(id, name, sessionKey, orgId)
        refresh()
    }

    fun deleteAccount(id: String) {
        repo.removeAccount(id)
        viewModelScope.launch {
            UsageWidgetReceiver.updateAll(getApplication())
        }
    }
}
