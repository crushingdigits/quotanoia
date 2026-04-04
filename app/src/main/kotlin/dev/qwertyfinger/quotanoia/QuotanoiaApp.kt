package dev.qwertyfinger.quotanoia

import android.app.Application
import android.util.Log
import dev.qwertyfinger.quotanoia.data.AccountStore
import dev.qwertyfinger.quotanoia.data.ApiClient
import dev.qwertyfinger.quotanoia.data.UsageRepository
import dev.qwertyfinger.quotanoia.data.history.QuotanoiaDatabase
import dev.qwertyfinger.quotanoia.data.history.UsageHistoryDao
import dev.qwertyfinger.quotanoia.notification.NotificationChannelManager
import dev.qwertyfinger.quotanoia.notification.NotificationPrefsStore
import dev.qwertyfinger.quotanoia.sync.WearSyncManager
import dev.qwertyfinger.quotanoia.worker.UsageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit

class QuotanoiaApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var store: AccountStore
        private set
    lateinit var repository: UsageRepository
        private set
    lateinit var wearSync: WearSyncManager
        private set
    lateinit var database: QuotanoiaDatabase
        private set
    lateinit var notificationPrefs: NotificationPrefsStore
        private set
    val historyDao: UsageHistoryDao
        get() = database.usageHistoryDao()

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        NotificationChannelManager.createChannels(this)

        database = QuotanoiaDatabase.getInstance(this)
        store = AccountStore(this)
        wearSync = WearSyncManager(this)
        notificationPrefs = NotificationPrefsStore(this)
        repository = UsageRepository(store, ApiClient(), wearSync, database.usageHistoryDao())
        repository.initialize()

        UsageWorker.enqueueNext(this)

        // Purge records older than 90 days on each app launch
        appScope.launch {
            try {
                val cutoff = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli()
                database.usageHistoryDao().deleteOlderThan(cutoff)
                Timber.d("Purged usage history records older than 90 days")
            } catch (e: Exception) {
                Timber.w(e, "Failed to purge old history records")
            }
        }
    }

    companion object {
        lateinit var instance: QuotanoiaApp
            private set
    }
}

/** Only logs Info, Warn, Error, and WTF in release builds. */
private class ReleaseTree : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.println(priority, tag ?: "quotanoia", message)
    }
}
