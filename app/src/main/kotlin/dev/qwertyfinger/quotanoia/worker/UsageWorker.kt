package dev.qwertyfinger.quotanoia.worker

import android.content.Context
import androidx.work.*
import timber.log.Timber
import dev.qwertyfinger.quotanoia.QuotanoiaApp
import dev.qwertyfinger.quotanoia.widget.UsageWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Self-rescheduling worker that fetches usage for all accounts,
 * then enqueues itself with a delay based on the current backoff state.
 *
 * Base interval: 5 minutes. On 429/503 → exponential backoff up to 30 min (or server Retry-After if larger).
 * On success → reset to 5 min.
 */
class UsageWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = QuotanoiaApp.instance
            app.repository.initialize()
            app.repository.refreshAll()

            // Update widgets
            UsageWidgetReceiver.updateAll(applicationContext)

            // Schedule next run based on backoff state
            val delay = app.repository.nextPollDelayMs()
            enqueueNext(applicationContext, delay)

            Timber.d("Usage fetched. Next poll in %ds", delay / 1000)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Worker failed")
            enqueueNext(applicationContext) // retry at base interval
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "claude_usage_poll"

        fun enqueueNext(context: Context, delayMs: Long = dev.qwertyfinger.quotanoia.data.BASE_POLL_INTERVAL_MS) {
            val request = OneTimeWorkRequestBuilder<UsageWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
