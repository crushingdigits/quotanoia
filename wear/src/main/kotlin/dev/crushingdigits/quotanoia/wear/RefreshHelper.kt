@file:Suppress("ChainMethodContinuation")

package dev.crushingdigits.quotanoia.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import timber.log.Timber

/**
 * Sends a refresh request message to the paired phone.
 * The phone's QuotanoiaApp will receive it and trigger a full usage refresh.
 */
object RefreshHelper {

    private const val PATH_REFRESH_REQUEST = "/refresh_request"

    fun requestRefreshFromPhone(context: Context) {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Timber.d("Requesting refresh from %s", node.displayName)
                    messageClient
                        .sendMessage(node.id, PATH_REFRESH_REQUEST, ByteArray(0))
                        .addOnSuccessListener {
                            Timber.d("Refresh request sent to %s", node.displayName)
                        }
                        .addOnFailureListener {
                            Timber.w(it, "Failed to send refresh request")
                        }
                }
            }
            .addOnFailureListener {
                Timber.w(it, "Failed to get connected nodes")
            }
    }
}
