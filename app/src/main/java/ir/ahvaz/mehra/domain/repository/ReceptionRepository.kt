package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CheckInCommand
import ir.ahvaz.mehra.domain.model.CheckOutCommand
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.Stay
import java.time.LocalDate

interface ReceptionRepository {
    suspend fun searchReservations(query: String): AppResult<List<Reservation>>
    suspend fun activeStays(query: String = ""): AppResult<CachedData<List<Stay>>>
    suspend fun stay(stayId: String): AppResult<Stay>
    suspend fun checkIn(command: CheckInCommand): AppResult<Stay>
    suspend fun checkOut(command: CheckOutCommand): AppResult<Receipt>
    suspend fun extendStay(stayId: String, newCheckOut: LocalDate): AppResult<Unit>
    suspend fun moveRoom(stayId: String, newRoomId: String, reason: String?): AppResult<Unit>
    suspend fun receipt(stayId: String): AppResult<Receipt>
    suspend fun documentTypes(): AppResult<List<String>>
}
