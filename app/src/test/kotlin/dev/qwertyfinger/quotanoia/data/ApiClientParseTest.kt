package dev.qwertyfinger.quotanoia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientParseTest {

    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        client = ApiClient()
    }

    // ── Valid JSON ──────────────────────────────────────────────────

    @Test
    fun `parseUsage returns correct data for valid JSON`() {
        val json = """
            {
              "five_hour": {
                "utilization": 42.5,
                "resets_at": "2025-06-15T12:00:00Z"
              },
              "seven_day": {
                "utilization": 78.3,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        val fetchResult = result.getOrThrow()
        assertEquals(42.5f, fetchResult.usage.fiveHour.utilization, 0.01f)
        assertEquals(78.3f, fetchResult.usage.sevenDay.utilization, 0.01f)
        assertNull(fetchResult.newSessionKey)
    }

    @Test
    fun `parseUsage passes through new session key`() {
        val json = """
            {
              "five_hour": {
                "utilization": 10.0,
                "resets_at": "2025-06-15T12:00:00Z"
              },
              "seven_day": {
                "utilization": 20.0,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, "new-key-123")

        assertTrue(result.isSuccess)
        assertEquals("new-key-123", result.getOrThrow().newSessionKey)
    }

    @Test
    fun `parseUsage handles zero utilization`() {
        val json = """
            {
              "five_hour": {
                "utilization": 0.0,
                "resets_at": "2025-06-15T12:00:00Z"
              },
              "seven_day": {
                "utilization": 0.0,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertEquals(0f, result.getOrThrow().usage.fiveHour.utilization, 0.01f)
        assertEquals(0f, result.getOrThrow().usage.sevenDay.utilization, 0.01f)
    }

    @Test
    fun `parseUsage handles 100 percent utilization`() {
        val json = """
            {
              "five_hour": {
                "utilization": 100.0,
                "resets_at": "2025-06-15T12:00:00Z"
              },
              "seven_day": {
                "utilization": 100.0,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertEquals(100f, result.getOrThrow().usage.fiveHour.utilization, 0.01f)
    }

    // ── Missing fields ─────────────────────────────────────────────

    @Test
    fun `parseUsage fails when five_hour is missing`() {
        val json = """
            {
              "seven_day": {
                "utilization": 50.0,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as ApiClient.ApiException).accountError
        assertTrue(error is AccountError.ParseError)
    }

    @Test
    fun `parseUsage fails when seven_day is missing`() {
        val json = """
            {
              "five_hour": {
                "utilization": 50.0,
                "resets_at": "2025-06-15T12:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as ApiClient.ApiException).accountError
        assertTrue(error is AccountError.ParseError)
    }

    @Test
    fun `parseUsage fails when utilization is missing`() {
        val json = """
            {
              "five_hour": {
                "resets_at": "2025-06-15T12:00:00Z"
              },
              "seven_day": {
                "utilization": 50.0,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as ApiClient.ApiException).accountError
        assertTrue(error is AccountError.ParseError)
    }

    // ── Malformed JSON ─────────────────────────────────────────────

    @Test
    fun `parseUsage fails for empty string`() {
        val result = client.parseUsage("", null)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is ApiClient.ApiException)
        assertTrue((exception as ApiClient.ApiException).accountError is AccountError.ParseError)
    }

    @Test
    fun `parseUsage fails for invalid JSON`() {
        val result = client.parseUsage("not json at all", null)

        assertTrue(result.isFailure)
        assertTrue(
            (result.exceptionOrNull() as ApiClient.ApiException).accountError is AccountError.ParseError,
        )
    }

    @Test
    fun `parseUsage fails for invalid timestamp format`() {
        val json = """
            {
              "five_hour": {
                "utilization": 42.5,
                "resets_at": "not-a-timestamp"
              },
              "seven_day": {
                "utilization": 78.3,
                "resets_at": "2025-06-20T00:00:00Z"
              }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isFailure)
        assertTrue(
            (result.exceptionOrNull() as ApiClient.ApiException).accountError is AccountError.ParseError,
        )
    }
}
