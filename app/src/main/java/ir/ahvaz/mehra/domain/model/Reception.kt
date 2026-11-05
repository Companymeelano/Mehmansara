package ir.ahvaz.mehra.domain.model

import java.time.Instant
import java.time.LocalDate

data class CheckInCommand(
    val reservationId: String,
    val roomId: String,
    val companions: List<Companion> = emptyList(),
    val documentTypes: List<String> = emptyList(),
    val initialPaymentRial: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String? = null,
)

data class CheckOutCommand(
    val stayId: String,
    val finalPaymentRial: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val discountRial: Long = 0L,
    val note: String? = null,
    val moveRoomToCleaning: Boolean = true,
)

data class ReceiptLine(
    val title: String,
    val amountRial: Long,
    val isCredit: Boolean = true,
)

data class Receipt(
    val receiptNumber: String,
    val issuedAt: Instant,
    val guestName: String,
    val roomNumber: String,
    val checkInAt: Instant?,
    val checkOutAt: Instant?,
    val plannedCheckOut: LocalDate?,
    val nights: Int,
    val guestsCount: Int,
    val lines: List<ReceiptLine>,
    val totalRial: Long,
    val paidRial: Long,
    val balanceRial: Long,
    val paymentMethod: PaymentMethod?,
    val operatorName: String? = null,
    val hotelName: String = "مهمان‌پذیر فرهنگیان اهواز",
)
