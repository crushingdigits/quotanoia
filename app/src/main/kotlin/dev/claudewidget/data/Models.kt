package dev.claudewidget.data

import java.time.Instant
import java.util.UUID

data class ClaudeAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val token: String,
)

data class UsageWindow(
    val utilization: Float, // 0..100
    val resetsAt: Instant,
)

data class UsageData(
    val fiveHour: UsageWindow,
    val sevenDay: UsageWindow,
    val fetchedAt: Instant = Instant.now(),
)

sealed class AccountError {
    data object TokenExpired : AccountError()
    data object RateLimited : AccountError()
    data class ServerError(val code: Int) : AccountError()
    data class NetworkError(val message: String) : AccountError()
    data class ParseError(val message: String) : AccountError()

    val displayMessage: String
        get() = when (this) {
            is TokenExpired -> "Token expired — re-enter in settings"
            is RateLimited -> "Rate limited — backing off"
            is ServerError -> "Server error ($code)"
            is NetworkError -> "Network: $message"
            is ParseError -> "Bad response: $message"
        }

    val isTransient: Boolean
        get() = this !is TokenExpired
}

data class AccountState(
    val account: ClaudeAccount,
    val usage: UsageData? = null,
    val error: AccountError? = null,
    val backoffMs: Long = BASE_POLL_INTERVAL_MS,
) {
    val hasError: Boolean get() = error != null
    val isStale: Boolean
        get() = usage != null &&
            Instant.now().epochSecond - usage.fetchedAt.epochSecond > 600 // 10 min
}

const val BASE_POLL_INTERVAL_MS = 5 * 60 * 1000L   // 5 minutes
const val MAX_BACKOFF_MS = 30 * 60 * 1000L          // 30 minutes (unless server says more)
