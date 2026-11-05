package ir.ahvaz.mehra.domain.model

import java.time.Instant

data class Room(
    val id: String,
    val roomNumber: String,
    val floor: Int,
    val capacity: Int,
    val roomTypeId: String,
    val roomTypeName: String,
    val ratePerNightRial: Long,
    val status: RoomStatus,
    val amenities: List<String> = emptyList(),
    val notes: String? = null,
    val currentGuestName: String? = null,
    val currentStayId: String? = null,
    val nextReservationAt: java.time.LocalDate? = null,
    val updatedAt: Instant? = null,
) {
    val titleFa: String get() = "اتاق $roomNumber"
}

data class LookupItem(val id: String, val title: String)

data class RoomLookups(
    val roomTypes: List<LookupItem> = emptyList(),
    val amenities: List<LookupItem> = emptyList(),
    val floors: List<Int> = emptyList(),
)

data class RoomHistoryEntry(
    val id: String,
    val guestName: String,
    val checkIn: java.time.LocalDate?,
    val checkOut: java.time.LocalDate?,
    val totalRial: Long,
    val status: StayStatus,
)

/** Form model used by the room create/edit screen. Strings keep raw user input. */
data class RoomDraft(
    val id: String? = null,
    val roomNumber: String = "",
    val floor: String = "",
    val capacity: String = "2",
    val roomTypeId: String = "",
    val ratePerNightRial: String = "",
    val amenities: Set<String> = emptySet(),
    val status: RoomStatus = RoomStatus.FREE,
    val notes: String = "",
) {
    val isEdit: Boolean get() = id != null
}

data class RoomFilterState(
    val query: String = "",
    val status: RoomStatus? = null,
    val floor: Int? = null,
)
