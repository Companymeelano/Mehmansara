package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.RealtimeEvent
import ir.ahvaz.mehra.domain.model.RealtimeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    suspend fun notifications(unreadOnly: Boolean = false): AppResult<CachedData<List<NotificationItem>>>
    suspend fun markRead(notificationId: String): AppResult<Unit>
    suspend fun markAllRead(): AppResult<Unit>
    fun observeUnreadCount(): Flow<Int>
    fun realtimeEvents(): Flow<RealtimeEvent>
    fun realtimeStatus(): StateFlow<RealtimeStatus>
    fun startRealtime()
    fun stopRealtime()
}
