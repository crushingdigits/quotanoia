# kotlinx.serialization Migration + Data Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all `org.json` manual parsing with `kotlinx.serialization` and extract the data layer into a shared `data` module.

**Architecture:** Create a `data` Android library module containing models, API client, account store, repository, and wear sync. Both `app` and `wear` depend on it. All JSON serialization uses `kotlinx.serialization`.

**Tech Stack:** Kotlin 2.3.20, kotlinx.serialization 1.10.0, OkHttp, EncryptedSharedPreferences, Play Services Wearable

**Spec:** `docs/superpowers/specs/2026-04-09-kotlinx-serialization-design.md`

**Repo:** `crushingdigits/quotanoia` at `/workspace/group/repos/quotanoia`

**Branch:** `refactor/kotlinx-serialization` off `main`

**Bot push token:** Generate via GitHub App (APP_ID=3305507, INSTALLATION_ID=122127464, PEM at `/credentials/personal-dev/github-app-key.pem`).

---

### Task 1: Create branch and data module skeleton

**Files:**
- Create: `data/build.gradle.kts`
- Create: `data/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Create branch**

```bash
cd /workspace/group/repos/quotanoia
git checkout main
git checkout -b refactor/kotlinx-serialization
```

- [ ] **Step 2: Add kotlinx-serialization-json library to version catalog**

In `gradle/libs.versions.toml`, add to `[libraries]` section (alphabetically near other kotlinx entries):

```toml
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```

- [ ] **Step 3: Create data module directory structure**

```bash
mkdir -p data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/model
mkdir -p data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/api
mkdir -p data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/store
mkdir -p data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/repository
mkdir -p data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/sync
```

- [ ] **Step 4: Create data/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.quotanoia.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.qwertyfinger.quotanoia.data"
}

dependencies {
    implementation(projects.core.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.security.crypto)
    implementation(libs.play.services.wearable)
}
```

- [ ] **Step 5: Create data/src/main/AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 6: Add data module to settings.gradle.kts**

Add `include(":data")` after the existing includes:

```kotlin
include(":app")
include(":core:coroutines")
include(":data")
include(":wear")
```

- [ ] **Step 7: Verify Gradle sync**

```bash
./gradlew :data:help
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add data/ settings.gradle.kts gradle/libs.versions.toml
git commit -m "Add data module skeleton with kotlinx.serialization"
```

---

### Task 2: Create @Serializable models and response DTOs

**Files:**
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/model/Models.kt`
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/api/UsageResponse.kt`
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/sync/WearSyncPayload.kt`

- [ ] **Step 1: Create Models.kt with @Serializable annotations**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/model/Models.kt`

```kotlin
package dev.qwertyfinger.quotanoia.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class ClaudeAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sessionKey: String,
    val orgId: String,
)

data class UsageWindow(
    val utilization: Float, // 0..100
    val resetsAt: Instant?, // null when window hasn't started
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
            is TokenExpired -> "Session expired — re-enter in settings"
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
    val nextRetryAt: Instant? = null,
) {
    val hasError: Boolean get() = error != null
    val isStale: Boolean
        get() = usage != null &&
            Instant.now().epochSecond - usage.fetchedAt.epochSecond > 600
}

const val BASE_POLL_INTERVAL_MS = 5 * 60 * 1000L
const val MAX_BACKOFF_MS = 30 * 60 * 1000L
```

Note: `UsageWindow`, `UsageData`, `AccountError`, `AccountState` are NOT `@Serializable` because they contain `Instant` which needs custom serialization. They use `@Serializable` DTOs for persistence (see `UsageCacheDto` in Task 4). Only `ClaudeAccount` is directly serializable (no Instant fields).

- [ ] **Step 2: Create UsageResponse.kt (API response DTOs)**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/api/UsageResponse.kt`

```kotlin
package dev.qwertyfinger.quotanoia.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsageResponse(
    @SerialName("five_hour") val fiveHour: WindowResponse,
    @SerialName("seven_day") val sevenDay: WindowResponse,
)

