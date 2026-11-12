package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.StaffMember

/** Small helpers shared by the demo repositories. */
internal fun <T> demoOk(value: T): AppResult<T> = AppResult.Success(value)

internal fun <T> demoCached(value: T): AppResult<CachedData<T>> =
    AppResult.Success(CachedData(data = value, updatedAtMillis = System.currentTimeMillis(), fromCache = false))

internal fun demoFailure(message: String): AppResult<Nothing> =
    AppResult.Failure(AppError.validation(message))

internal fun demoNotFound(message: String): AppResult<Nothing> =
    AppResult.Failure(AppError.notFound(message))

internal fun List<StaffMember>.nameOf(id: String?): String? =
    id?.let { staffId -> firstOrNull { it.id == staffId }?.fullName }

/** Users type Persian digits on a Persian keyboard; money and counts must still parse. */
internal fun String.demoDigits(): String = buildString {
    for (ch in this@demoDigits) {
        val persian = "۰۱۲۳۴۵۶۷۸۹".indexOf(ch)
        val arabic = "٠١٢٣٤٥٦٧٨٩".indexOf(ch)
        when {
            persian >= 0 -> append('0' + persian)
            arabic >= 0 -> append('0' + arabic)
            ch.isDigit() -> append(ch)
        }
    }
}
