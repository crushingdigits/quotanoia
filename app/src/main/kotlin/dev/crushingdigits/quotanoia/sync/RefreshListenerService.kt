package dev.crushingdigits.quotanoia.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dev.crushingdigits.quotanoia.QuotanoiaApp
import dev.crushingdigits.quotanoia.widget.UsageWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Receives refresh requests from the watch via the Wearable MessageClient.
 * Declared in the manifest so Android starts it even when the app process is dead.
 */
class RefreshListenerService : WearableListenerService() {

    @Suppress("InjectDispatcher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Timber.d("Message from watch: path=%s", messageEvent.path)
        if (messageEvent.path != PATH_REFRESH_REQUEST) return

        Timber.d("Watch requested refresh — fetching all accounts")
        val app = QuotanoiaApp.instance
        app.repository.initialize()
        scope.launch {
            app.repository.refreshAll()
            UsageWidgetReceiver.updateAll(applicationContext)
        }
    }

    companion object {
        const val PATH_REFRESH_REQUEST = "/refresh_request"
    }
}
