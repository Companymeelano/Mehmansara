package ir.ahvaz.mehra.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.map
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.core.common.Validators
import ir.ahvaz.mehra.core.network.ApiExecutor
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.data.mapper.toDomain
import ir.ahvaz.mehra.data.remote.api.GuestsApi
import ir.ahvaz.mehra.data.remote.dto.CompanionDto
import ir.ahvaz.mehra.data.remote.dto.GuestDetailDto
import ir.ahvaz.mehra.data.remote.dto.GuestDraftDto
import ir.ahvaz.mehra.data.remote.dto.GuestDto
import ir.ahvaz.mehra.data.sync.OfflineQueue
import ir.ahvaz.mehra.data.sync.ResponseCache
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.GuestDetail
import ir.ahvaz.mehra.domain.model.GuestDraft
import ir.ahvaz.mehra.domain.repository.GuestRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuestRepositoryImpl @Inject constructor(
    private val api: GuestsApi,
    executor: ApiExecutor,
    cache: ResponseCache,
    queue: OfflineQueue,
    gson: Gson,
    networkMonitor: NetworkMonitor,
) : BaseRepository(executor, cache, queue, gson, networkMonitor), GuestRepository {

    private val listType = object : TypeToken<List<GuestDto>>() {}.type

    override suspend fun guests(query: String): AppResult<CachedData<List<Guest>>> {
        val key = "guests:${query.trim()}"
        return readThrough(key, listType) {
            executor.value {
                api.list(search = query.trim().takeIf { it.isNotEmpty() }, page = 1, pageSize = 200)
            }.map { it.items.orEmpty() }
        }.mapCached { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun guest(guestId: String): AppResult<GuestDetail> =
        executor.value { api.get(guestId) }.map { it.toDomain() }

    override suspend fun save(draft: GuestDraft): AppResult<Guest> {
        val validationError = validate(draft)
        if (validationError != null) return AppResult.Failure(validationError)
        val dto = draft.toDto()
        return if (draft.id == null) {
            executor.value { api.create(UUID.randomUUID().toString(), dto) }.map { it.toDomain() }
        } else {
            executor.value { api.update(draft.id, dto) }.map { it.toDomain() }
        }
    }

    override suspend fun quickSearch(query: String): AppResult<List<Guest>> =
        when (val result = guests(query)) {
            is AppResult.Success -> AppResult.Success(result.data.data)
            is AppResult.Failure -> result
        }

    private fun validate(draft: GuestDraft): AppError? {
        val fields = mutableMapOf<String, String>()
        if (draft.firstName.isBlank()) fields["firstName"] = "نام الزامی است."
        if (draft.lastName.isBlank()) fields["lastName"] = "نام خانوادگی الزامی است."
        if (draft.phone.isNotBlank() && !Validators.isValidIranMobile(draft.phone)) {
            fields["phone"] = "شماره تماس معتبر نیست (مثال: ۰۹۱۲۳۴۵۶۷۸۹)."
        }
        if (draft.nationalId.isNotBlank() && !Validators.isValidIranNationalId(draft.nationalId)) {
            fields["nationalId"] = "کد ملی معتبر نیست."
        }
        return if (fields.isEmpty()) null else AppError.validation("اطلاعات مهمان کامل نیست.", fields)
    }

    private fun GuestDraft.toDto(): GuestDraftDto = GuestDraftDto(
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        nationalId = nationalId.trim().takeIf { it.isNotEmpty() },
        phone = phone.trim().takeIf { it.isNotEmpty() },
        city = city.trim().takeIf { it.isNotEmpty() },
        address = address.trim().takeIf { it.isNotEmpty() },
        notes = notes.trim().takeIf { it.isNotEmpty() },
        companions = companions.map {
            CompanionDto(
                id = it.id,
                fullName = it.fullName,
                nationalId = it.nationalId,
                relation = it.relation,
            )
        },
    )
}
