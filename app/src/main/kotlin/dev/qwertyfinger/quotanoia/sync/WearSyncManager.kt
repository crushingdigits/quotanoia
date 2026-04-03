package dev.qwertyfinger.quotanoia.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.data.AccountState
import timber.log.Timber

/**
 * Pushes account usage data to the paired watch via the Wearable Data Layer.
 * Only sends names + usage percentages + errors — never tokens.
 */
class WearSyncManager(context: Context) {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    fun syncStates(states: List<AccountState>) {
        // Log connected nodes for diagnostics
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Timber.d("Connected wearable nodes: %d", nodes.size)
                nodes.forEach { node ->
                    Timber.d("  Node: %s (id=%s, nearby=%s)", node.displayName, node.id, node.isNearby)
                }
            }
            .addOnFailureListener { Timber.w(it, "Failed to get connected nodes") }

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
            .addOnSuccessListener { item ->
                Timber.d("putDataItem succeeded: uri=%s", item.uri)
            }
            .addOnFailureListener { Timber.w(it, "putDataItem failed") }
    }

    companion object {
        const val PATH_USAGE = "/claude_usage"
    }
}
