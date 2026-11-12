package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.DashboardSnapshot
import ir.ahvaz.mehra.domain.model.OccupancyPoint
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.TransactionType
import ir.ahvaz.mehra.domain.repository.DashboardRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Dashboard numbers are computed from the demo dataset, so they always add up. */
@Singleton
class DemoDashboardRepository @Inject constructor(
    private val store: DemoStore,
) : DashboardRepository {

    override suspend fun load(businessDate: LocalDate?): AppResult<CachedData<DashboardSnapshot>> {
        delay(180)
        val today = store.today
        val rooms = store.rooms
        val total = rooms.size
        val occupied = rooms.count { it.status == RoomStatus.OCCUPIED }

        val snapshot = DashboardSnapshot(
            totalRooms = total,
            freeRooms = rooms.count { it.status == RoomStatus.FREE },
            reservedRooms = rooms.count { it.status == RoomStatus.RESERVED },
            occupiedRooms = occupied,
            cleaningRooms = rooms.count { it.status == RoomStatus.CLEANING },
            maintenanceRooms = rooms.count { it.status == RoomStatus.MAINTENANCE },
            outOfServiceRooms = rooms.count { it.status == RoomStatus.OUT_OF_SERVICE },
            todayCheckIns = store.arrivalsOn(today).count {
                it.status == ReservationStatus.CONFIRMED || it.status == ReservationStatus.CHECKED_IN
            },
            todayCheckOuts = store.checkoutOn(today).size,
            revenueTodayRial = store.transactionsOn(today)
                .filter { it.type == TransactionType.RECEIVE }
                .sumOf { it.amountRial },
            occupancyPercent = if (total == 0) 0 else occupied * 100 / total,
            occupancySeries = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                OccupancyPoint(date = date, occupied = store.occupiedOn(date), total = total)
            },
            recentActivities = store.recentActivities(),
            alerts = store.alerts(),
            businessDate = businessDate ?: today,
        )
        return demoCached(snapshot)
    }
}
