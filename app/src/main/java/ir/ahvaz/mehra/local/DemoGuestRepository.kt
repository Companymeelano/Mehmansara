package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.GuestDetail
import ir.ahvaz.mehra.domain.model.GuestDraft
import ir.ahvaz.mehra.domain.model.ReservationSummary
import ir.ahvaz.mehra.domain.model.RoomHistoryEntry
import ir.ahvaz.mehra.domain.repository.GuestRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoGuestRepository @Inject constructor(
    private val store: DemoStore,
) : GuestRepository {

    override suspend fun guests(query: String): AppResult<CachedData<List<Guest>>> {
        delay(140)
        val q = query.trim()
        val digits = q.demoDigits()
        val list = store.guests.filter { guest ->
            q.isEmpty() ||
                guest.fullName.contains(q) ||
                guest.city?.contains(q) == true ||
                guest.carPlate?.contains(q) == true ||
                (digits.isNotEmpty() && guest.phone?.contains(digits) == true) ||
                (digits.isNotEmpty() && guest.nationalId?.contains(digits) == true)
        }.sortedBy { it.fullName }
        return demoCached(list)
    }

    override suspend fun guest(guestId: String): AppResult<GuestDetail> {
        delay(140)
        val guest = store.guest(guestId) ?: return demoNotFound("مهمان مورد نظر یافت نشد.")
        val stays = store.stays
            .filter { it.guestId == guestId }
            .map { stay ->
                RoomHistoryEntry(
                    id = stay.id,
                    guestName = stay.guestName,
                    checkIn = store.localDateOf(stay.checkInAt),
                    checkOut = store.localDateOf(stay.actualCheckOutAt) ?: stay.plannedCheckOut,
                    totalRial = stay.totalRial,
                    status = stay.status,
                )
            }
        val reservations = store.reservations
            .filter { it.guestId == guestId }
            .map { reservation ->
                ReservationSummary(
                    id = reservation.id,
                    code = reservation.code,
                    checkIn = reservation.checkIn,
                    checkOut = reservation.checkOut,
                    status = reservation.status,
                    totalRial = reservation.totalRial,
                    roomNumber = reservation.roomNumber,
                )
            }
        val ledger = store.transactions.filter { it.guestName == guest.fullName }
        return demoOk(GuestDetail(guest = guest, stays = stays, reservations = reservations, ledger = ledger))
    }

    override suspend fun save(draft: GuestDraft): AppResult<Guest> {
        delay(220)
        val firstName = draft.firstName.trim()
        val lastName = draft.lastName.trim()
        if (firstName.isEmpty() || lastName.isEmpty()) {
            return demoFailure("نام و نام خانوادگی مهمان را کامل وارد کنید.")
        }
        val phone = draft.phone.demoDigits().ifBlank { null }
        if (phone != null && phone.length < 10) {
            return demoFailure("شماره تماس معتبر نیست.")
        }
        val existing = draft.id?.let { store.guest(it) }
        val guest = Guest(
            id = existing?.id ?: store.nextId("guest"),
            firstName = firstName,
            lastName = lastName,
            nationalId = draft.nationalId.demoDigits().ifBlank { null },
            phone = phone,
            carPlate = draft.carPlate.trim().ifBlank { null },
            city = draft.city.trim().ifBlank { null },
            address = draft.address.trim().ifBlank { null },
            notes = draft.notes.trim().ifBlank { null },
            balanceRial = existing?.balanceRial ?: 0L,
            totalStays = existing?.totalStays ?: 0,
            lastStayAt = existing?.lastStayAt,
            companions = draft.companions,
        )
        val index = store.guests.indexOfFirst { it.id == guest.id }
        if (index >= 0) store.guests[index] = guest else store.guests += guest
        return demoOk(guest)
    }

    override suspend fun quickSearch(query: String): AppResult<List<Guest>> {
        val q = query.trim()
        if (q.isEmpty()) return demoOk(emptyList())
        val digits = q.demoDigits()
        val list = store.guests.filter { guest ->
            guest.fullName.contains(q) ||
                guest.carPlate?.contains(q) == true ||
                (digits.isNotEmpty() && guest.phone?.contains(digits) == true) ||
                (digits.isNotEmpty() && guest.nationalId?.contains(digits) == true)
        }.take(8)
        return demoOk(list)
    }
}
