package dev.crushingdigits.quotanoia.core.coroutines

import kotlinx.coroutines.CancellationException

/**
 * Catches all exceptions except [CancellationException] and wraps them into [Result].
 * Use instead of [runCatching] in suspend functions to allow proper cancellation propagation.
 */
inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> {
    @Suppress("TooGenericExceptionCaught")
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
