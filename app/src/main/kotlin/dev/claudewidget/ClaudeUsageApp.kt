package dev.claudewidget

import android.app.Application
import dev.claudewidget.data.AccountStore
import dev.claudewidget.data.ApiClient
import dev.claudewidget.data.UsageRepository
import dev.claudewidget.sync.WearSyncManager
import dev.claudewidget.worker.UsageWorker

class ClaudeUsageApp : Application() {

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
        lateinit var instance: ClaudeUsageApp
            private set
    }
}
