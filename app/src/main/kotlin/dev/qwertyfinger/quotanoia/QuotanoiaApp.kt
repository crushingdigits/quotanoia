package dev.qwertyfinger.quotanoia

import android.app.Application
import android.util.Log
import dev.qwertyfinger.quotanoia.data.AccountStore
import dev.qwertyfinger.quotanoia.data.ApiClient
import dev.qwertyfinger.quotanoia.data.UsageRepository
import dev.qwertyfinger.quotanoia.sync.WearSyncManager
import dev.qwertyfinger.quotanoia.worker.UsageWorker
import timber.log.Timber

class QuotanoiaApp : Application() {

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

        UsageWorker.enqueueNext(this)
    }

    companion object {
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
