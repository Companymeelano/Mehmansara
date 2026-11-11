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

/**
 * The operator's own room inventory (from the handwritten list): room number -> bed count.
 * Used by the one-tap «بارگذاری پیش‌فرض» seed in the rooms screen so the necessary default
 * data is already inside the app instead of being typed one by one.
 */
object DefaultRoomInventory {
    val rooms: List<Pair<String, Int>> = listOf(
        "1" to 5, "2" to 4, "3" to 3, "4" to 4, "5" to 5, "6" to 4,
        "7" to 3, "8" to 3, "9" to 4, "10" to 3, "11" to 3, "12" to 5,
        "13" to 4, "14" to 6, "15" to 3, "16" to 3, "17" to 3,
        "207" to 2, "211" to 1,
    )
}
