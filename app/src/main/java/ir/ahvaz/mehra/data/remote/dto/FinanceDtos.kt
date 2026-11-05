package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("amountRial") val amountRial: Long? = null,
    @SerializedName("method") val method: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("operatorName") val operatorName: String? = null,
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("reference") val reference: String? = null,
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null,
    @SerializedName("isReversed") val isReversed: Boolean? = null,
)

data class MethodTotalDto(
    @SerializedName("method") val method: String? = null,
    @SerializedName("amountRial") val amountRial: Long? = null,
    @SerializedName("count") val count: Int? = null,
)

data class CashBoxDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("receivedRial") val receivedRial: Long? = null,
    @SerializedName("paidRial") val paidRial: Long? = null,
    @SerializedName("discountsRial") val discountsRial: Long? = null,
    @SerializedName("netRial") val netRial: Long? = null,
    @SerializedName("transactionCount") val transactionCount: Int? = null,
    @SerializedName("openBalanceRial") val openBalanceRial: Long? = null,
    @SerializedName("byMethod") val byMethod: List<MethodTotalDto>? = null,
)

data class DailyPointDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("receivedRial") val receivedRial: Long? = null,
    @SerializedName("paidRial") val paidRial: Long? = null,
)

data class FinanceSummaryDto(
    @SerializedName("from") val from: String? = null,
    @SerializedName("to") val to: String? = null,
    @SerializedName("receivedRial") val receivedRial: Long? = null,
    @SerializedName("paidRial") val paidRial: Long? = null,
    @SerializedName("discountsRial") val discountsRial: Long? = null,
    @SerializedName("netRial") val netRial: Long? = null,
    @SerializedName("byMethod") val byMethod: List<MethodTotalDto>? = null,
    @SerializedName("dailySeries") val dailySeries: List<DailyPointDto>? = null,
)

data class DebtorDto(
    @SerializedName("guestId") val guestId: String? = null,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("balanceRial") val balanceRial: Long? = null,
    @SerializedName("lastActivityAtUtc") val lastActivityAtUtc: String? = null,
)

data class NewTransactionRequestDto(
    @SerializedName("type") val type: String,
    @SerializedName("amountRial") val amountRial: Long,
    @SerializedName("method") val method: String,
    @SerializedName("description") val description: String,
    @SerializedName("guestId") val guestId: String? = null,
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("reference") val reference: String? = null,
)

data class DiscountRequestDto(
    @SerializedName("guestId") val guestId: String? = null,
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("amountRial") val amountRial: Long,
    @SerializedName("reason") val reason: String,
)
