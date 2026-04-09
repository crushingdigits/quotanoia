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
