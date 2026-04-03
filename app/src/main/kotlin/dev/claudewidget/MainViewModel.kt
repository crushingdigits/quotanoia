package dev.claudewidget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.claudewidget.data.AccountState
import dev.claudewidget.widget.UsageWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ClaudeUsageApp.instance.repository

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

    fun addAccount(name: String, token: String) {
        repo.addAccount(name, token)
        refresh()
    }

    fun updateAccount(id: String, name: String, token: String) {
        repo.updateAccount(id, name, token)
        refresh()
    }

    fun deleteAccount(id: String) {
        repo.removeAccount(id)
        viewModelScope.launch {
            UsageWidgetReceiver.updateAll(getApplication())
        }
    }
}
