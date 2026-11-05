package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.GuestDetail
import ir.ahvaz.mehra.domain.model.GuestDraft

interface GuestRepository {
    suspend fun guests(query: String = ""): AppResult<CachedData<List<Guest>>>
    suspend fun guest(guestId: String): AppResult<GuestDetail>
    suspend fun save(draft: GuestDraft): AppResult<Guest>
    suspend fun quickSearch(query: String): AppResult<List<Guest>>
}
