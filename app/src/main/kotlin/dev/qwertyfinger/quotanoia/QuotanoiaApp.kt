@file:Suppress("FunctionSignature", "ClassSignature")

package dev.qwertyfinger.quotanoia

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dev.qwertyfinger.quotanoia.data.AccountStore
import dev.qwertyfinger.quotanoia.data.ApiClient
import dev.qwertyfinger.quotanoia.data.UsageRepository
import dev.qwertyfinger.quotanoia.sync.WearSyncManager
import dev.qwertyfinger.quotanoia.widget.UsageWidgetReceiver
import dev.qwertyfinger.quotanoia.worker.UsageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class QuotanoiaApp : Application(), MessageClient.OnMessageReceivedListener {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var store: AccountStore
        private set
    lateinit var repository: UsageRepository
        private set
    lateinit var wearSync: WearSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        store = AccountStore(this)
        wearSync = WearSyncManager(this)
        repository = UsageRepository(store, ApiClient(), wearSync)
        repository.initialize()

        // Listen for refresh requests from the watch
        Wearable.getMessageClient(this).addListener(this)
        Timber.d("Phone MessageClient listener registered")

        UsageWorker.enqueueNext(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Timber.d("Message from watch: path=%s", messageEvent.path)
        if (messageEvent.path == PATH_REFRESH_REQUEST) {
            Timber.d("Watch requested refresh — fetching all accounts")
            appScope.launch {
                repository.refreshAll()
                UsageWidgetReceiver.updateAll(this@QuotanoiaApp)
            }
        }
    }

    companion object {
        const val PATH_REFRESH_REQUEST = "/refresh_request"
        lateinit var instance: QuotanoiaApp
            private set
    }
}

/** Only logs Info, Warn, Error, and WTF in release builds. */
private class ReleaseTree : Timber.Tree() {
    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority >= Log.INFO

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        Log.println(priority, tag ?: "quotanoia", message)
    }
}
