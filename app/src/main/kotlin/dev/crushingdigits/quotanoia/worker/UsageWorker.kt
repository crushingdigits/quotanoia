package dev.crushingdigits.quotanoia.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.crushingdigits.quotanoia.QuotanoiaApp
import dev.crushingdigits.quotanoia.core.coroutines.runCatchingCancellable
import dev.crushingdigits.quotanoia.widget.UsageWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that fetches usage for all accounts every 15 minutes.
 *
 * Uses PeriodicWorkRequest — survives process death and doesn't rely on
 * a self-rescheduling chain that can break silently.
 */
class UsageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @Suppress("InjectDispatcher")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val app = QuotanoiaApp.instance
            app.repository.initialize()
            app.repository.refreshAll()
            UsageWidgetReceiver.updateAll(applicationContext)
            Timber.d("Usage fetched for all accounts")
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { e ->
                Timber.e(e, "Worker failed")
                Result.retry()
            },
        )
    }

    companion object {
        private const val WORK_NAME = "claude_usage_poll"
        private const val POLL_INTERVAL_MINUTES = 15L

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageWorker>(
                POLL_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).addTag(WORK_NAME)
                .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
