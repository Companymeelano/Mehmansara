package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReservationDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("guestId") val guestId: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("guestPhone") val guestPhone: String? = null,
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("roomTypeName") val roomTypeName: String? = null,
    @SerializedName("checkInDate") val checkInDate: String? = null,
    @SerializedName("checkOutDate") val checkOutDate: String? = null,
    @SerializedName("guestsCount") val guestsCount: Int? = null,
    @SerializedName("nightlyRateRial") val nightlyRateRial: Long? = null,
    @SerializedName("totalRial") val totalRial: Long? = null,
    @SerializedName("discountRial") val discountRial: Long? = null,
    @SerializedName("paidRial") val paidRial: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdByName") val createdByName: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
    @SerializedName("companions") val companions: List<CompanionDto>? = null,
)

data class CreateReservationRequestDto(
    @SerializedName("guestId") val guestId: String? = null,
    @SerializedName("guest") val guest: GuestDraftDto? = null,
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("checkInDate") val checkInDate: String,
    @SerializedName("checkOutDate") val checkOutDate: String,
    @SerializedName("guestsCount") val guestsCount: Int,
    @SerializedName("discountRial") val discountRial: Long? = null,
    @SerializedName("prepaidRial") val prepaidRial: Long? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("joinWaitlist") val joinWaitlist: Boolean = false,
    @SerializedName("companions") val companions: List<CompanionDto>? = null,
)

data class QuoteRequestDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("checkInDate") val checkInDate: String,
    @SerializedName("checkOutDate") val checkOutDate: String,
    @SerializedName("guestsCount") val guestsCount: Int,
    @SerializedName("discountRial") val discountRial: Long? = null,
)

data class QuoteDto(
    @SerializedName("nights") val nights: Int? = null,
    @SerializedName("nightlyRateRial") val nightlyRateRial: Long? = null,
    @SerializedName("roomTotalRial") val roomTotalRial: Long? = null,
    @SerializedName("discountRial") val discountRial: Long? = null,
    @SerializedName("totalRial") val totalRial: Long? = null,
    @SerializedName("prepaidRial") val prepaidRial: Long? = null,
    @SerializedName("remainingRial") val remainingRial: Long? = null,
)

data class AvailableRoomDto(
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("floor") val floor: Int? = null,
    @SerializedName("roomTypeName") val roomTypeName: String? = null,
    @SerializedName("capacity") val capacity: Int? = null,
    @SerializedName("ratePerNightRial") val ratePerNightRial: Long? = null,
)

data class CalendarEntryDto(
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("isArrival") val isArrival: Boolean? = null,
    @SerializedName("isDeparture") val isDeparture: Boolean? = null,
)

data class WaitlistEntryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("guestPhone") val guestPhone: String? = null,
    @SerializedName("requestedCheckIn") val requestedCheckIn: String? = null,
    @SerializedName("requestedCheckOut") val requestedCheckOut: String? = null,
    @SerializedName("guestsCount") val guestsCount: Int? = null,
    @SerializedName("roomTypeName") val roomTypeName: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
)

data class CancelReservationRequestDto(
    @SerializedName("reason") val reason: String? = null,
)

data class PromoteWaitlistRequestDto(
    @SerializedName("roomId") val roomId: String,
)
