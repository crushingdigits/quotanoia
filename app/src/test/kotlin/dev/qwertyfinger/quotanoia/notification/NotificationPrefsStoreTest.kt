package dev.qwertyfinger.quotanoia.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationPrefsStoreTest {

    private lateinit var store: NotificationPrefsStore
    private val accountId = "test-account-123"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clear prefs before each test
        context.getSharedPreferences(NotificationPrefsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = NotificationPrefsStore(context)
    }

    @Test
    fun `alerts disabled by default`() {
        assertFalse(store.isAlertsEnabled(accountId))
    }

    @Test
    fun `default thresholds applied on first enable`() {
        store.setAlertsEnabled(accountId, true)
        assertTrue(store.isAlertsEnabled(accountId))

        val thresholds5h = store.getThresholds(accountId, "5h")
        val thresholds7d = store.getThresholds(accountId, "7d")

        assertEquals(NotificationPrefsStore.DEFAULT_THRESHOLDS, thresholds5h)
        assertEquals(NotificationPrefsStore.DEFAULT_THRESHOLDS, thresholds7d)
    }

    @Test
    fun `threshold add and remove`() {
        store.setAlertsEnabled(accountId, true)

        // Add a new threshold
        val updated = store.getThresholds(accountId, "5h") + 80
        store.setThresholds(accountId, "5h", updated)
        assertEquals(listOf(50, 75, 80, 90, 100), store.getThresholds(accountId, "5h"))

        // Remove a threshold
        val removed = store.getThresholds(accountId, "5h") - 75
        store.setThresholds(accountId, "5h", removed)
        assertEquals(listOf(50, 80, 90, 100), store.getThresholds(accountId, "5h"))
    }

    @Test
    fun `seen flag set and check`() {
        assertFalse(store.isThresholdSeen(accountId, "5h", 50))

        store.markThresholdSeen(accountId, "5h", 50, "2026-01-01T00:00:00Z")
        assertTrue(store.isThresholdSeen(accountId, "5h", 50))

        assertTrue(store.isThresholdSeenForReset(accountId, "5h", 50, "2026-01-01T00:00:00Z"))
        assertFalse(store.isThresholdSeenForReset(accountId, "5h", 50, "2026-01-02T00:00:00Z"))
    }

    @Test
    fun `stale seen flags cleared on resetsAt change`() {
        store.setAlertsEnabled(accountId, true)

        // Mark threshold as seen with old reset time
        store.markThresholdSeen(accountId, "5h", 50, "2026-01-01T00:00:00Z")
        store.markThresholdSeen(accountId, "5h", 75, "2026-01-01T00:00:00Z")
        assertTrue(store.isThresholdSeen(accountId, "5h", 50))
        assertTrue(store.isThresholdSeen(accountId, "5h", 75))

        // Clear stale flags with new reset time
        store.clearStaleSeenFlags(accountId, "5h", "2026-01-02T00:00:00Z")

        // Flags should be cleared
        assertFalse(store.isThresholdSeen(accountId, "5h", 50))
        assertFalse(store.isThresholdSeen(accountId, "5h", 75))
    }

    @Test
    fun `stale seen flags not cleared when resetsAt matches`() {
        store.setAlertsEnabled(accountId, true)

        store.markThresholdSeen(accountId, "5h", 50, "2026-01-01T00:00:00Z")
        assertTrue(store.isThresholdSeen(accountId, "5h", 50))

        // Same reset time — should not clear
        store.clearStaleSeenFlags(accountId, "5h", "2026-01-01T00:00:00Z")
        assertTrue(store.isThresholdSeen(accountId, "5h", 50))
    }

    @Test
    fun `hint card dismiss persistence`() {
        assertFalse(store.isHintCardDismissed())

        store.dismissHintCard()
        assertTrue(store.isHintCardDismissed())
    }

    @Test
    fun `session error notified flag`() {
        assertFalse(store.isSessionErrorNotified(accountId))

        store.setSessionErrorNotified(accountId, true)
        assertTrue(store.isSessionErrorNotified(accountId))

        store.setSessionErrorNotified(accountId, false)
        assertFalse(store.isSessionErrorNotified(accountId))
    }

    @Test
    fun `empty thresholds returned for unknown account`() {
        val thresholds = store.getThresholds("nonexistent", "5h")
        assertTrue(thresholds.isEmpty())
    }

    @Test
    fun `re-enabling preserves existing thresholds`() {
        store.setAlertsEnabled(accountId, true)
        store.setThresholds(accountId, "5h", listOf(30, 60, 95))

        store.setAlertsEnabled(accountId, false)
        store.setAlertsEnabled(accountId, true)

        // Should keep the custom thresholds, not reset to defaults
        assertEquals(listOf(30, 60, 95), store.getThresholds(accountId, "5h"))
    }
}