@Serializable
data class WindowResponse(
    val utilization: Double = 0.0,
    @SerialName("resets_at") val resetsAt: String? = null,
)
```

- [ ] **Step 3: Create WearSyncPayload.kt**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/sync/WearSyncPayload.kt`

```kotlin
package dev.qwertyfinger.quotanoia.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WearSyncAccount(
    val id: String,
    val name: String,
    @SerialName("has_data") val hasData: Boolean,
    @SerialName("5h_pct") val fiveHourPct: Double = -1.0,
    @SerialName("7d_pct") val sevenDayPct: Double = -1.0,
    @SerialName("has_error") val hasError: Boolean = false,
    @SerialName("error_msg") val errorMsg: String = "",
)
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :data:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add data/src/
git commit -m "Add @Serializable models and response DTOs"
```

---

### Task 3: Move and update ApiClient to use kotlinx.serialization

**Files:**
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/api/ApiClient.kt`
- Delete: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/ApiClient.kt`

- [ ] **Step 1: Create new ApiClient.kt in data module**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/api/ApiClient.kt`

This is the existing ApiClient with `org.json` replaced by `kotlinx.serialization.json`:

```kotlin
package dev.qwertyfinger.quotanoia.data.api

import dev.qwertyfinger.quotanoia.data.model.AccountError
import dev.qwertyfinger.quotanoia.data.model.UsageData
import dev.qwertyfinger.quotanoia.data.model.UsageWindow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

data class UsageFetchResult(val usage: UsageData, val newSessionKey: String? = null)

