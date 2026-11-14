package ir.ahvaz.mehra.domain.model

import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.date.nightsBetween
import java.time.Instant
import java.time.LocalDate

data class Reservation(
    val id: String,
    val code: String,
    val guestId: String,
    val guestName: String,
    val guestPhone: String? = null,
    val roomId: String? = null,
    val roomNumber: String? = null,
    val roomTypeName: String? = null,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestsCount: Int = 1,
    val nightlyRateRial: Long = 0L,
    val totalRial: Long = 0L,
    val discountRial: Long = 0L,
    val paidRial: Long = 0L,
    val status: ReservationStatus = ReservationStatus.PENDING,
    val notes: String? = null,
    val createdByName: String? = null,
    val createdAt: Instant? = null,
    val companions: List<Companion> = emptyList(),
) {
    val nights: Int get() = nightsBetween(checkIn, checkOut)
    val payableRial: Long get() = (totalRial - discountRial).coerceAtLeast(0L)
    val remainingRial: Long get() = (payableRial - paidRial).coerceAtLeast(0L)

    /** True when the reservation is still live (not cancelled/expired/finished) and covers [day]. */
    fun isActiveOn(day: LocalDate): Boolean =
        status !in setOf(
            ReservationStatus.CANCELLED,
            ReservationStatus.EXPIRED,
            ReservationStatus.CHECKED_OUT,
            ReservationStatus.NO_SHOW,
        ) && checkIn <= day && day < checkOut
}

/** An in-house stay created by check-in. */
data class Stay(
    val id: String,
    val reservationId: String,
    val reservationCode: String,
    val guestId: String,
    val guestName: String,
    val guestPhone: String? = null,
    val roomId: String,
    val roomNumber: String,
    val checkInAt: Instant? = null,
    val plannedCheckOut: LocalDate,
    val actualCheckOutAt: Instant? = null,
    val status: StayStatus = StayStatus.IN_HOUSE,
    val guestsCount: Int = 1,
    val companions: List<Companion> = emptyList(),
    val totalRial: Long = 0L,
    val paidRial: Long = 0L,
) {
    val balanceRial: Long get() = (totalRial - paidRial)
}

data class ReservationQuote(
    val nights: Int,
    val nightlyRateRial: Long,
    val roomTotalRial: Long,
    val discountRial: Long,
    val totalRial: Long,
    val prepaidRial: Long,
    val remainingRial: Long,
)

/** Form model for the "new reservation" wizard. */
data class ReservationDraft(
    val guestId: String? = null,
    val guestName: String = "",
    val roomId: String? = null,
    val roomNumber: String? = null,
    val checkIn: JalaliDate? = null,
    val checkOut: JalaliDate? = null,
    val guestsCount: String = "1",
    val discountRial: String = "",
    val prepaidRial: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val joinWaitlist: Boolean = false,
    val companionNames: List<String> = emptyList(),
)

data class AvailabilityQuery(
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestsCount: Int = 1,
    val roomTypeId: String? = null,
)

data class AvailableRoom(
    val roomId: String,
    val roomNumber: String,
    val floor: Int,
    val roomTypeName: String,
    val capacity: Int,
    val ratePerNightRial: Long,
)

data class ReservationCalendarEntry(
    val reservationId: String,
    val date: LocalDate,
    val roomId: String?,
    val roomNumber: String?,
    val guestName: String,
    val status: ReservationStatus,
    val isArrival: Boolean,
    val isDeparture: Boolean,
)

data class WaitlistEntry(
    val id: String,
    val guestName: String,
    val guestPhone: String? = null,
    val requestedCheckIn: LocalDate? = null,
    val requestedCheckOut: LocalDate? = null,
    val guestsCount: Int = 1,
    val roomTypeName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)
