package dev.qwertyfinger.quotanoia.wear

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives usage data pushed from the phone app via MessageClient.
 * Updates the local cache and requests a tile refresh.
 */
class DataLayerListenerService : WearableListenerService() {

    @Suppress("TooGenericExceptionCaught")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: path=${messageEvent.path}, from=${messageEvent.sourceNodeId}")

        if (messageEvent.path != PATH_USAGE) return

        try {
            val store = WearApp.instance.store
            val accounts = store.parsePayload(messageEvent.data)
            Log.d(TAG, "Received ${accounts.size} accounts from phone")

            store.updateFromSyncAccounts(accounts)

            // Request tile update
            try {
                val updater = androidx.wear.tiles.TileService
                    .getUpdater(this)
                updater.requestUpdate(UsageTileService::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "Could not request tile update", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    companion object {
        private const val TAG = "DataLayerListener"
        private const val PATH_USAGE = "/claude_usage"
    }
}
