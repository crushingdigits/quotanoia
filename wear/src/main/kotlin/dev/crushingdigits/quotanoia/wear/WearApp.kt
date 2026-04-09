package dev.crushingdigits.quotanoia.wear

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class WearApp :
    Application(),
    MessageClient.OnMessageReceivedListener {

    lateinit var store: WearAccountStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = WearAccountStore(this)

        // Register as message listener — more reliable than manifest-declared service
        Wearable.getMessageClient(this).addListener(this)
        Log.d(TAG, "MessageClient listener registered")
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(
            TAG,
            "Message received: path=${messageEvent.path}, " +
                "from=${messageEvent.sourceNodeId}, size=${messageEvent.data.size}",
        )

        if (messageEvent.path != PATH_USAGE) return

        try {
            val accounts = store.parsePayload(messageEvent.data)
            Log.d(TAG, "Parsed ${accounts.size} accounts from phone")
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
        private const val TAG = "WearApp"
        private const val PATH_USAGE = "/claude_usage"
        lateinit var instance: WearApp
            private set
    }
}
