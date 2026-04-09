package dev.crushingdigits.quotanoia.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WearSyncAccount(
    val id: String,
    val name: String,
    @SerialName("has_data") val hasData: Boolean,
    @SerialName("5h_pct") val fiveHourPct: Double = -1.0,
    @SerialName("7d_pct") val sevenDayPct: Double = -1.0,
    @SerialName("5h_resets_at") val fiveHourResetsAtMs: Long = 0L,
    @SerialName("7d_resets_at") val sevenDayResetsAtMs: Long = 0L,
    @SerialName("has_error") val hasError: Boolean = false,
    @SerialName("error_msg") val errorMsg: String = "",
)
