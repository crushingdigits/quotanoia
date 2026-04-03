package dev.qwertyfinger.quotanoia.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import dev.qwertyfinger.quotanoia.sync.WearSyncManager
import java.time.Instant
import kotlin.math.min

class UsageRepository(
    private val store: AccountStore,
    private val api: ApiClient = ApiClient(),
    private val wearSync: WearSyncManager? = null,
) {

    private val _states = MutableStateFlow<List<AccountState>>(emptyList())
    val states: StateFlow<List<AccountState>> = _states.asStateFlow()

    /** Load accounts from disk and populate states with cached data. */
    fun initialize() {
        val accounts = store.loadAccounts()
        _states.value = accounts.map { account ->
            AccountState(
                account = account,
                usage = store.getCachedUsage(account.id),
                error = store.getCachedError(account.id)?.let {
                    AccountError.NetworkError(it) // generic for cached errors
                },
                backoffMs = store.getBackoff(account.id),
            )
        }
    }

    /** Fetch usage for all accounts. Returns updated states. */
    suspend fun refreshAll(): List<AccountState> = withContext(Dispatchers.IO) {
        val current = _states.value.toMutableList()
        val accounts = store.loadAccounts()

        for ((index, account) in accounts.withIndex()) {
            val state = current.getOrNull(index) ?: AccountState(account = account)
            val result = api.fetchUsage(account.token)

            val newState = result.fold(
                onSuccess = { usage ->
                    store.cacheUsage(account.id, usage)
                    state.copy(
                        usage = usage,
                        error = null,
                        backoffMs = BASE_POLL_INTERVAL_MS,
                        nextRetryAt = null,
                    )
                },
                onFailure = { throwable ->
                    val apiEx = throwable as? ApiClient.ApiException
                    val error = apiEx?.accountError
                        ?: AccountError.NetworkError(throwable.message ?: "Unknown")
                    val serverRetry = apiEx?.retryAfterMs
                    val newBackoff = if (error.isTransient) {
                        val exponential = min(state.backoffMs * 2, MAX_BACKOFF_MS)
                        if (serverRetry != null) maxOf(exponential, serverRetry) else exponential
                    } else {
                        state.backoffMs
                    }
                    store.cacheError(account.id, error, newBackoff)
                    state.copy(
                        error = error,
                        backoffMs = newBackoff,
                        nextRetryAt = Instant.now().plusMillis(newBackoff),
                    )
                }
            )
            if (index < current.size) current[index] = newState else current.add(newState)
        }

        // Remove states for deleted accounts
        val accountIds = accounts.map { it.id }.toSet()
        current.removeAll { it.account.id !in accountIds }

        _states.value = current
        wearSync?.syncStates(current)
        current
    }

    /** Fetch for a single account. */
    suspend fun refreshAccount(accountId: String): AccountState? = withContext(Dispatchers.IO) {
        val current = _states.value.toMutableList()
        val idx = current.indexOfFirst { it.account.id == accountId }
        if (idx < 0) return@withContext null

        val state = current[idx]
        val result = api.fetchUsage(state.account.token)

        val newState = result.fold(
            onSuccess = { usage ->
                store.cacheUsage(accountId, usage)
                state.copy(usage = usage, error = null, backoffMs = BASE_POLL_INTERVAL_MS, nextRetryAt = null)
            },
            onFailure = { throwable ->
                val apiEx = throwable as? ApiClient.ApiException
                val error = apiEx?.accountError
                    ?: AccountError.NetworkError(throwable.message ?: "Unknown")
                val serverRetry = apiEx?.retryAfterMs
                val newBackoff = if (error.isTransient) {
                    val exponential = min(state.backoffMs * 2, MAX_BACKOFF_MS)
                    if (serverRetry != null) maxOf(exponential, serverRetry) else exponential
                } else {
                    state.backoffMs
                }
                store.cacheError(accountId, error, newBackoff)
                state.copy(
                    error = error,
                    backoffMs = newBackoff,
                    nextRetryAt = Instant.now().plusMillis(newBackoff),
                )
            }
        )

        current[idx] = newState
        _states.value = current
        wearSync?.syncStates(current)
        newState
    }

    /** Minimum delay until next poll (smallest backoff across all accounts). */
    fun nextPollDelayMs(): Long =
        _states.value.minOfOrNull { it.backoffMs } ?: BASE_POLL_INTERVAL_MS

    // ── Account management (delegates to store, refreshes states) ──

    fun addAccount(name: String, token: String): ClaudeAccount {
        val account = ClaudeAccount(name = name, token = token)
        store.addAccount(account)
        initialize()
        return account
    }

    fun removeAccount(id: String) {
        store.removeAccount(id)
        initialize()
    }

    fun updateAccount(id: String, name: String, token: String) {
        store.updateAccount(ClaudeAccount(id = id, name = name, token = token))
        initialize()
    }
}
