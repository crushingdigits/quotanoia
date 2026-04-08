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