class ApiClient(debug: Boolean = false) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }.apply {
                level = if (debug) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        ).build()

    fun fetchUsage(
        sessionKey: String,
        orgId: String,
    ): Result<UsageFetchResult> {
        Timber.d("Fetching usage for org %s", orgId)

        val url = "$BASE_URL/$orgId/usage"
        val request = Request
            .Builder()
            .url(url)
            .header("accept", "*/*")
            .header("content-type", "application/json")
            .header("anthropic-client-platform", "web_claude_ai")
            .header("Cookie", "sessionKey=$sessionKey")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                val retryAfterMs = resp
                    .header("Retry-After")
                    ?.toLongOrNull()
                    ?.let { it * MILLIS_PER_SECOND }

                Timber.d("Response: %d for org %s", resp.code, orgId)

                when (resp.code) {
                    HTTP_OK -> {
                        val newKey = parseSetCookieSessionKey(resp)
                        parseUsage(resp.body?.string().orEmpty(), newKey)
                    }

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> {
                        Timber.w("Session expired (%d) for org %s", resp.code, orgId)
                        Result.failure(ApiException(AccountError.TokenExpired))
                    }

                    HTTP_TOO_MANY_REQUESTS -> {
                        Timber.w("Rate limited (429), retry-after: %s ms", retryAfterMs)
                        Result.failure(ApiException(AccountError.RateLimited, retryAfterMs))
                    }

                    in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> {
                        Timber.w("Server error (%d), retry-after: %s ms", resp.code, retryAfterMs)
                        Result.failure(ApiException(AccountError.ServerError(resp.code), retryAfterMs))
                    }

                    else -> {
                        Timber.w("Unexpected status %d", resp.code)
                        Result.failure(ApiException(AccountError.ServerError(resp.code)))
                    }
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "Network error fetching usage")
            Result.failure(ApiException(AccountError.NetworkError(e.message ?: "Unknown")))
        }
    }

    @Suppress("TooGenericExceptionCaught")
    internal fun parseUsage(
        body: String,
        newSessionKey: String?,
    ): Result<UsageFetchResult> {
        return try {
            val response = json.decodeFromString<UsageResponse>(body)
            val data = UsageData(
                fiveHour = response.fiveHour.toDomain(),
                sevenDay = response.sevenDay.toDomain(),
            )
            Timber.d("Parsed usage: 5h=%.1f%%, 7d=%.1f%%", data.fiveHour.utilization, data.sevenDay.utilization)
            if (newSessionKey != null) Timber.d("Session key rotated")
            Result.success(UsageFetchResult(data, newSessionKey))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse usage response")
            Result.failure(ApiException(AccountError.ParseError(e.message ?: "Parse failed")))
        }
    }

    private fun parseSetCookieSessionKey(resp: okhttp3.Response): String? {
        for (value in resp.headers("Set-Cookie")) {
            for (part in value.split(";")) {
                val trimmed = part.trim()
                if (trimmed.startsWith("sessionKey=")) {
                    return trimmed.removePrefix("sessionKey=")
                }
            }
        }
        return null
    }

    class ApiException(val accountError: AccountError, val retryAfterMs: Long? = null) :
        Exception(accountError.displayMessage)

    companion object {
        private const val BASE_URL = "https://claude.ai/api/organizations"
        private const val TIMEOUT_SECONDS = 15L
        private const val MILLIS_PER_SECOND = 1000L
        private const val HTTP_OK = 200
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR_MIN = 500
        private const val HTTP_SERVER_ERROR_MAX = 599
    }
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
private fun WindowResponse.toDomain(): UsageWindow {
    val resetsAt = resetsAt
        ?.takeIf { it.isNotEmpty() && it != "null" }
        ?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                Timber.w("Unparseable resets_at: %s", it)
                null
            }
        }
    return UsageWindow(utilization.toFloat(), resetsAt)
}
```

Key changes from original:
- `JSONObject` → `Json.decodeFromString<UsageResponse>(body)`
- `parseWindow()` → `WindowResponse.toDomain()` extension
- `BuildConfig.DEBUG` → constructor param `debug: Boolean` (data module has no BuildConfig)
- Package: `dev.qwertyfinger.quotanoia.data.api`

- [ ] **Step 2: Delete old ApiClient.kt**

```bash
rm app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/ApiClient.kt
```

- [ ] **Step 3: Verify compilation**

This will fail because `app` still references the old path. That's expected — we fix it in Task 6.

- [ ] **Step 4: Commit**

```bash
git add data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/api/ApiClient.kt
git add app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/ApiClient.kt
git commit -m "Move ApiClient to data module with kotlinx.serialization"
```

---

### Task 4: Move and update AccountStore to use kotlinx.serialization

**Files:**
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/store/AccountStore.kt`
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/store/UsageCacheDto.kt`
- Delete: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/AccountStore.kt`

- [ ] **Step 1: Create UsageCacheDto.kt**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/store/UsageCacheDto.kt`

```kotlin
package dev.qwertyfinger.quotanoia.data.store

import kotlinx.serialization.Serializable

@Serializable
data class UsageCacheDto(
    val fiveHourUtil: Float,
    val fiveHourResets: String? = null,
    val sevenDayUtil: Float,
    val sevenDayResets: String? = null,
    val fetchedAt: String,
)
```

- [ ] **Step 2: Create new AccountStore.kt in data module**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/store/AccountStore.kt`

