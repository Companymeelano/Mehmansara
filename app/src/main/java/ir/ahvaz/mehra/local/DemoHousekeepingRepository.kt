package ir.ahvaz.mehra.local

import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.common.CachedData
import ir.ahvaz.mehra.domain.model.CreateCleaningCommand
import ir.ahvaz.mehra.domain.model.HousekeepingPerformance
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.repository.HousekeepingRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoHousekeepingRepository @Inject constructor(
    private val store: DemoStore,
) : HousekeepingRepository {

    override suspend fun tasks(status: HousekeepingTaskStatus?): AppResult<CachedData<List<HousekeepingTask>>> {
        delay(140)
        val list = store.housekeepingTasks
            .filter { status == null || it.status == status }
            .sortedWith(compareBy({ it.status.ordinal }, { it.roomNumber }))
        return demoCached(list)
    }

    override suspend fun staff(): AppResult<List<StaffMember>> = demoOk(store.housekeepingStaff)

    override suspend fun create(command: CreateCleaningCommand): AppResult<HousekeepingTask> {
        delay(220)
        val room = store.room(command.roomId) ?: return demoNotFound("اتاق مورد نظر یافت نشد.")
        val task = HousekeepingTask(
            id = store.nextId("hk"),
            roomId = room.id,
            roomNumber = room.roomNumber,
            type = command.type,
            status = if (command.assigneeId != null) HousekeepingTaskStatus.ASSIGNED else HousekeepingTaskStatus.PENDING,
            assignedToId = command.assigneeId,
            assignedToName = store.housekeepingStaff.nameOf(command.assigneeId),
            notes = command.notes,
            createdAt = Instant.now(),
        )
        store.housekeepingTasks.add(0, task)
        return demoOk(task)
    }

    override suspend fun assign(taskId: String, staffId: String): AppResult<Unit> {
        delay(180)
        val index = store.housekeepingTasks.indexOfFirst { it.id == taskId }
        if (index < 0) return demoNotFound("کار مورد نظر یافت نشد.")
        val staff = store.housekeepingStaff.firstOrNull { it.id == staffId }
            ?: return demoNotFound("همکار انتخاب شده یافت نشد.")
        store.housekeepingTasks[index] = store.housekeepingTasks[index].copy(
            status = HousekeepingTaskStatus.ASSIGNED,
            assignedToId = staff.id,
            assignedToName = staff.fullName,
        )
        return AppResult.Success(Unit)
    }

    override suspend fun start(taskId: String): AppResult<Unit> {
        delay(180)
        val index = store.housekeepingTasks.indexOfFirst { it.id == taskId }
        if (index < 0) return demoNotFound("کار مورد نظر یافت نشد.")
        store.housekeepingTasks[index] = store.housekeepingTasks[index].copy(
            status = HousekeepingTaskStatus.IN_PROGRESS,
            startedAt = Instant.now(),
        )
        return AppResult.Success(Unit)
    }

    override suspend fun complete(taskId: String, note: String?): AppResult<Unit> {
        delay(220)
        val index = store.housekeepingTasks.indexOfFirst { it.id == taskId }
        if (index < 0) return demoNotFound("کار مورد نظر یافت نشد.")
        val task = store.housekeepingTasks[index]
        store.housekeepingTasks[index] = task.copy(
            status = HousekeepingTaskStatus.DONE,
            finishedAt = Instant.now(),
            notes = note?.ifBlank { null } ?: task.notes,
        )
        return AppResult.Success(Unit)
    }

    override suspend fun approve(taskId: String): AppResult<Unit> {
        delay(200)
        val index = store.housekeepingTasks.indexOfFirst { it.id == taskId }
        if (index < 0) return demoNotFound("کار مورد نظر یافت نشد.")
        val task = store.housekeepingTasks[index]
        store.housekeepingTasks[index] = task.copy(
            status = HousekeepingTaskStatus.APPROVED,
            approvedAt = Instant.now(),
            approvedByName = DemoStore.DEMO_OPERATOR,
        )
        val roomIndex = store.rooms.indexOfFirst { it.id == task.roomId }
        if (roomIndex >= 0 && store.rooms[roomIndex].status == RoomStatus.CLEANING) {
            store.rooms[roomIndex] = store.rooms[roomIndex].copy(
                status = RoomStatus.FREE,
                updatedAt = Instant.now(),
            )
        }
        return AppResult.Success(Unit)
    }

    override suspend fun performance(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<List<HousekeepingPerformance>> {
        delay(160)
        val tasks = store.housekeepingTasks.filter { task ->
            val created = store.localDateOf(task.createdAt) ?: return@filter false
            !created.isBefore(from) && !created.isAfter(to)
        }
        val list = store.housekeepingStaff.map { staff ->
            val mine = tasks.filter { it.assignedToId == staff.id }
            val finished = mine.filter { it.finishedAt != null }
            val averageMinutes = if (finished.isEmpty()) 0 else {
                finished.mapNotNull { task ->
                    val start = task.startedAt ?: return@mapNotNull null
                    val end = task.finishedAt ?: return@mapNotNull null
                    ((end.toEpochMilli() - start.toEpochMilli()) / 60_000L).toInt()
                }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
            }
            HousekeepingPerformance(
                employeeId = staff.id,
                employeeName = staff.fullName,
                completedCount = finished.size,
                averageMinutes = averageMinutes,
                approvedCount = mine.count { it.status == HousekeepingTaskStatus.APPROVED },
                rejectedCount = mine.count { it.status == HousekeepingTaskStatus.REJECTED },
            )
        }
        return demoOk(list)
    }
}
