package ir.ahvaz.mehra.data.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.MaintenanceApi
import ir.ahvaz.mehra.data.remote.dto.AssignRequestDto
import ir.ahvaz.mehra.data.remote.dto.CostRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateMaintenanceRequestDto
import ir.ahvaz.mehra.data.remote.dto.MaintenanceStatusRequestDto
import ir.ahvaz.mehra.data.remote.dto.MaintenanceTicketDto
import ir.ahvaz.mehra.data.remote.dto.StaffDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.OperationType
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.CreateMaintenanceCommand
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.repository.MaintenanceRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val api: MaintenanceApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), MaintenanceRepository {

    private val ticketListType = object : TypeToken<List<MaintenanceTicketDto>>() {}.type
    private val staffListType = object : TypeToken<List<StaffDto>>() {}.type

    override suspend fun tickets(
        status: MaintenanceStatus?,
        priority: MaintenancePriority?,
    ): AppResult<CachedData<List<MaintenanceTicket>>> =
        readThrough("maintenance:${status?.wire.orEmpty()}:${priority?.wire.orEmpty()}", ticketListType) {
            executor.value { api.tickets(status?.wire, priority?.wire) }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }

    override suspend fun ticket(ticketId: String): AppResult<MaintenanceTicket> =
        executor.value { api.ticket(ticketId) }.map { it.toDomain() }

    override suspend fun technicians(): AppResult<List<StaffMember>> {
        val result = readThrough("maintenance:technicians", staffListType) {
            executor.value { api.technicians() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
        return when (result) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }
    }

    override suspend fun create(command: CreateMaintenanceCommand): AppResult<MaintenanceTicket> {
        if (command.title.isBlank()) {
            return AppResult.Failure(
                AppError.validation("عنوان خرابی الزامی است.", mapOf("title" to "الزامی"))
            )
        }
        val body = CreateMaintenanceRequestDto(
            roomId = command.roomId,
            title = command.title.trim(),
            description = command.description.trim(),
            priority = command.priority.wire,
        )
        val idempotencyKey = UUID.randomUUID().toString()
        val result = executor.value { api.create(idempotencyKey, body) }
        return when (result) {
            is AppResult.Success -> {
                cache.invalidatePrefix("maintenance:")
                cache.invalidatePrefix("rooms:")
                AppResult.Success(result.data.toDomain())
            }

            is AppResult.Failure -> queueForLater(
                error = result.error,
                operationType = OperationType.MAINTENANCE_CREATE,
                method = "POST",
                path = "/api/v1/maintenance/tickets",
                body = body,
                summaryFa = "ثبت خرابی: ${command.title}",
                idempotencyKey = idempotencyKey,
            )
        }
    }

    override suspend fun assign(ticketId: String, technicianId: String): AppResult<Unit> =
        executor.unit { api.assign(ticketId, AssignRequestDto(technicianId)) }

    override suspend fun updateStatus(
        ticketId: String,
        status: MaintenanceStatus,
        note: String?,
    ): AppResult<Unit> =
        executor.unit { api.updateStatus(ticketId, MaintenanceStatusRequestDto(status.wire, note)) }

    override suspend fun registerCost(ticketId: String, amountRial: Long, description: String?): AppResult<Unit> {
        if (amountRial < 0) {
            return AppResult.Failure(AppError.validation("مبلغ هزینه نامعتبر است."))
        }
        return executor.unit { api.registerCost(ticketId, CostRequestDto(amountRial, description)) }
    }

    override suspend fun releaseRoom(ticketId: String): AppResult<Unit> =
        executor.unit { api.releaseRoom(ticketId) }

    override suspend fun uploadPhotos(ticketId: String, localUris: List<String>): AppResult<List<String>> {
        if (localUris.isEmpty()) return AppResult.Success(emptyList())
        val parts = mutableListOf<MultipartBody.Part>()
        for (uri in localUris) {
            val parsed = android.net.Uri.parse(uri)
            val mime = context.contentResolver.getType(parsed) ?: "image/jpeg"
            val bytes = runCatching {
                context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
            }.getOrNull() ?: continue
            val fileName = parsed.lastPathSegment?.substringAfterLast('/') ?: "photo.jpg"
            parts += MultipartBody.Part.createFormData(
                "photos",
                fileName,
                bytes.toRequestBody(mime.toMediaTypeOrNull()),
            )
        }
        if (parts.isEmpty()) {
            return AppResult.Failure(AppError.validation("تصاویر انتخاب‌شده قابل خواندن نبودند."))
        }
        val result = executor.value { api.uploadPhotos(ticketId, parts) }
        return when (result) {
            is AppResult.Success -> {
                cache.invalidatePrefix("maintenance:")
                AppResult.Success(result.data)
            }

            is AppResult.Failure -> result
        }
    }
}
