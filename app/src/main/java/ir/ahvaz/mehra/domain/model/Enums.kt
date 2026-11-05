package ir.ahvaz.mehra.domain.model

/** Room lifecycle. Mirrors the backend enum by [wire] name. */
enum class RoomStatus(val wire: String, val labelFa: String) {
    FREE("Free", "آزاد"),
    RESERVED("Reserved", "رزرو‌شده"),
    OCCUPIED("Occupied", "اشغال"),
    CLEANING("Cleaning", "در حال نظافت"),
    MAINTENANCE("Maintenance", "تعمیرات"),
    OUT_OF_SERVICE("OutOfService", "خارج از سرویس");

    val isAssignable: Boolean get() = this == FREE

    companion object {
        fun fromWire(value: String?): RoomStatus? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        } ?: null
    }
}

enum class ReservationStatus(val wire: String, val labelFa: String) {
    WAITLIST("Waitlist", "لیست انتظار"),
    PENDING("Pending", "در انتظار تأیید"),
    CONFIRMED("Confirmed", "تأیید شده"),
    CHECKED_IN("CheckedIn", "پذیرش شده"),
    CHECKED_OUT("CheckedOut", "خروج کرده"),
    CANCELLED("Cancelled", "لغو شده"),
    NO_SHOW("NoShow", "عدم حضور"),
    EXPIRED("Expired", "منقضی");

    val isOpen: Boolean get() = this == PENDING || this == CONFIRMED || this == WAITLIST

    companion object {
        fun fromWire(value: String?): ReservationStatus? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class HousekeepingTaskStatus(val wire: String, val labelFa: String) {
    PENDING("Pending", "در انتظار تخصیص"),
    ASSIGNED("Assigned", "تخصیص داده شده"),
    IN_PROGRESS("InProgress", "در حال انجام"),
    DONE("Done", "پایان یافته"),
    APPROVED("Approved", "تأیید نهایی"),
    REJECTED("Rejected", "رد شده");

    val isOpen: Boolean get() = this != APPROVED && this != REJECTED

    companion object {
        fun fromWire(value: String?): HousekeepingTaskStatus? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class CleaningType(val wire: String, val labelFa: String) {
    CHECKOUT("Checkout", "نظافت پس از خروج"),
    DAILY("Daily", "نظافت روزانه"),
    DEEP("Deep", "نظافت اساسی"),
    TURNDOWN("Turndown", "آماده‌سازی شب");

    companion object {
        fun fromWire(value: String?): CleaningType? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class MaintenancePriority(val wire: String, val labelFa: String) {
    LOW("Low", "کم"),
    NORMAL("Normal", "عادی"),
    HIGH("High", "بالا"),
    URGENT("Urgent", "فوری");

    companion object {
        fun fromWire(value: String?): MaintenancePriority? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class MaintenanceStatus(val wire: String, val labelFa: String) {
    OPEN("Open", "ثبت شده"),
    ASSIGNED("Assigned", "ارجاع شده"),
    IN_PROGRESS("InProgress", "در حال تعمیر"),
    WAITING_PARTS("WaitingParts", "در انتظار قطعه"),
    RESOLVED("Resolved", "رفع نقص"),
    CLOSED("Closed", "بسته شده");

    val isOpen: Boolean get() = this != CLOSED && this != RESOLVED

    companion object {
        fun fromWire(value: String?): MaintenanceStatus? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class PaymentMethod(val wire: String, val labelFa: String) {
    CASH("Cash", "نقد"),
    POS("Pos", "کارت‌خوان"),
    CARD("Card", "کارت به کارت"),
    TRANSFER("Transfer", "حواله بانکی"),
    ONLINE("Online", "درگاه اینترنتی"),
    CHECK("Check", "چک"),
    OTHER("Other", "سایر");

    companion object {
        fun fromWire(value: String?): PaymentMethod? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class TransactionType(val wire: String, val labelFa: String) {
    RECEIVE("Receive", "دریافت"),
    PAY("Pay", "پرداخت"),
    DISCOUNT("Discount", "تخفیف"),
    REFUND("Refund", "بازگشت وجه");

    /** Signs the effect on the cash box. */
    val sign: Int get() = if (this == RECEIVE || this == DISCOUNT) 1 else -1

    companion object {
        fun fromWire(value: String?): TransactionType? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class StayStatus(val wire: String, val labelFa: String) {
    IN_HOUSE("InHouse", "در اقامت"),
    CHECKED_OUT("CheckedOut", "خروج کرده"),
    MOVED("Moved", "جابه‌جا شده");

    companion object {
        fun fromWire(value: String?): StayStatus? = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }
}

enum class AlertSeverity(val wire: String, val labelFa: String) {
    INFO("Info", "اطلاع"),
    WARNING("Warning", "هشدار"),
    CRITICAL("Critical", "بحرانی");

    companion object {
        fun fromWire(value: String?): AlertSeverity = entries.firstOrNull {
            it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        } ?: INFO
    }
}
