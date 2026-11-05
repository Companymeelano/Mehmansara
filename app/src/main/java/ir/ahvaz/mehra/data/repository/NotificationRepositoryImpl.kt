package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.di.ApplicationScope
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.network.realtime.SignalRClient
import ir.ahvaz.mehra.data.local.dao.NotificationDao
import ir.ahvaz.mehra.data.local.entity.CachedNotificationEntity
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.SystemApi
import ir.ahvaz.mehra.data.remote.dto.NotificationDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.ActivityKind
import ir.ahvaz.mehra.domain.model.AlertSeverity
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.RealtimeEvent
import ir.ahvaz.mehra.domain.model.RealtimeStatus
import ir.ahvaz.mehra.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: SystemApi,
    private val notificationDao: NotificationDao,
    private val signalRClient: SignalRClient,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
    @ApplicationScope private val appScope: CoroutineScope,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), NotificationRepository {

    private val listType = object : TypeToken<List<NotificationDto>>() {}.type

    init {
        // Push notifications received over SignalR are stored locally so the badge and the
        // notification centre stay consistent even while the app is in the background.
        appScope.launch {
            signalRClient.events.collect { event ->
                if (event is RealtimeEvent.Received) {
                    runCatching {
                        notificationDao.upsertAll(
                            listOf(
                                CachedNotificationEntity(
                                    id = "${event.target}-${event.receivedAt.toEpochMilli()}",
                                    title = event.title,
                                    body = event.body,
                                    severity = AlertSeverity.INFO.wire,
                                    kind = event.kind.name,
                                    createdAtMillis = event.receivedAt.toEpochMilli(),
                                    isRead = false,
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun notifications(unreadOnly: Boolean): AppResult<CachedData<List<NotificationItem>>> {
        val result = readThrough("notifications", listType) {
            executor.value { api.notifications(unreadOnly) }
        }
        return when (result) {
            is AppResult.Success -> {
                val items = result.data.data
                runCatching {
                    notificationDao.upsertAll(
                        items.mapNotNull { dto ->
                            val id = dto.id ?: return@mapNotNull null
                            CachedNotificationEntity(
                                id = id,
                                title = dto.title.orEmpty(),
                                body = dto.body,
                                severity = dto.severity ?: AlertSeverity.INFO.wire,
                                kind = dto.kind ?: ActivityKind.SYSTEM.name,
                                createdAtMillis = ir.ahvaz.mehra.core.date.IsoFormat.parseInstant(dto.createdAtUtc)?.toEpochMilli(),
                                isRead = dto.isRead ?: false,
                            )
                        }
                    )
                }
                AppResult.Success(
                    CachedData(
                        data = items.map { it.toDomain() },
                        updatedAtMillis = result.data.updatedAtMillis,
                        fromCache = result.data.fromCache,
                    )
                )
            }

            is AppResult.Failure -> {
                val local = runCatching {
                    notificationDao.observeAll().let { flow ->
                        flow.first()
                    }
                }.getOrNull().orEmpty()
                if (local.isEmpty()) result
                else AppResult.Success(
                    CachedData(
                        data = local.map {
                            NotificationItem(
                                id = it.id,
                                title = it.title,
                                body = it.body,
                                severity = AlertSeverity.fromWire(it.severity),
                                createdAt = it.createdAtMillis?.let { m -> Instant.ofEpochMilli(m) },
                                isRead = it.isRead,
                                kind = ActivityKind.fromWire(it.kind),
                            )
                        },
                        updatedAtMillis = local.first().createdAtMillis ?: 0L,
                        fromCache = true,
                    )
                )
            }
        }
    }

    override suspend fun markRead(notificationId: String): AppResult<Unit> {
        val result = executor.unit { api.markRead(notificationId) }
        if (result is AppResult.Success) {
            runCatching { notificationDao.markRead(notificationId) }
        }
        return result
    }

    override suspend fun markAllRead(): AppResult<Unit> {
        val result = executor.unit { api.markAllRead() }
        if (result is AppResult.Success) {
            runCatching { notificationDao.markAllRead() }
        }
        return result
    }

    override fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    override fun realtimeEvents(): Flow<RealtimeEvent> = signalRClient.events

    override fun realtimeStatus(): StateFlow<RealtimeStatus> = signalRClient.status

    override fun startRealtime() {
        signalRClient.start()
    }

    override fun stopRealtime() {
        signalRClient.stop()
    }
}
