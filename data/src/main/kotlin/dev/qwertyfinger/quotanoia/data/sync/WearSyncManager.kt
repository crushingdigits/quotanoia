package dev.qwertyfinger.quotanoia.data.sync

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.data.model.AccountState
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Pushes account usage data to the paired watch via MessageClient.
 * More reliable than DataClient for sideloaded debug builds.
 * Only sends names + usage percentages + errors — never tokens.
 */
class WearSyncManager(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

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
        val dtos = states.map { state ->
            WearSyncAccount(
                id = state.account.id,
                name = state.account.name,
                hasData = state.usage != null,
                fiveHourPct = state.usage
                    ?.fiveHour
                    ?.utilization
                    ?.toDouble() ?: -1.0,
                sevenDayPct = state.usage
                    ?.sevenDay
                    ?.utilization
                    ?.toDouble() ?: -1.0,
                fiveHourResetsAtMs = state.usage
                    ?.fiveHour
                    ?.resetsAt
                    ?.toEpochMilli() ?: 0L,
                sevenDayResetsAtMs = state.usage
                    ?.sevenDay
                    ?.resetsAt
                    ?.toEpochMilli() ?: 0L,
                hasError = state.hasError,
                errorMsg = state.error?.displayMessage.orEmpty(),
            )
        }
        return json.encodeToString(dtos).toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val PATH_USAGE = "/claude_usage"
    }
}
