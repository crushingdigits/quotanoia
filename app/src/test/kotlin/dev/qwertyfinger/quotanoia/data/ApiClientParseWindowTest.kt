package dev.qwertyfinger.quotanoia.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ApiClientParseWindowTest {

    private val client = ApiClient()

    @Test
    fun `parseWindow with valid utilization and resets_at`() {
        val json = JSONObject().apply {
            put("utilization", 42.5)
            put("resets_at", "2026-04-07T18:00:00Z")
        }
        val window = client.parseWindow(json, "test")
        assertEquals(42.5f, window.utilization, 0.01f)
        assertEquals(Instant.parse("2026-04-07T18:00:00Z"), window.resetsAt)
    }

    @Test
    fun `parseWindow with null resets_at returns null instant`() {
        val json = JSONObject().apply {
            put("utilization", 0.0)
            put("resets_at", JSONObject.NULL)
        }
        val window = client.parseWindow(json, "test")
        assertEquals(0.0f, window.utilization, 0.01f)
        assertNull(window.resetsAt)
    }

    @Test
    fun `parseWindow with missing resets_at returns null instant`() {
        val json = JSONObject().apply {
            put("utilization", 10.0)
        }
        val window = client.parseWindow(json, "test")
        assertEquals(10.0f, window.utilization, 0.01f)
        assertNull(window.resetsAt)
    }

    @Test
    fun `parseWindow with malformed resets_at returns null instant`() {
        val json = JSONObject().apply {
            put("utilization", 75.0)
            put("resets_at", "not-a-timestamp")
        }
        val window = client.parseWindow(json, "test")
        assertEquals(75.0f, window.utilization, 0.01f)
        assertNull(window.resetsAt)
    }

    @Test
    fun `parseWindow with empty resets_at returns null instant`() {
        val json = JSONObject().apply {
            put("utilization", 50.0)
            put("resets_at", "")
        }
        val window = client.parseWindow(json, "test")
        assertEquals(50.0f, window.utilization, 0.01f)
        assertNull(window.resetsAt)
    }

    @Test
    fun `parseWindow with missing utilization defaults to zero`() {
        val json = JSONObject().apply {
            put("resets_at", "2026-04-07T18:00:00Z")
        }
        val window = client.parseWindow(json, "test")
        assertEquals(0.0f, window.utilization, 0.01f)
        assertEquals(Instant.parse("2026-04-07T18:00:00Z"), window.resetsAt)
    }

    @Test
    fun `parseWindow with string null resets_at returns null instant`() {
        val json = JSONObject().apply {
            put("utilization", 20.0)
            put("resets_at", "null")
        }
        val window = client.parseWindow(json, "test")
        assertEquals(20.0f, window.utilization, 0.01f)
        assertNull(window.resetsAt)
    }
}
