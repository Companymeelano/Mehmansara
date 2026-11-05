package ir.ahvaz.mehra.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.core.datastore.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Background drain of the offline outbox plus cache housekeeping.
 *
 * Enqueued one-shot when connectivity returns and periodically (15 min, the platform
 * minimum) as a safety net.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxProcessor: OutboxProcessor,
    private val settingsRepository: SettingsRepository,
    private val responseCache: ResponseCache,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val result = outboxProcessor.process()
        responseCache.purgeExpired()
        settingsRepository.markSynced(System.currentTimeMillis())
        if (result.remaining > 0) Result.retry() else Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }

    companion object {

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Called whenever the device regains connectivity. */
        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${Constants.SYNC_WORK_NAME}-now",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /** Registered once at app start. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
