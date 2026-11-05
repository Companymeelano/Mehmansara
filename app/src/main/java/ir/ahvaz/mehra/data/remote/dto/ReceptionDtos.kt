package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StayDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("reservationCode") val reservationCode: String? = null,
    @SerializedName("guestId") val guestId: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("guestPhone") val guestPhone: String? = null,
    @SerializedName("roomId") val roomId: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("checkInAtUtc") val checkInAtUtc: String? = null,
    @SerializedName("plannedCheckOut") val plannedCheckOut: String? = null,
    @SerializedName("actualCheckOutAtUtc") val actualCheckOutAtUtc: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("guestsCount") val guestsCount: Int? = null,
    @SerializedName("companions") val companions: List<CompanionDto>? = null,
    @SerializedName("totalRial") val totalRial: Long? = null,
    @SerializedName("paidRial") val paidRial: Long? = null,
)

data class CheckInRequestDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("companions") val companions: List<CompanionDto>? = null,
    @SerializedName("documentTypes") val documentTypes: List<String>? = null,
    @SerializedName("initialPaymentRial") val initialPaymentRial: Long? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("note") val note: String? = null,
)

data class CheckOutRequestDto(
    @SerializedName("finalPaymentRial") val finalPaymentRial: Long? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("discountRial") val discountRial: Long? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("moveRoomToCleaning") val moveRoomToCleaning: Boolean = true,
)

data class ReceiptLineDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("amountRial") val amountRial: Long? = null,
    @SerializedName("isCredit") val isCredit: Boolean? = null,
)

data class ReceiptDto(
    @SerializedName("receiptNumber") val receiptNumber: String? = null,
    @SerializedName("issuedAtUtc") val issuedAtUtc: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("checkInAtUtc") val checkInAtUtc: String? = null,
    @SerializedName("checkOutAtUtc") val checkOutAtUtc: String? = null,
    @SerializedName("plannedCheckOut") val plannedCheckOut: String? = null,
    @SerializedName("nights") val nights: Int? = null,
    @SerializedName("guestsCount") val guestsCount: Int? = null,
    @SerializedName("lines") val lines: List<ReceiptLineDto>? = null,
    @SerializedName("totalRial") val totalRial: Long? = null,
    @SerializedName("paidRial") val paidRial: Long? = null,
    @SerializedName("balanceRial") val balanceRial: Long? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("operatorName") val operatorName: String? = null,
)

data class ExtendStayRequestDto(
    @SerializedName("newCheckOut") val newCheckOut: String,
)

data class MoveRoomRequestDto(
    @SerializedName("newRoomId") val newRoomId: String,
    @SerializedName("reason") val reason: String? = null,
)
