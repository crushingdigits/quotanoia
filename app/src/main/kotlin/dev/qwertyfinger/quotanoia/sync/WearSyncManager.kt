package dev.qwertyfinger.quotanoia.sync

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.data.AccountState
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Pushes account usage data to the paired watch via MessageClient.
 * More reliable than DataClient for sideloaded debug builds.
 * Only sends names + usage percentages + errors — never tokens.
 */
class WearSyncManager(context: Context) {

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    fun syncStates(states: List<AccountState>) {
        val payload = serializeStates(states)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Timber.d("Connected wearable nodes: %d", nodes.size)
                for (node in nodes) {
                    Timber.d("Sending usage to %s (id=%s)", node.displayName, node.id)
                    messageClient.sendMessage(node.id, PATH_USAGE, payload)
                        .addOnSuccessListener {
                            Timber.d("Message sent to %s", node.displayName)
                        }
                        .addOnFailureListener {
                            Timber.w(it, "Failed to send message to %s", node.displayName)
                        }
                }
            }
            .addOnFailureListener { Timber.w(it, "Failed to get connected nodes") }
    }

    private fun serializeStates(states: List<AccountState>): ByteArray {
        val arr = JSONArray()
        states.forEach { state ->
            arr.put(JSONObject().apply {
                put("id", state.account.id)
                put("name", state.account.name)
                put("has_data", state.usage != null)
                if (state.usage != null) {
                    put("5h_pct", state.usage.fiveHour.utilization.toDouble())
                    put("7d_pct", state.usage.sevenDay.utilization.toDouble())
                    put("5h_resets_at", state.usage.fiveHour.resetsAt.toEpochMilli())
                    put("7d_resets_at", state.usage.sevenDay.resetsAt.toEpochMilli())
                }
                put("has_error", state.hasError)
                put("error_msg", state.error?.displayMessage ?: "")
            })
        }
        return arr.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val PATH_USAGE = "/claude_usage"
    }
}
