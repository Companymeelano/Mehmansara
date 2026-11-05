package ir.ahvaz.mehra.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import ir.ahvaz.mehra.domain.model.AlertSeverity
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.StayStatus
import ir.ahvaz.mehra.domain.model.TransactionType

@Composable
@ReadOnlyComposable
fun RoomStatus.tint(): Color = when (this) {
    RoomStatus.FREE -> MehraTheme.colors.statusFree
    RoomStatus.RESERVED -> MehraTheme.colors.statusReserved
    RoomStatus.OCCUPIED -> MehraTheme.colors.statusOccupied
    RoomStatus.CLEANING -> MehraTheme.colors.statusCleaning
    RoomStatus.MAINTENANCE -> MehraTheme.colors.statusMaintenance
    RoomStatus.OUT_OF_SERVICE -> MehraTheme.colors.statusOutOfService
}

@Composable
@ReadOnlyComposable
fun RoomStatus.softTint(): Color = when (this) {
    RoomStatus.FREE -> MehraTheme.colors.successSoft
    RoomStatus.RESERVED -> MehraTheme.colors.infoSoft
    RoomStatus.OCCUPIED -> MehraTheme.colors.goldSoft
    RoomStatus.CLEANING -> MehraTheme.colors.infoSoft
    RoomStatus.MAINTENANCE -> MehraTheme.colors.warningSoft
    RoomStatus.OUT_OF_SERVICE -> MehraTheme.colors.dangerSoft
}

@Composable
@ReadOnlyComposable
fun ReservationStatus.tint(): Color = when (this) {
    ReservationStatus.WAITLIST -> MehraTheme.colors.statusCleaning
    ReservationStatus.PENDING -> MehraTheme.colors.warning
    ReservationStatus.CONFIRMED -> MehraTheme.colors.info
    ReservationStatus.CHECKED_IN -> MehraTheme.colors.success
    ReservationStatus.CHECKED_OUT -> MehraTheme.colors.statusOutOfService
    ReservationStatus.CANCELLED, ReservationStatus.EXPIRED -> MehraTheme.colors.danger
    ReservationStatus.NO_SHOW -> MehraTheme.colors.warning
}

@Composable
@ReadOnlyComposable
fun HousekeepingTaskStatus.tint(): Color = when (this) {
    HousekeepingTaskStatus.PENDING -> MehraTheme.colors.warning
    HousekeepingTaskStatus.ASSIGNED -> MehraTheme.colors.info
    HousekeepingTaskStatus.IN_PROGRESS -> MehraTheme.colors.statusCleaning
    HousekeepingTaskStatus.DONE -> MehraTheme.colors.success
    HousekeepingTaskStatus.APPROVED -> MehraTheme.colors.success
    HousekeepingTaskStatus.REJECTED -> MehraTheme.colors.danger
}

@Composable
@ReadOnlyComposable
fun MaintenanceStatus.tint(): Color = when (this) {
    MaintenanceStatus.OPEN -> MehraTheme.colors.warning
    MaintenanceStatus.ASSIGNED -> MehraTheme.colors.info
    MaintenanceStatus.IN_PROGRESS -> MehraTheme.colors.statusMaintenance
    MaintenanceStatus.WAITING_PARTS -> MehraTheme.colors.warning
    MaintenanceStatus.RESOLVED -> MehraTheme.colors.success
    MaintenanceStatus.CLOSED -> MehraTheme.colors.statusOutOfService
}

@Composable
@ReadOnlyComposable
fun MaintenancePriority.tint(): Color = when (this) {
    MaintenancePriority.LOW -> MehraTheme.colors.statusOutOfService
    MaintenancePriority.NORMAL -> MehraTheme.colors.info
    MaintenancePriority.HIGH -> MehraTheme.colors.warning
    MaintenancePriority.URGENT -> MehraTheme.colors.danger
}

@Composable
@ReadOnlyComposable
fun AlertSeverity.tint(): Color = when (this) {
    AlertSeverity.INFO -> MehraTheme.colors.info
    AlertSeverity.WARNING -> MehraTheme.colors.warning
    AlertSeverity.CRITICAL -> MehraTheme.colors.danger
}

@Composable
@ReadOnlyComposable
fun TransactionType.tint(): Color = when (this) {
    TransactionType.RECEIVE -> MehraTheme.colors.success
    TransactionType.PAY -> MehraTheme.colors.info
    TransactionType.DISCOUNT -> MehraTheme.colors.warning
    TransactionType.REFUND -> MehraTheme.colors.danger
}

@Composable
@ReadOnlyComposable
fun StayStatus.tint(): Color = when (this) {
    StayStatus.IN_HOUSE -> MehraTheme.colors.success
    StayStatus.CHECKED_OUT -> MehraTheme.colors.statusOutOfService
    StayStatus.MOVED -> MehraTheme.colors.info
}