```kotlin
package dev.qwertyfinger.quotanoia.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.qwertyfinger.quotanoia.data.model.AccountError
import dev.qwertyfinger.quotanoia.data.model.BASE_POLL_INTERVAL_MS
import dev.qwertyfinger.quotanoia.data.model.ClaudeAccount
import dev.qwertyfinger.quotanoia.data.model.UsageData
import dev.qwertyfinger.quotanoia.data.model.UsageWindow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class AccountStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val masterKey = MasterKey
        .Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "claude_accounts",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)

    // ── Accounts ────────────────────────────────────────────

    @Suppress("TooGenericExceptionCaught")
    fun loadAccounts(): List<ClaudeAccount> {
        val raw = securePrefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ClaudeAccount>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<ClaudeAccount>) {
        securePrefs.edit()
            .putString(KEY_ACCOUNTS, json.encodeToString(accounts))
            .apply()
    }

    fun addAccount(account: ClaudeAccount) {
        val list = loadAccounts().toMutableList()
        list.add(account)
        saveAccounts(list)
    }

    fun removeAccount(id: String) {
        saveAccounts(loadAccounts().filter { it.id != id })
        clearCachedState(id)
    }

    fun updateAccount(account: ClaudeAccount) {
        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == account.id }
        if (idx >= 0) {
            list[idx] = account
            saveAccounts(list)
        }
    }

    // ── Usage cache ─────────────────────────────────────────

    fun cacheUsage(
        accountId: String,
        usage: UsageData,
    ) {
        val dto = UsageCacheDto(
            fiveHourUtil = usage.fiveHour.utilization,
            fiveHourResets = usage.fiveHour.resetsAt?.toString(),
            sevenDayUtil = usage.sevenDay.utilization,
            sevenDayResets = usage.sevenDay.resetsAt?.toString(),
            fetchedAt = usage.fetchedAt.toString(),
        )
        cachePrefs
            .edit()
            .putString("usage_$accountId", json.encodeToString(dto))
            .remove("error_$accountId")
            .putLong("backoff_$accountId", BASE_POLL_INTERVAL_MS)
            .apply()
    }

    fun cacheError(
        accountId: String,
        error: AccountError,
        backoffMs: Long,
    ) {
        cachePrefs
            .edit()
            .putString("error_$accountId", error.displayMessage)
            .putLong("backoff_$accountId", backoffMs)
            .apply()
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun getCachedUsage(accountId: String): UsageData? {
        val raw = cachePrefs.getString("usage_$accountId", null) ?: return null
        return try {
            val dto = json.decodeFromString<UsageCacheDto>(raw)
            UsageData(
                fiveHour = UsageWindow(
                    utilization = dto.fiveHourUtil,
                    resetsAt = dto.fiveHourResets?.let { parseInstant(it) },
                ),
                sevenDay = UsageWindow(
                    utilization = dto.sevenDayUtil,
                    resetsAt = dto.sevenDayResets?.let { parseInstant(it) },
                ),
                fetchedAt = parseInstant(dto.fetchedAt) ?: return null,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedError(accountId: String): String? =
        cachePrefs.getString("error_$accountId", null)

    fun getBackoff(accountId: String): Long =
        cachePrefs.getLong("backoff_$accountId", BASE_POLL_INTERVAL_MS)

    private fun clearCachedState(accountId: String) {
        cachePrefs
            .edit()
            .remove("usage_$accountId")
            .remove("error_$accountId")
            .remove("backoff_$accountId")
            .apply()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun parseInstant(value: String): Instant? {
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_ACCOUNTS = "accounts_json"
    }
}
```

Key changes: All `JSONObject`/`JSONArray` replaced with `Json.encodeToString`/`decodeFromString`. Usage cache uses `UsageCacheDto`.

- [ ] **Step 3: Delete old AccountStore.kt**

```bash
rm app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/AccountStore.kt
```

- [ ] **Step 4: Commit**

```bash
git add data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/store/
git add app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/AccountStore.kt
git commit -m "Move AccountStore to data module with kotlinx.serialization"
```

---

### Task 5: Move UsageRepository and WearSyncManager

**Files:**
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/repository/UsageRepository.kt`
- Create: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/sync/WearSyncManager.kt`
- Delete: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/UsageRepository.kt`
- Delete: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/Models.kt`
- Delete: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/sync/WearSyncManager.kt`

- [ ] **Step 1: Create new UsageRepository.kt**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/repository/UsageRepository.kt`

Copy the existing `UsageRepository.kt` with updated imports:

