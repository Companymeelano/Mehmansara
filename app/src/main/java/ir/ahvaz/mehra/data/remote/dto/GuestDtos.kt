package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CompanionDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("nationalId") val nationalId: String? = null,
    @SerializedName("relation") val relation: String? = null,
)

data class GuestDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("nationalId") val nationalId: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("balanceRial") val balanceRial: Long? = null,
    @SerializedName("totalStays") val totalStays: Int? = null,
    @SerializedName("lastStayAtUtc") val lastStayAtUtc: String? = null,
    @SerializedName("companions") val companions: List<CompanionDto>? = null,
)

data class GuestDraftDto(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("nationalId") val nationalId: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("companions") val companions: List<CompanionDto>? = null,
)

data class GuestDetailDto(
    @SerializedName("guest") val guest: GuestDto? = null,
    @SerializedName("stays") val stays: List<RoomHistoryDto>? = null,
    @SerializedName("reservations") val reservations: List<ReservationSummaryDto>? = null,
    @SerializedName("ledger") val ledger: List<TransactionDto>? = null,
)

data class ReservationSummaryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("checkInDate") val checkInDate: String? = null,
    @SerializedName("checkOutDate") val checkOutDate: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("totalRial") val totalRial: Long? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
)
