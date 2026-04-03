package dev.qwertyfinger.quotanoia

import android.app.Application
import dev.qwertyfinger.quotanoia.data.AccountStore
import dev.qwertyfinger.quotanoia.data.ApiClient
import dev.qwertyfinger.quotanoia.data.UsageRepository
import dev.qwertyfinger.quotanoia.sync.WearSyncManager
import dev.qwertyfinger.quotanoia.worker.UsageWorker

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