```kotlin
package dev.qwertyfinger.quotanoia.data.repository

import dev.qwertyfinger.quotanoia.data.api.ApiClient
import dev.qwertyfinger.quotanoia.data.model.AccountError
import dev.qwertyfinger.quotanoia.data.model.AccountState
import dev.qwertyfinger.quotanoia.data.model.BASE_POLL_INTERVAL_MS
import dev.qwertyfinger.quotanoia.data.model.ClaudeAccount
import dev.qwertyfinger.quotanoia.data.model.MAX_BACKOFF_MS
import dev.qwertyfinger.quotanoia.data.store.AccountStore
import dev.qwertyfinger.quotanoia.data.sync.WearSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import kotlin.math.min
```

The body is identical to the existing `UsageRepository.kt`. Copy it verbatim.

- [ ] **Step 2: Create new WearSyncManager.kt with kotlinx.serialization**

File: `data/src/main/kotlin/dev/qwertyfinger/quotanoia/data/sync/WearSyncManager.kt`

```kotlin
package dev.qwertyfinger.quotanoia.data.sync

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.data.model.AccountState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class WearSyncManager(context: Context) {

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val json = Json { encodeDefaults = true }

    fun syncStates(states: List<AccountState>) {
        val payload = serializeStates(states)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Timber.d("Connected wearable nodes: %d", nodes.size)
                for (node in nodes) {
                    Timber.d("Sending usage to %s (id=%s)", node.displayName, node.id)
                    messageClient
                        .sendMessage(node.id, PATH_USAGE, payload)
                        .addOnSuccessListener {
                            Timber.d("Message sent to %s", node.displayName)
                        }.addOnFailureListener {
                            Timber.w(it, "Failed to send message to %s", node.displayName)
                        }
                }
            }.addOnFailureListener { Timber.w(it, "Failed to get connected nodes") }
    }

    private fun serializeStates(states: List<AccountState>): ByteArray {
        val accounts = states.map { state ->
            WearSyncAccount(
                id = state.account.id,
                name = state.account.name,
                hasData = state.usage != null,
                fiveHourPct = state.usage?.fiveHour?.utilization?.toDouble() ?: -1.0,
                sevenDayPct = state.usage?.sevenDay?.utilization?.toDouble() ?: -1.0,
                hasError = state.hasError,
                errorMsg = state.error?.displayMessage.orEmpty(),
            )
        }
        return json.encodeToString(accounts).toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val PATH_USAGE = "/claude_usage"
    }
}
```

- [ ] **Step 3: Delete old files**

```bash
rm app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/UsageRepository.kt
rm app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/Models.kt
rm app/src/main/kotlin/dev/qwertyfinger/quotanoia/sync/WearSyncManager.kt
rmdir app/src/main/kotlin/dev/qwertyfinger/quotanoia/sync/ 2>/dev/null || true
```

- [ ] **Step 4: Verify data module compiles**

```bash
./gradlew :data:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Move UsageRepository and WearSyncManager to data module"
```

---

### Task 6: Update app module — dependencies and imports

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/QuotanoiaApp.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/MainViewModel.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/MainActivity.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/worker/UsageWorker.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/widget/UsageWidget.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/widget/UsageWidgetReceiver.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/ui/screens/AccountListScreen.kt`
- Modify: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/ui/screens/AddAccountDialog.kt`
- Delete: `app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/` (entire directory if empty)

- [ ] **Step 1: Update app/build.gradle.kts**

Add `implementation(projects.data)` and remove dependencies that moved to data module:

```kotlin
dependencies {
    implementation(projects.core.coroutines)
    implementation(projects.data)
    implementation(libs.compose.material.icons)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.workmanager)
    implementation(libs.play.services.wearable)
}
```

Removed: `okhttp`, `okhttp-logging`, `security-crypto`, `testImplementation("org.json:json:...")`.
Kept: `play-services-wearable` (app still uses MessageClient in QuotanoiaApp).

- [ ] **Step 2: Update all imports in app module**

