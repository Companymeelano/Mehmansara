package ir.ahvaz.mehra.demo

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.RealtimeEvent
import ir.ahvaz.mehra.domain.model.RealtimeStatus
import ir.ahvaz.mehra.domain.repository.NotificationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifications and the "realtime" indicator are simulated: the showcase build reports a
 * connected hub so the live badge can be demonstrated, but nothing leaves the device.
 */
@Singleton
class DemoNotificationRepository @Inject constructor(
    private val store: DemoStore,
) : NotificationRepository {

    private val unreadCount = MutableStateFlow(store.unreadNotifications())
    private val status = MutableStateFlow(
        RealtimeStatus(isConnected = true, lastEventAt = Instant.now())
    )
    private val events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 8, replay = 0)

    override suspend fun notifications(unreadOnly: Boolean): AppResult<CachedData<List<NotificationItem>>> {
        delay(120)
        val list = store.notifications
            .filter { !unreadOnly || !it.isRead }
            .sortedByDescending { it.createdAt }
        return demoCached(list)
    }

    override suspend fun markRead(notificationId: String): AppResult<Unit> {
        val index = store.notifications.indexOfFirst { it.id == notificationId }
        if (index < 0) return demoNotFound("اعلان مورد نظر یافت نشد.")
        store.notifications[index] = store.notifications[index].copy(isRead = true)
        unreadCount.value = store.unreadNotifications()
        return AppResult.Success(Unit)
    }

    override suspend fun markAllRead(): AppResult<Unit> {
        store.notifications.indices.forEach { index ->
            store.notifications[index] = store.notifications[index].copy(isRead = true)
        }
        unreadCount.value = 0
        return AppResult.Success(Unit)
    }

    override fun observeUnreadCount(): Flow<Int> = unreadCount.asStateFlow()

    override fun realtimeEvents(): Flow<RealtimeEvent> = events.asSharedFlow()

    override fun realtimeStatus(): StateFlow<RealtimeStatus> = status.asStateFlow()

    override fun startRealtime() {
        status.value = RealtimeStatus(isConnected = true, lastEventAt = Instant.now())
    }

    override fun stopRealtime() {
        status.value = status.value.copy(isConnected = false)
    }

    @Suppress("unused")
    private fun sharedEvents(): SharedFlow<RealtimeEvent> = events.asSharedFlow()
}
