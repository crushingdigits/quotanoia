# kotlinx.serialization Migration + Data Module Extraction

## Goal

Replace all manual `org.json` JSON parsing with `kotlinx.serialization` and extract the data layer into a shared `data` module that both `app` and `wear` depend on.

## Module Structure

```
quotanoia/
├── app/          → phone UI, ViewModel, widget, worker
├── wear/         → watch UI, tile, services
├── data/         → NEW: repository, API client, account store, models, sync
├── core/
│   └── coroutines/
└── build-logic/
```

### `data` module (Android library)

Contains:
- `model/` — `@Serializable` data classes: `ClaudeAccount`, `UsageData`, `UsageWindow`, `AccountError`, `AccountState`, API response DTOs (`UsageResponse`, `WindowResponse`), sync payload DTO
- `api/ApiClient.kt` — HTTP calls + `Json.decodeFromString<UsageResponse>()` parsing
- `store/AccountStore.kt` — EncryptedSharedPreferences, serialize with `Json.encodeToString()`
- `repository/UsageRepository.kt` — orchestrates fetch + cache + state
- `sync/WearSyncManager.kt` — phone→watch MessageClient sync using `Json.encodeToString()`

Dependencies: `kotlinx-serialization-json`, `okhttp`, `okhttp-logging`, `security-crypto`, `play-services-wearable`, `timber`, `core:coroutines`

### `app` module changes

- Add `implementation(projects.data)`, remove `okhttp`, `security-crypto`, `play-services-wearable`
- Update imports from `dev.qwertyfinger.quotanoia.data` to `dev.qwertyfinger.quotanoia.data.*` (package stays same if data module uses same package)

### `wear` module changes

- Add `implementation(projects.data)` for shared models + deserialization
- `WearAccountStore.kt` stays in wear (read-only cache, plain SharedPreferences) but uses shared `@Serializable` models
- `DataLayerListenerService.kt` / `WearApp.kt` — replace `JSONObject` parsing with `Json.decodeFromString()`

## Serialization Strategy

### API responses

```kotlin
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

`resetsAt` stays `String?` in DTO, converted to `Instant?` in domain model. No custom serializer needed.

### SharedPreferences cache

Accounts: `Json.encodeToString(List<ClaudeAccount>)` → single string in EncryptedSharedPreferences.
Usage cache: `Json.encodeToString(UsageData)` per account ID.

### Phone→watch sync

Sync payload: `@Serializable` data class with list of account states. Encoded to JSON string → bytes via MessageClient. Watch decodes with `Json.decodeFromString()`.

## Deletions

- Remove `org.json` dependency from all modules
- Remove `testImplementation("org.json:json:20231013")` from app

## Tests

- `ApiClientParseTest.kt` — test JSON strings stay raw, verify `Json.decodeFromString` handles null/malformed/missing fields
- Remove `org.json.JSONObject` usage from test fixtures

## Gradle Changes

- `libs.versions.toml`: add `kotlinx-serialization-json`
- Root `build.gradle.kts`: add `kotlin("plugin.serialization")` apply false
- `data/build.gradle.kts`: new module with `quotanoia.android.library` convention + serialization plugin
- `settings.gradle.kts`: include `:data`

## Branch

`refactor/kotlinx-serialization` off `main`
