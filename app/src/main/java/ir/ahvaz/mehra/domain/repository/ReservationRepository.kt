package ir.ahvaz.mehra.domain.repository

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.AvailabilityQuery
import ir.ahvaz.mehra.domain.model.AvailableRoom
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationCalendarEntry
import ir.ahvaz.mehra.domain.model.ReservationDraft
import ir.ahvaz.mehra.domain.model.ReservationQuote
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.WaitlistEntry
import java.time.LocalDate

data class ReservationFilter(
    val query: String = "",
    val status: ReservationStatus? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
)

interface ReservationRepository {
    suspend fun list(filter: ReservationFilter = ReservationFilter()): AppResult<CachedData<List<Reservation>>>
    suspend fun get(reservationId: String): AppResult<Reservation>
    suspend fun availability(query: AvailabilityQuery): AppResult<List<AvailableRoom>>
    suspend fun quote(
        roomId: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guestsCount: Int,
        discountRial: Long,
    ): AppResult<ReservationQuote>
    suspend fun create(draft: ReservationDraft): AppResult<Reservation>
    suspend fun confirm(reservationId: String): AppResult<Unit>
    suspend fun cancel(reservationId: String, reason: String?): AppResult<Unit>
    suspend fun waitlist(): AppResult<CachedData<List<WaitlistEntry>>>
    suspend fun promoteWaitlist(waitlistId: String, roomId: String): AppResult<Reservation>
    suspend fun calendar(from: LocalDate, to: LocalDate): AppResult<List<ReservationCalendarEntry>>
}
