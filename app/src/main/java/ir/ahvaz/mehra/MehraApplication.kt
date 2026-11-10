package ir.ahvaz.mehra

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.ahvaz.mehra.core.datastore.SettingsRepository
import ir.ahvaz.mehra.core.diagnostics.CrashRecorder
import ir.ahvaz.mehra.core.di.ApplicationScope
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.notifications.NotificationChannels
import ir.ahvaz.mehra.core.notifications.NotificationPublisher
import ir.ahvaz.mehra.core.security.AppLockManager
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.SyncWorker
import ir.ahvaz.mehra.domain.model.RealtimeEvent
import ir.ahvaz.mehra.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MehraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationPublisher: NotificationPublisher

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var offlineQueue: OfflineQueue

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        // Earliest possible hook: capture even a crash inside Hilt injection / super.onCreate.
        CrashRecorder.install(base)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        CrashRecorder.install(this)
        super.onCreate()

        NotificationChannels.create(this)
        SyncWorker.schedulePeriodic(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appLockManager.onAppForegrounded()
            }

            override fun onStop(owner: LifecycleOwner) {
                appLockManager.onAppBackgrounded()
            }
        })

        // Drain the outbox as soon as the network comes back.
        appScope.launch {
            networkMonitor.isOnline
                .collect { online ->
                    if (online && sessionManager.isLoggedIn) {
                        SyncWorker.enqueueImmediate(this@MehraApplication)
                    }
                }
        }

        // Realtime channel lifecycle follows the operator setting and the session.
        appScope.launch {
            settingsRepository.settings
                .map { it.realtimeEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled && sessionManager.isLoggedIn) {
                        notificationRepository.startRealtime()
                    } else {
                        notificationRepository.stopRealtime()
                    }
                }
        }

        // Surface hub events as system notifications when the app is not in the foreground.
        appScope.launch {
            combine(
                notificationRepository.realtimeEvents(),
                settingsRepository.settings.map { it.notificationsEnabled }.distinctUntilChanged(),
            ) { event, notificationsEnabled -> event to notificationsEnabled }
                .collect { (event, notificationsEnabled) ->
                    if (event is RealtimeEvent.Received) {
                        notificationPublisher.publish(event, notificationsEnabled)
                    }
                }
        }

        // Keep the "awaiting confirmation" badge visible in the notification shade.
        appScope.launch {
            offlineQueue.observePendingCount()
                .distinctUntilChanged()
                .collect { count ->
                    if (count > 0) notificationPublisher.publishPendingSync(count)
                }
        }
    }
}
