package dev.qwertyfinger.quotanoia.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.qwertyfinger.quotanoia.data.AccountError
import dev.qwertyfinger.quotanoia.data.AccountState
import dev.qwertyfinger.quotanoia.data.ClaudeAccount
import dev.qwertyfinger.quotanoia.data.UsageData
import dev.qwertyfinger.quotanoia.data.UsageWindow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class NotificationEvaluatorTest {

    private lateinit var prefs: NotificationPrefsStore
    private lateinit var evaluator: NotificationEvaluator

    private val account = ClaudeAccount(
        id = "acc-1",
        name = "Test Account",
        sessionKey = "sk-test",
        orgId = "org-1",
    )

    private val resetsAt = Instant.parse("2026-01-01T05:00:00Z")

    private fun makeUsage(fiveHourUtil: Float, sevenDayUtil: Float) = UsageData(
        fiveHour = UsageWindow(fiveHourUtil, resetsAt),
        sevenDay = UsageWindow(sevenDayUtil, resetsAt),
    )

    private fun makeState(
        usage: UsageData? = null,
        error: AccountError? = null,
    ) = AccountState(account = account, usage = usage, error = error)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(NotificationPrefsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        prefs = NotificationPrefsStore(context)
        evaluator = NotificationEvaluator(prefs)
    }

    @Test
    fun `alerts disabled produces no notifications`() {
        val state = makeState(usage = makeUsage(80f, 80f))
        val result = evaluator.evaluate(listOf(state))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `threshold crossing produces notification`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(usage = makeUsage(55f, 30f))

        val result = evaluator.evaluate(listOf(state))

        // 5h: 55 >= 50 → fire. 7d: 30 < 50 → no fire
        val fiveHourNotifs = result.filter { it.title.contains("5-Hour") }
        assertEquals(1, fiveHourNotifs.size)
        assertTrue(fiveHourNotifs[0].title.contains("55%"))
    }

    @Test
    fun `already seen threshold does not produce notification`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(usage = makeUsage(55f, 30f))

        // First evaluation
        evaluator.evaluate(listOf(state))
        // Second evaluation — same state
        val result = evaluator.evaluate(listOf(state))

        val fiveHourNotifs = result.filter { it.title.contains("5-Hour") }
        assertTrue(fiveHourNotifs.isEmpty())
    }

    @Test
    fun `reset cycle change clears seen flag and allows re-fire`() {
        prefs.setAlertsEnabled(account.id, true)

        val state1 = makeState(usage = makeUsage(55f, 30f))
        evaluator.evaluate(listOf(state1))

        // New reset cycle
        val newResetsAt = Instant.parse("2026-01-01T10:00:00Z")
        val state2 = AccountState(
            account = account,
            usage = UsageData(
                fiveHour = UsageWindow(55f, newResetsAt),
                sevenDay = UsageWindow(30f, newResetsAt),
            ),
        )

        val result = evaluator.evaluate(listOf(state2))
        val fiveHourNotifs = result.filter { it.title.contains("5-Hour") }
        assertEquals(1, fiveHourNotifs.size)
    }

    @Test
    fun `session expired produces account_issues notification`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(error = AccountError.TokenExpired)

        val result = evaluator.evaluate(listOf(state))

        assertEquals(1, result.size)
        assertEquals(NotificationChannelManager.CHANNEL_ACCOUNT_ISSUES, result[0].channelId)
        assertTrue(result[0].title.contains("Session Expired"))
    }

    @Test
    fun `session expired dedup - already notified is skipped`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(error = AccountError.TokenExpired)

        // First evaluation
        val first = evaluator.evaluate(listOf(state))
        assertEquals(1, first.size)

        // Second evaluation — should be deduped
        val second = evaluator.evaluate(listOf(state))
        assertTrue(second.isEmpty())
    }

    @Test
    fun `session fixed produces dismiss marker`() {
        prefs.setAlertsEnabled(account.id, true)

        // First, notify about session error
        val errorState = makeState(error = AccountError.TokenExpired)
        evaluator.evaluate(listOf(errorState))

        // Then session is fixed
        val fixedState = makeState(usage = makeUsage(30f, 20f))
        val result = evaluator.evaluate(listOf(fixedState))

        // Should have a dismiss marker (negative id) plus any threshold notifications
        val dismissals = result.filter { it.id < 0 }
        assertEquals(1, dismissals.size)
    }

    @Test
    fun `channel routing - low thresholds go to warnings`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(usage = makeUsage(55f, 30f))

        val result = evaluator.evaluate(listOf(state))
        val fiveHourNotifs = result.filter { it.title.contains("5-Hour") }

        assertTrue(fiveHourNotifs.all { it.channelId == NotificationChannelManager.CHANNEL_USAGE_WARNINGS })
    }

    @Test
    fun `channel routing - high thresholds go to critical`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(usage = makeUsage(95f, 30f))

        val result = evaluator.evaluate(listOf(state))

        // 5h: 95 >= 50, 75, 90 all fire. 50 and 75 → warnings, 90 → critical
        val critical = result.filter { it.channelId == NotificationChannelManager.CHANNEL_USAGE_CRITICAL }
        val warnings = result.filter { it.channelId == NotificationChannelManager.CHANNEL_USAGE_WARNINGS }
        assertTrue(critical.isNotEmpty())
        assertTrue(warnings.isNotEmpty())
    }

    @Test
    fun `multiple thresholds crossed simultaneously`() {
        prefs.setAlertsEnabled(account.id, true)
        val state = makeState(usage = makeUsage(100f, 100f))

        val result = evaluator.evaluate(listOf(state))

        // All 4 thresholds for both windows = 8 notifications
        assertEquals(8, result.filter { it.id > 0 }.size)
    }

    @Test
    fun `75 threshold goes to warnings channel`() {
        prefs.setAlertsEnabled(account.id, true)
        prefs.setThresholds(account.id, "5h", listOf(75))
        prefs.setThresholds(account.id, "7d", emptyList())
        val state = makeState(usage = makeUsage(80f, 30f))

        val result = evaluator.evaluate(listOf(state))
        assertEquals(1, result.size)
        assertEquals(NotificationChannelManager.CHANNEL_USAGE_WARNINGS, result[0].channelId)
    }

    @Test
    fun `notification ids are deterministic`() {
        val expectedId = "acc-1_5h_50".hashCode()
        prefs.setAlertsEnabled(account.id, true)
        prefs.setThresholds(account.id, "5h", listOf(50))
        prefs.setThresholds(account.id, "7d", emptyList())
        val state = makeState(usage = makeUsage(55f, 30f))

        val result = evaluator.evaluate(listOf(state))
        assertEquals(1, result.size)
        assertEquals(expectedId, result[0].id)
    }
}
