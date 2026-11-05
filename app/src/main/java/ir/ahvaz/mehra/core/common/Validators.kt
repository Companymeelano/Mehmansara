package ir.ahvaz.mehra.core.common

/** Client side validation. The server re-validates everything — this only shortens feedback loops. */
object Validators {

    fun isBlank(value: String?): Boolean = value.isNullOrBlank()

    /** Iranian national id (کد ملی): 10 digits with the official check digit. */
    fun isValidIranNationalId(raw: String?): Boolean {
        val value = raw?.digitsOnly() ?: return false
        if (value.length != 10) return false
        if (value.toSet().size == 1) return false
        var sum = 0
        for (i in 0..8) {
            sum += (value[i] - '0') * (10 - i)
        }
        val remainder = sum % 11
        val check = value[9] - '0'
        return if (remainder < 2) check == remainder else check == 11 - remainder
    }

    fun isValidIranMobile(raw: String?): Boolean {
        val value = normalizeIranMobile(raw ?: return false)
        return value.length == 11 && value.startsWith("09")
    }

    fun isValidEmail(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(raw.trim())
    }

    fun isValidUsername(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return Regex("^[A-Za-z0-9._-]{3,32}$").matches(raw.trim())
    }

    /** Returns a Persian error message, or null when the password is acceptable. */
    fun passwordError(raw: String?): String? = when {
        raw == null || raw.isEmpty() -> "رمز عبور را وارد کنید."
        raw.length < 8 -> "رمز عبور باید حداقل ۸ کاراکتر باشد."
        raw.length > 128 -> "رمز عبور بیش از حد طولانی است."
        raw.all { it.isDigit() } -> "رمز عبور نباید فقط عدد باشد."
        raw.all { it.isLetter() } -> "رمز عبور باید شامل عدد یا نماد باشد."
        else -> null
    }

    fun requiredError(raw: String?, fieldLabel: String): String? =
        if (raw.isNullOrBlank()) "$fieldLabel الزامی است." else null
}
