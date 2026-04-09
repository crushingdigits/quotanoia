package dev.qwertyfinger.quotanoia.data

import dev.qwertyfinger.quotanoia.data.api.ApiClient
import dev.qwertyfinger.quotanoia.data.api.UsageFetchResult
import dev.qwertyfinger.quotanoia.data.model.AccountError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ApiClientParseTest {

    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        client = ApiClient()
    }

    // ── parseUsage: valid JSON ─────────────────────────────────────

    @Test
    fun `parseUsage returns correct data for valid JSON`() {
        val json = """
            {
              "five_hour": { "utilization": 42.5, "resets_at": "2025-06-15T12:00:00Z" },
              "seven_day": { "utilization": 78.3, "resets_at": "2025-06-20T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        val data = result.getOrThrow().usage
        assertEquals(42.5f, data.fiveHour.utilization, 0.01f)
        assertEquals(78.3f, data.sevenDay.utilization, 0.01f)
        assertEquals(Instant.parse("2025-06-15T12:00:00Z"), data.fiveHour.resetsAt)
        assertEquals(Instant.parse("2025-06-20T00:00:00Z"), data.sevenDay.resetsAt)
        assertNull(result.getOrThrow().newSessionKey)
    }

    @Test
    fun `parseUsage passes through new session key`() {
        val json = validJson(fiveHourUtil = 10.0, sevenDayUtil = 20.0)
        val result = client.parseUsage(json, "new-key-123")

        assertTrue(result.isSuccess)
        assertEquals("new-key-123", result.getOrThrow().newSessionKey)
    }

    @Test
    fun `parseUsage handles zero utilization`() {
        val json = validJson(fiveHourUtil = 0.0, sevenDayUtil = 0.0)
        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertEquals(
            0f,
            result
                .getOrThrow()
                .usage.fiveHour.utilization,
            0.01f,
        )
        assertEquals(
            0f,
            result
                .getOrThrow()
                .usage.sevenDay.utilization,
            0.01f,
        )
    }

    @Test
    fun `parseUsage handles 100 percent utilization`() {
        val json = validJson(fiveHourUtil = 100.0, sevenDayUtil = 100.0)
        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertEquals(
            100f,
            result
                .getOrThrow()
                .usage.fiveHour.utilization,
            0.01f,
        )
    }

    // ── parseUsage: null and missing resets_at ─────────────────────

    @Test
    fun `parseUsage handles null resets_at gracefully`() {
        val json = """
            {
              "five_hour": { "utilization": 0.0, "resets_at": null },
              "seven_day": { "utilization": 10.0, "resets_at": "2025-06-20T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertNull(
            result
                .getOrThrow()
                .usage.fiveHour.resetsAt,
        )
        assertNotNull(
            result
                .getOrThrow()
                .usage.sevenDay.resetsAt,
        )
    }

    @Test
    fun `parseUsage handles malformed timestamp gracefully`() {
        val json = """
            {
              "five_hour": { "utilization": 42.5, "resets_at": "not-a-timestamp" },
              "seven_day": { "utilization": 78.3, "resets_at": "2025-06-20T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertNull(
            result
                .getOrThrow()
                .usage.fiveHour.resetsAt,
        )
        assertNotNull(
            result
                .getOrThrow()
                .usage.sevenDay.resetsAt,
        )
    }

    @Test
    fun `parseUsage handles missing resets_at field gracefully`() {
        val json = """
            {
              "five_hour": { "utilization": 50.0 },
              "seven_day": { "utilization": 30.0, "resets_at": "2025-06-20T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertNull(
            result
                .getOrThrow()
                .usage.fiveHour.resetsAt,
        )
    }

    @Test
    fun `parseUsage handles missing utilization with default zero`() {
        val json = """
            {
              "five_hour": { "resets_at": "2025-06-15T12:00:00Z" },
              "seven_day": { "utilization": 50.0, "resets_at": "2025-06-20T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertEquals(
            0f,
            result
                .getOrThrow()
                .usage.fiveHour.utilization,
            0.01f,
        )
    }

    // ── parseUsage: missing window objects ─────────────────────────

    @Test
    fun `parseUsage fails when five_hour is missing`() {
        val json = """{ "seven_day": { "utilization": 50.0, "resets_at": "2025-06-20T00:00:00Z" } }"""

        val result = client.parseUsage(json, null)

        assertTrue(result.isFailure)
        assertParseError(result)
    }

    @Test
    fun `parseUsage fails when seven_day is missing`() {
        val json = """{ "five_hour": { "utilization": 50.0, "resets_at": "2025-06-15T12:00:00Z" } }"""

        val result = client.parseUsage(json, null)

        assertTrue(result.isFailure)
        assertParseError(result)
    }

    // ── parseUsage: malformed JSON ─────────────────────────────────

    @Test
    fun `parseUsage fails for empty string`() {
        val result = client.parseUsage("", null)

        assertTrue(result.isFailure)
        assertParseError(result)
    }

    @Test
    fun `parseUsage fails for invalid JSON`() {
        val result = client.parseUsage("not json at all", null)

        assertTrue(result.isFailure)
        assertParseError(result)
    }

    // ── Window parsing via parseUsage ─────────────────────────────

    @Test
    fun `parseUsage with valid window data returns correct window`() {
        val json = """
            {
              "five_hour": { "utilization": 42.5, "resets_at": "2026-04-07T18:00:00Z" },
              "seven_day": { "utilization": 10.0, "resets_at": "2026-04-10T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        val fiveHour = result.getOrThrow().usage.fiveHour
        assertEquals(42.5f, fiveHour.utilization, 0.01f)
        assertEquals(Instant.parse("2026-04-07T18:00:00Z"), fiveHour.resetsAt)
    }

    @Test
    fun `parseUsage with empty resets_at returns null instant`() {
        val json = """
            {
              "five_hour": { "utilization": 50.0, "resets_at": "" },
              "seven_day": { "utilization": 10.0, "resets_at": "2026-04-10T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertNull(
            result
                .getOrThrow()
                .usage.fiveHour.resetsAt,
        )
    }

    @Test
    fun `parseUsage with string null resets_at returns null`() {
        val json = """
            {
              "five_hour": { "utilization": 20.0, "resets_at": "null" },
              "seven_day": { "utilization": 10.0, "resets_at": "2026-04-10T00:00:00Z" }
            }
        """.trimIndent()

        val result = client.parseUsage(json, null)

        assertTrue(result.isSuccess)
        assertNull(
            result
                .getOrThrow()
                .usage.fiveHour.resetsAt,
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun validJson(
        fiveHourUtil: Double = 42.5,
        sevenDayUtil: Double = 78.3,
    ) = """
        {
          "five_hour": { "utilization": $fiveHourUtil, "resets_at": "2025-06-15T12:00:00Z" },
          "seven_day": { "utilization": $sevenDayUtil, "resets_at": "2025-06-20T00:00:00Z" }
        }
        """.trimIndent()

    private fun assertParseError(result: Result<UsageFetchResult>) {
        val error = (result.exceptionOrNull() as ApiClient.ApiException).accountError
        assertTrue(error is AccountError.ParseError)
    }
}