For every file in `app/src/main/kotlin/` that imports from `dev.qwertyfinger.quotanoia.data.*` or `dev.qwertyfinger.quotanoia.sync.*`, update imports:

Old → New mappings:
- `dev.qwertyfinger.quotanoia.data.AccountStore` → `dev.qwertyfinger.quotanoia.data.store.AccountStore`
- `dev.qwertyfinger.quotanoia.data.ApiClient` → `dev.qwertyfinger.quotanoia.data.api.ApiClient`
- `dev.qwertyfinger.quotanoia.data.UsageRepository` → `dev.qwertyfinger.quotanoia.data.repository.UsageRepository`
- `dev.qwertyfinger.quotanoia.data.UsageFetchResult` → `dev.qwertyfinger.quotanoia.data.api.UsageFetchResult`
- `dev.qwertyfinger.quotanoia.data.*` (models) → `dev.qwertyfinger.quotanoia.data.model.*`
- `dev.qwertyfinger.quotanoia.sync.WearSyncManager` → `dev.qwertyfinger.quotanoia.data.sync.WearSyncManager`

Update each file. Read the file, find imports, replace them.

- [ ] **Step 3: Update ApiClient construction in QuotanoiaApp.kt**

The new `ApiClient` takes `debug: Boolean` instead of using `BuildConfig.DEBUG`:

```kotlin
repository = UsageRepository(store, ApiClient(debug = BuildConfig.DEBUG), wearSync)
```

- [ ] **Step 4: Delete remaining old data directory**

```bash
rm -rf app/src/main/kotlin/dev/qwertyfinger/quotanoia/data/
```

- [ ] **Step 5: Verify app compiles**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Update app module to use data module"
```

---

### Task 7: Update wear module — replace org.json with kotlinx.serialization

**Files:**
- Modify: `wear/build.gradle.kts`
- Modify: `wear/src/main/kotlin/dev/qwertyfinger/quotanoia/wear/WearAccountStore.kt`
- Modify: `wear/src/main/kotlin/dev/qwertyfinger/quotanoia/wear/DataLayerListenerService.kt`
- Modify: `wear/src/main/kotlin/dev/qwertyfinger/quotanoia/wear/WearApp.kt`

- [ ] **Step 1: Update wear/build.gradle.kts**

Add `implementation(projects.data)`:

```kotlin
dependencies {
    implementation(projects.core.coroutines)
    implementation(projects.data)
    implementation(libs.core.ktx)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.play.services.wearable)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.guava:guava:33.3.1-android")
}
```

- [ ] **Step 2: Update WearAccountStore.kt**

Replace `JSONArray` parsing with `kotlinx.serialization`:

```kotlin
package dev.qwertyfinger.quotanoia.wear

import android.content.Context
import android.content.SharedPreferences
import dev.qwertyfinger.quotanoia.data.sync.WearSyncAccount
import kotlinx.serialization.json.Json

class WearAccountStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wear_usage_cache", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    data class WearAccount(val id: String, val name: String)
    data class WearUsage(
        val fiveHourPct: Float,
        val sevenDayPct: Float,
        val hasError: Boolean = false,
        val errorMsg: String? = null,
    )

    data class AccountWithUsage(val account: WearAccount, val usage: WearUsage)

    fun getAll(): List<AccountWithUsage> {
        val count = prefs.getInt("count", 0)
        if (count == 0) return emptyList()

        return (0 until count).mapNotNull { i ->
            val id = prefs.getString("${i}_id", null) ?: return@mapNotNull null
            val name = prefs.getString("${i}_name", null) ?: return@mapNotNull null
            val hasData = prefs.getBoolean("${i}_has_data", false)
            val hasError = prefs.getBoolean("${i}_has_error", false)
            val errorMsg = prefs.getString("${i}_error_msg", null)

            val usage = if (hasData) {
                WearUsage(
                    fiveHourPct = prefs.getFloat("${i}_5h_pct", -1f),
                    sevenDayPct = prefs.getFloat("${i}_7d_pct", -1f),
                    hasError = hasError,
                    errorMsg = errorMsg,
                )
            } else {
                WearUsage(-1f, -1f, hasError = hasError, errorMsg = errorMsg)
            }

            AccountWithUsage(WearAccount(id, name), usage)
        }
    }

    fun updateFromSyncAccounts(accounts: List<WearSyncAccount>) {
        prefs.edit().apply {
            clear()
            putInt("count", accounts.size)
            accounts.forEachIndexed { i, account ->
                putString("${i}_id", account.id)
                putString("${i}_name", account.name)
                putBoolean("${i}_has_data", account.hasData)
                putFloat("${i}_5h_pct", account.fiveHourPct.toFloat())
                putFloat("${i}_7d_pct", account.sevenDayPct.toFloat())
                putBoolean("${i}_has_error", account.hasError)
                putString("${i}_error_msg", account.errorMsg)
            }
            apply()
        }
    }

    fun parsePayload(data: ByteArray): List<WearSyncAccount> {
        val raw = String(data, Charsets.UTF_8)
        return json.decodeFromString<List<WearSyncAccount>>(raw)
    }
}
```

Key changes:
- Removed `org.json.JSONArray` import
- `updateFromJson(JSONArray)` → `updateFromSyncAccounts(List<WearSyncAccount>)`
- Added `parsePayload()` to deserialize bytes → shared DTO

- [ ] **Step 3: Update DataLayerListenerService.kt**

```kotlin
package dev.qwertyfinger.quotanoia.wear

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    @Suppress("TooGenericExceptionCaught")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: path=${messageEvent.path}, from=${messageEvent.sourceNodeId}")

        if (messageEvent.path != PATH_USAGE) return

        try {
            val store = WearApp.instance.store
            val accounts = store.parsePayload(messageEvent.data)
            Log.d(TAG, "Received ${accounts.size} accounts from phone")
            store.updateFromSyncAccounts(accounts)

            try {
                val updater = androidx.wear.tiles.TileService.getUpdater(this)
                updater.requestUpdate(UsageTileService::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "Could not request tile update", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    companion object {
        private const val TAG = "DataLayerListener"
        private const val PATH_USAGE = "/claude_usage"
    }
}
```

- [ ] **Step 4: Update WearApp.kt**

```kotlin
package dev.qwertyfinger.quotanoia.wear

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class WearApp :
    Application(),
    MessageClient.OnMessageReceivedListener {

    lateinit var store: WearAccountStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = WearAccountStore(this)

        Wearable.getMessageClient(this).addListener(this)
        Log.d(TAG, "MessageClient listener registered")
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: path=${messageEvent.path}, from=${messageEvent.sourceNodeId}")

        if (messageEvent.path != PATH_USAGE) return

        try {
            val accounts = store.parsePayload(messageEvent.data)
            Log.d(TAG, "Parsed ${accounts.size} accounts from phone")
            store.updateFromSyncAccounts(accounts)

            try {
                val updater = androidx.wear.tiles.TileService.getUpdater(this)
                updater.requestUpdate(UsageTileService::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "Could not request tile update", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    companion object {
        private const val TAG = "WearApp"
        private const val PATH_USAGE = "/claude_usage"
        lateinit var instance: WearApp
            private set
    }
}
```

- [ ] **Step 5: Verify wear compiles**

```bash
./gradlew :wear:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Update wear module to use shared data models and kotlinx.serialization"
```

---

### Task 8: Update tests

**Files:**
- Modify: `app/src/test/kotlin/dev/qwertyfinger/quotanoia/data/ApiClientParseTest.kt`
- Modify: `app/src/test/kotlin/dev/qwertyfinger/quotanoia/data/ModelsTest.kt`

- [ ] **Step 1: Update ApiClientParseTest.kt imports**

Replace all `org.json.JSONObject` usage in test fixtures with raw JSON strings. Update the import for `ApiClient` and models:

- `dev.qwertyfinger.quotanoia.data.ApiClient` → `dev.qwertyfinger.quotanoia.data.api.ApiClient`
- `dev.qwertyfinger.quotanoia.data.UsageFetchResult` → `dev.qwertyfinger.quotanoia.data.api.UsageFetchResult`
- `dev.qwertyfinger.quotanoia.data.AccountError` → `dev.qwertyfinger.quotanoia.data.model.AccountError`
- Remove `import org.json.JSONObject`

For `parseWindow()` tests that used `JSONObject` to build test input: the new `ApiClient` no longer has a public `parseWindow()`. These tests should test `parseUsage()` instead — which already has comprehensive coverage for null/malformed/missing resets_at from the earlier test rewrite. Remove the direct `parseWindow()` tests.

- [ ] **Step 2: Update ModelsTest.kt imports**

Replace model imports:
- `dev.qwertyfinger.quotanoia.data.*` → `dev.qwertyfinger.quotanoia.data.model.*`

- [ ] **Step 3: Run all tests**

```bash
./gradlew testDebugUnitTest
```

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Update tests for data module imports"
```

---

### Task 9: Run full verification, detekt, and push

**Files:** None (verification only)

- [ ] **Step 1: Run detekt with auto-correct**

```bash
./gradlew detekt --auto-correct
```

Fix any remaining violations manually if auto-correct doesn't handle them.

- [ ] **Step 2: Run full build pipeline**

```bash
./gradlew clean detekt :app:lintDebug :wear:lintDebug testDebugUnitTest assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Squash into clean commits**

```bash
git reset --soft origin/main
git commit -m "Migrate to kotlinx.serialization and extract data module

- Create data module with @Serializable models, DTOs, repository, API client
- Replace all org.json manual parsing with kotlinx.serialization
- Phone-to-watch sync uses shared @Serializable WearSyncAccount DTOs
- AccountStore uses Json.encodeToString/decodeFromString for persistence
- ApiClient parses responses via Json.decodeFromString<UsageResponse>
- Both app and wear depend on the shared data module
- Remove org.json dependency entirely"
```

- [ ] **Step 4: Generate bot token and push**

```bash
BOT_TOKEN=$(node -e "
const jwt = require('/workspace/group/node_modules/jsonwebtoken');
const fs = require('fs');
const pem = fs.readFileSync('/credentials/personal-dev/github-app-key.pem', 'utf8');
const token = jwt.sign({ iss: '3305507' }, pem, { algorithm: 'RS256', expiresIn: 600 });
const https = require('https');
const req = https.request({
  hostname: 'api.github.com',
  path: '/app/installations/122127464/access_tokens',
  method: 'POST',
  headers: { 'Authorization': 'Bearer ' + token, 'Accept': 'application/vnd.github+json', 'User-Agent': 'node' }
}, res => { let d=''; res.on('data',c=>d+=c); res.on('end',()=>console.log(JSON.parse(d).token)); });
req.end();
")

git remote set-url origin "https://x-access-token:${BOT_TOKEN}@github.com/crushingdigits/quotanoia.git"
git push -u origin refactor/kotlinx-serialization
```

- [ ] **Step 5: Create PR**

```bash
GH_TOKEN="$BOT_TOKEN" gh pr create --repo crushingdigits/quotanoia \
  --base main \
  --head refactor/kotlinx-serialization \
  --title "Migrate to kotlinx.serialization and extract data module" \
  --body "## Summary
- Create \`data\` module: models, API client, account store, repository, wear sync
- Replace all \`org.json\` manual parsing with \`kotlinx.serialization\`
- Shared \`@Serializable\` DTOs for phone↔watch sync
- Remove \`org.json\` dependency

## Files
- 7 files moved from \`app\` to \`data\` module
- 4 wear files updated to use shared DTOs
- Tests updated for new imports

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```
