package dev.qwertyfinger.quotanoia.data

import dev.qwertyfinger.quotanoia.data.model.AccountError
import dev.qwertyfinger.quotanoia.data.model.AccountState
import dev.qwertyfinger.quotanoia.data.model.BASE_POLL_INTERVAL_MS
import dev.qwertyfinger.quotanoia.data.model.ClaudeAccount
import dev.qwertyfinger.quotanoia.data.model.MAX_BACKOFF_MS
import dev.qwertyfinger.quotanoia.data.model.UsageData
import dev.qwertyfinger.quotanoia.data.model.UsageWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ModelsTest {

    // ── AccountError.displayMessage ────────────────────────────────

    @Test
    fun `TokenExpired displayMessage mentions session expired`() {
        val msg = AccountError.TokenExpired.displayMessage
        assertTrue(msg.contains("expired", ignoreCase = true))
    }

    @Test
    fun `RateLimited displayMessage mentions rate limited`() {
        val msg = AccountError.RateLimited.displayMessage
        assertTrue(msg.contains("rate limited", ignoreCase = true))
    }

    @Test
    fun `ServerError displayMessage includes HTTP status code`() {
        val msg = AccountError.ServerError(503).displayMessage
        assertTrue(msg.contains("503"))
    }

    @Test
    fun `NetworkError displayMessage includes the error message`() {
        val msg = AccountError.NetworkError("timeout").displayMessage
        assertTrue(msg.contains("timeout"))
    }

    @Test
    fun `ParseError displayMessage includes the error message`() {
        val msg = AccountError.ParseError("unexpected token").displayMessage
        assertTrue(msg.contains("unexpected token"))
    }

    // ── AccountError.isTransient ───────────────────────────────────

    @Test
    fun `TokenExpired is not transient`() {
        assertFalse(AccountError.TokenExpired.isTransient)
    }

    @Test
    fun `RateLimited is transient`() {
        assertTrue(AccountError.RateLimited.isTransient)
    }

    @Test
    fun `ServerError is transient`() {
        assertTrue(AccountError.ServerError(500).isTransient)
    }

    @Test
    fun `NetworkError is transient`() {
        assertTrue(AccountError.NetworkError("no route").isTransient)
    }

    @Test
    fun `ParseError is transient`() {
        assertTrue(AccountError.ParseError("bad json").isTransient)
    }

    // ── AccountState.isStale ───────────────────────────────────────

    @Test
    fun `isStale returns false when usage is null`() {
        val state = AccountState(
            account = testAccount(),
            usage = null,
        )
        assertFalse(state.isStale)
    }

    @Test
    fun `isStale returns false for fresh data`() {
        val state = AccountState(
            account = testAccount(),
            usage = testUsageData(fetchedAt = Instant.now()),
        )
        assertFalse(state.isStale)
    }

    @Test
    fun `isStale returns true when data is older than 10 minutes`() {
        val elevenMinutesAgo = Instant.now().minusSeconds(660)
        val state = AccountState(
            account = testAccount(),
            usage = testUsageData(fetchedAt = elevenMinutesAgo),
        )
        assertTrue(state.isStale)
    }

    @Test
    fun `isStale returns false when data is exactly 10 minutes old`() {
        val tenMinutesAgo = Instant.now().minusSeconds(600)
        val state = AccountState(
            account = testAccount(),
            usage = testUsageData(fetchedAt = tenMinutesAgo),
        )
        // 600 seconds is not > 600, so should be false
        assertFalse(state.isStale)
    }

    // ── AccountState.hasError ──────────────────────────────────────

    @Test
    fun `hasError returns false when no error`() {
        val state = AccountState(account = testAccount())
        assertFalse(state.hasError)
    }

    @Test
    fun `hasError returns true when error is present`() {
        val state = AccountState(
            account = testAccount(),
            error = AccountError.TokenExpired,
        )
        assertTrue(state.hasError)
    }

    // ── Constants ──────────────────────────────────────────────────

    @Test
    fun `BASE_POLL_INTERVAL_MS is 5 minutes`() {
        assertEquals(5 * 60 * 1000L, BASE_POLL_INTERVAL_MS)
    }

    @Test
    fun `MAX_BACKOFF_MS is 30 minutes`() {
        assertEquals(30 * 60 * 1000L, MAX_BACKOFF_MS)
    }

    // ── Helpers ────────────────────────────────────────────────────

    private fun testAccount() = ClaudeAccount(
        id = "test-id",
        name = "Test",
        sessionKey = "sk-test",
        orgId = "org-test",
    )

    private fun testUsageData(fetchedAt: Instant = Instant.now()) = UsageData(
        fiveHour = UsageWindow(utilization = 50f, resetsAt = Instant.now().plusSeconds(3600)),
        sevenDay = UsageWindow(utilization = 30f, resetsAt = Instant.now().plusSeconds(86400)),
        fetchedAt = fetchedAt,
    )
}
