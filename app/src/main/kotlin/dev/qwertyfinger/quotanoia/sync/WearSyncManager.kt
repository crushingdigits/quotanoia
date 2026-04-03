package dev.qwertyfinger.quotanoia.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import timber.log.Timber
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.data.AccountState

/**
 * Pushes account usage data to the paired watch via the Wearable Data Layer.
 * Only sends names + usage percentages + errors — never tokens.
 */
class WearSyncManager(context: Context) {

    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun syncStates(states: List<AccountState>) {
        val request = PutDataMapRequest.create(PATH_USAGE).apply {
            dataMap.putInt("count", states.size)
            dataMap.putLong("timestamp", System.currentTimeMillis())

            states.forEachIndexed { i, state ->
                dataMap.putString("${i}_id", state.account.id)
                dataMap.putString("${i}_name", state.account.name)

                if (state.usage != null) {
                    dataMap.putFloat("${i}_5h_pct", state.usage.fiveHour.utilization)
                    dataMap.putFloat("${i}_7d_pct", state.usage.sevenDay.utilization)
                    dataMap.putBoolean("${i}_has_data", true)
                } else {
                    dataMap.putBoolean("${i}_has_data", false)
                }

                dataMap.putBoolean("${i}_has_error", state.hasError)
                dataMap.putString("${i}_error_msg", state.error?.displayMessage ?: "")
            }
        }
        request.setUrgent()

        dataClient.putDataItem(request.asPutDataRequest())
            .addOnSuccessListener { Timber.d("Synced %d accounts to watch", states.size) }
            .addOnFailureListener { Timber.w(it, "Watch sync failed") }
    }

    companion object {
        const val PATH_USAGE = "/claude_usage"
    }
}
