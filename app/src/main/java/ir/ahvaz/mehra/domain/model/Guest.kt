package ir.ahvaz.mehra.domain.model

import java.time.Instant

data class Companion(
    val id: String? = null,
    val fullName: String,
    val nationalId: String? = null,
    val relation: String? = null,
)

data class Guest(
    val id: String,
    val firstName: String,
    val lastName: String,
    val nationalId: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val balanceRial: Long = 0L,
    val totalStays: Int = 0,
    val lastStayAt: Instant? = null,
    val companions: List<Companion> = emptyList(),
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

data class GuestDetail(
    val guest: Guest,
    val stays: List<RoomHistoryEntry> = emptyList(),
    val reservations: List<ReservationSummary> = emptyList(),
    val ledger: List<Transaction> = emptyList(),
)

/** Lightweight projection used inside lists. */
data class ReservationSummary(
    val id: String,
    val code: String,
    val checkIn: java.time.LocalDate?,
    val checkOut: java.time.LocalDate?,
    val status: ReservationStatus,
    val totalRial: Long,
    val roomNumber: String?,
)

data class GuestDraft(
    val id: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val nationalId: String = "",
    val phone: String = "",
    val city: String = "",
    val address: String = "",
    val notes: String = "",
    val companions: List<Companion> = emptyList(),
) {
    val isEdit: Boolean get() = id != null
}
