package dev.qwertyfinger.quotanoia.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

/**
 * Sends a refresh request message to the paired phone.
 * The phone's QuotanoiaApp will receive it and trigger a full usage refresh.
 */
object RefreshHelper {

    private const val TAG = "RefreshHelper"
    private const val PATH_REFRESH_REQUEST = "/refresh_request"

    fun requestRefreshFromPhone(context: Context) {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Log.d(TAG, "Requesting refresh from ${node.displayName}")
                    messageClient.sendMessage(node.id, PATH_REFRESH_REQUEST, ByteArray(0))
                        .addOnSuccessListener { Log.d(TAG, "Refresh request sent to ${node.displayName}") }
                        .addOnFailureListener { Log.w(TAG, "Failed to send refresh request", it) }
                }
            }
            .addOnFailureListener { Log.w(TAG, "Failed to get connected nodes", it) }
    }
}
