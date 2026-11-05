package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RoomDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("floor") val floor: Int? = null,
    @SerializedName("capacity") val capacity: Int? = null,
    @SerializedName("roomTypeId") val roomTypeId: String? = null,
    @SerializedName("roomTypeName") val roomTypeName: String? = null,
    @SerializedName("ratePerNightRial") val ratePerNightRial: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("amenities") val amenities: List<String>? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("currentGuestName") val currentGuestName: String? = null,
    @SerializedName("currentStayId") val currentStayId: String? = null,
    @SerializedName("nextReservationDate") val nextReservationDate: String? = null,
    @SerializedName("updatedAtUtc") val updatedAtUtc: String? = null,
)

data class RoomDraftDto(
    @SerializedName("roomNumber") val roomNumber: String,
    @SerializedName("floor") val floor: Int,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("roomTypeId") val roomTypeId: String,
    @SerializedName("ratePerNightRial") val ratePerNightRial: Long,
    @SerializedName("amenities") val amenities: List<String>? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class RoomStatusRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("note") val note: String? = null,
)

data class LookupDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
)

data class RoomLookupsDto(
    @SerializedName("roomTypes") val roomTypes: List<LookupDto>? = null,
    @SerializedName("amenities") val amenities: List<LookupDto>? = null,
    @SerializedName("floors") val floors: List<Int>? = null,
)

data class RoomHistoryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("checkInDate") val checkInDate: String? = null,
    @SerializedName("checkOutDate") val checkOutDate: String? = null,
    @SerializedName("totalRial") val totalRial: Long? = null,
    @SerializedName("status") val status: String? = null,
)
