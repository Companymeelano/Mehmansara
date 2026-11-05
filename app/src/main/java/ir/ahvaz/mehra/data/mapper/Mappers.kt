package ir.ahvaz.mehra.data.mapper

import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.date.IsoFormat
import ir.ahvaz.mehra.core.date.TehranTime
import ir.ahvaz.mehra.core.date.toJalali
import ir.ahvaz.mehra.data.remote.dto.ActivityDto
import ir.ahvaz.mehra.data.remote.dto.AlertDto
import ir.ahvaz.mehra.data.remote.dto.AppUserDto
import ir.ahvaz.mehra.data.remote.dto.AuditLogDto
import ir.ahvaz.mehra.data.remote.dto.AvailableRoomDto
import ir.ahvaz.mehra.data.remote.dto.CalendarEntryDto
import ir.ahvaz.mehra.data.remote.dto.CashBoxDto
import ir.ahvaz.mehra.data.remote.dto.CompanionDto
import ir.ahvaz.mehra.data.remote.dto.DailyPointDto
import ir.ahvaz.mehra.data.remote.dto.DashboardSummaryDto
import ir.ahvaz.mehra.data.remote.dto.DebtorDto
import ir.ahvaz.mehra.data.remote.dto.FinanceSummaryDto
import ir.ahvaz.mehra.data.remote.dto.GuestDetailDto
import ir.ahvaz.mehra.data.remote.dto.GuestDto
import ir.ahvaz.mehra.data.remote.dto.HousekeepingTaskDto
import ir.ahvaz.mehra.data.remote.dto.LookupDto
import ir.ahvaz.mehra.data.remote.dto.MaintenanceTicketDto
import ir.ahvaz.mehra.data.remote.dto.MethodTotalDto
import ir.ahvaz.mehra.data.remote.dto.NotificationDto
import ir.ahvaz.mehra.data.remote.dto.OccupancyPointDto
import ir.ahvaz.mehra.data.remote.dto.PerformanceDto
import ir.ahvaz.mehra.data.remote.dto.QuoteDto
import ir.ahvaz.mehra.data.remote.dto.ReceiptDto
import ir.ahvaz.mehra.data.remote.dto.ReportDto
import ir.ahvaz.mehra.data.remote.dto.ReservationDto
import ir.ahvaz.mehra.data.remote.dto.ReservationSummaryDto
import ir.ahvaz.mehra.data.remote.dto.RoleDto
import ir.ahvaz.mehra.data.remote.dto.RoomDto
import ir.ahvaz.mehra.data.remote.dto.RoomHistoryDto
import ir.ahvaz.mehra.data.remote.dto.RoomLookupsDto
import ir.ahvaz.mehra.data.remote.dto.SessionUserDto
import ir.ahvaz.mehra.data.remote.dto.StaffDto
import ir.ahvaz.mehra.data.remote.dto.StayDto
import ir.ahvaz.mehra.data.remote.dto.TransactionDto
import ir.ahvaz.mehra.data.remote.dto.WaitlistEntryDto
import ir.ahvaz.mehra.domain.model.ActivityItem
import ir.ahvaz.mehra.domain.model.ActivityKind
import ir.ahvaz.mehra.domain.model.AlertItem
import ir.ahvaz.mehra.domain.model.AlertSeverity
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.AvailableRoom
import ir.ahvaz.mehra.domain.model.CashBoxSummary
import ir.ahvaz.mehra.domain.model.CleaningType
import ir.ahvaz.mehra.domain.model.Companion
import ir.ahvaz.mehra.domain.model.DailyPoint
import ir.ahvaz.mehra.domain.model.DashboardSnapshot
import ir.ahvaz.mehra.domain.model.DebtorItem
import ir.ahvaz.mehra.domain.model.FinanceSummary
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.GuestDetail
import ir.ahvaz.mehra.domain.model.HousekeepingPerformance
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.LookupItem
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.MethodTotal
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.OccupancyPoint
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.ReceiptLine
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.ReportType
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationCalendarEntry
import ir.ahvaz.mehra.domain.model.ReservationQuote
import ir.ahvaz.mehra.domain.model.ReservationStatus
import ir.ahvaz.mehra.domain.model.ReservationSummary
import ir.ahvaz.mehra.domain.model.RoleInfo
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomHistoryEntry
import ir.ahvaz.mehra.domain.model.RoomLookups
import ir.ahvaz.mehra.domain.model.RoomStatus
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.model.StaffMember
import ir.ahvaz.mehra.domain.model.Stay
import ir.ahvaz.mehra.domain.model.StayStatus
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionType
import ir.ahvaz.mehra.domain.model.UserRole
import ir.ahvaz.mehra.domain.model.WaitlistEntry

/**
 * Wire models are null-heavy and use strings for dates; domain models are strict and use
 * java.time. All conversion happens here so the rest of the app never touches a DTO.
 */

private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "\u2014"

private fun String?.orEmptySafe(): String = this.orEmpty()

fun CompanionDto.toDomain(): Companion = Companion(
    id = id,
    fullName = fullName.orEmpty(),
    nationalId = nationalId,
    relation = relation,
)

fun List<CompanionDto>?.toDomainCompanions(): List<Companion> = this.orEmpty().map { it.toDomain() }

fun RoomDto.toDomain(): Room = Room(
    id = id.orEmpty(),
    roomNumber = roomNumber.orEmpty(),
    floor = floor ?: 0,
    capacity = capacity ?: 0,
    roomTypeId = roomTypeId.orEmpty(),
    roomTypeName = roomTypeName.orEmpty(),
    ratePerNightRial = ratePerNightRial ?: 0L,
    status = RoomStatus.fromWire(status) ?: RoomStatus.FREE,
    amenities = amenities.orEmpty(),
    notes = notes,
    currentGuestName = currentGuestName,
    currentStayId = currentStayId,
    nextReservationAt = IsoFormat.parseDate(nextReservationDate),
    updatedAt = IsoFormat.parseInstant(updatedAtUtc),
)

fun LookupDto.toDomain(): LookupItem = LookupItem(id.orEmpty(), title.orEmpty())

fun RoomLookupsDto.toDomain(): RoomLookups = RoomLookups(
    roomTypes = roomTypes.orEmpty().map { it.toDomain() },
    amenities = amenities.orEmpty().map { it.toDomain() },
    floors = floors.orEmpty(),
)

fun RoomHistoryDto.toDomain(): RoomHistoryEntry = RoomHistoryEntry(
    id = id.orEmpty(),
    guestName = guestName.orEmpty(),
    checkIn = IsoFormat.parseDate(checkInDate),
    checkOut = IsoFormat.parseDate(checkOutDate),
    totalRial = totalRial ?: 0L,
    status = StayStatus.fromWire(status) ?: StayStatus.CHECKED_OUT,
)

fun GuestDto.toDomain(): Guest = Guest(
    id = id.orEmpty(),
    firstName = firstName.orEmpty(),
    lastName = lastName.orEmpty(),
    nationalId = nationalId,
    phone = phone,
    city = city,
    address = address,
    notes = notes,
    balanceRial = balanceRial ?: 0L,
    totalStays = totalStays ?: 0,
    lastStayAt = IsoFormat.parseInstant(lastStayAtUtc),
    companions = companions.toDomainCompanions(),
)

fun ReservationSummaryDto.toDomain(): ReservationSummary = ReservationSummary(
    id = id.orEmpty(),
    code = code.orEmpty(),
    checkIn = IsoFormat.parseDate(checkInDate),
    checkOut = IsoFormat.parseDate(checkOutDate),
    status = ReservationStatus.fromWire(status) ?: ReservationStatus.PENDING,
    totalRial = totalRial ?: 0L,
    roomNumber = roomNumber,
)

fun GuestDetailDto.toDomain(): GuestDetail = GuestDetail(
    guest = (guest ?: GuestDto()).toDomain(),
    stays = stays.orEmpty().map { it.toDomain() },
    reservations = reservations.orEmpty().map { it.toDomain() },
    ledger = ledger.orEmpty().map { it.toDomain() },
)

fun ReservationDto.toDomain(): Reservation? {
    val checkIn = IsoFormat.parseDate(checkInDate) ?: return null
    val checkOut = IsoFormat.parseDate(checkOutDate) ?: return null
    return Reservation(
        id = id.orEmpty(),
        code = code.orEmpty(),
        guestId = guestId.orEmpty(),
        guestName = guestName.orEmpty(),
        guestPhone = guestPhone,
        roomId = roomId,
        roomNumber = roomNumber,
        roomTypeName = roomTypeName,
        checkIn = checkIn,
        checkOut = checkOut,
        guestsCount = guestsCount ?: 1,
        nightlyRateRial = nightlyRateRial ?: 0L,
        totalRial = totalRial ?: 0L,
        discountRial = discountRial ?: 0L,
        paidRial = paidRial ?: 0L,
        status = ReservationStatus.fromWire(status) ?: ReservationStatus.PENDING,
        notes = notes,
        createdByName = createdByName,
        createdAt = IsoFormat.parseInstant(createdAtUtc),
        companions = companions.toDomainCompanions(),
    )
}

fun List<ReservationDto>.toDomainReservations(): List<Reservation> = mapNotNull { it.toDomain() }

fun StayDto.toDomain(): Stay? {
    val planned = IsoFormat.parseDate(plannedCheckOut) ?: return null
    return Stay(
        id = id.orEmpty(),
        reservationId = reservationId.orEmpty(),
        reservationCode = reservationCode.orEmpty(),
        guestId = guestId.orEmpty(),
        guestName = guestName.orEmpty(),
        guestPhone = guestPhone,
        roomId = roomId.orEmpty(),
        roomNumber = roomNumber.orEmpty(),
        checkInAt = IsoFormat.parseInstant(checkInAtUtc),
        plannedCheckOut = planned,
        actualCheckOutAt = IsoFormat.parseInstant(actualCheckOutAtUtc),
        status = StayStatus.fromWire(status) ?: StayStatus.IN_HOUSE,
        guestsCount = guestsCount ?: 1,
        companions = companions.toDomainCompanions(),
        totalRial = totalRial ?: 0L,
        paidRial = paidRial ?: 0L,
    )
}

fun QuoteDto.toDomain(): ReservationQuote = ReservationQuote(
    nights = nights ?: 0,
    nightlyRateRial = nightlyRateRial ?: 0L,
    roomTotalRial = roomTotalRial ?: 0L,
    discountRial = discountRial ?: 0L,
    totalRial = totalRial ?: 0L,
    prepaidRial = prepaidRial ?: 0L,
    remainingRial = remainingRial ?: 0L,
)

fun AvailableRoomDto.toDomain(): AvailableRoom = AvailableRoom(
    roomId = roomId.orEmpty(),
    roomNumber = roomNumber.orEmpty(),
    floor = floor ?: 0,
    roomTypeName = roomTypeName.orEmpty(),
    capacity = capacity ?: 0,
    ratePerNightRial = ratePerNightRial ?: 0L,
)

fun CalendarEntryDto.toDomain(): ReservationCalendarEntry? {
    val date = IsoFormat.parseDate(date) ?: return null
    return ReservationCalendarEntry(
        reservationId = reservationId.orEmpty(),
        date = date,
        roomId = roomId,
        roomNumber = roomNumber,
        guestName = guestName.orEmpty(),
        status = ReservationStatus.fromWire(status) ?: ReservationStatus.CONFIRMED,
        isArrival = isArrival ?: false,
        isDeparture = isDeparture ?: false,
    )
}

fun WaitlistEntryDto.toDomain(): WaitlistEntry = WaitlistEntry(
    id = id.orEmpty(),
    guestName = guestName.orEmpty(),
    guestPhone = guestPhone,
    requestedCheckIn = IsoFormat.parseDate(requestedCheckIn),
    requestedCheckOut = IsoFormat.parseDate(requestedCheckOut),
    guestsCount = guestsCount ?: 1,
    roomTypeName = roomTypeName,
    notes = notes,
    createdAt = IsoFormat.parseInstant(createdAtUtc),
)

fun ReceiptDto.toDomain(): Receipt = Receipt(
    receiptNumber = receiptNumber.orEmpty(),
    issuedAt = IsoFormat.parseInstant(issuedAtUtc) ?: TehranTime.nowInstant(),
    guestName = guestName.orEmpty(),
    roomNumber = roomNumber.orEmpty(),
    checkInAt = IsoFormat.parseInstant(checkInAtUtc),
    checkOutAt = IsoFormat.parseInstant(checkOutAtUtc),
    plannedCheckOut = IsoFormat.parseDate(plannedCheckOut),
    nights = nights ?: 0,
    guestsCount = guestsCount ?: 1,
    lines = lines.orEmpty().map {
        ReceiptLine(
            title = it.title.orEmpty(),
            amountRial = it.amountRial ?: 0L,
            isCredit = it.isCredit ?: true,
        )
    },
    totalRial = totalRial ?: 0L,
    paidRial = paidRial ?: 0L,
    balanceRial = balanceRial ?: 0L,
    paymentMethod = PaymentMethod.fromWire(paymentMethod),
    operatorName = operatorName,
)

fun TransactionDto.toDomain(): Transaction = Transaction(
    id = id.orEmpty(),
    code = code.orEmpty(),
    type = TransactionType.fromWire(type) ?: TransactionType.RECEIVE,
    amountRial = amountRial ?: 0L,
    method = PaymentMethod.fromWire(method) ?: PaymentMethod.CASH,
    description = description.orEmpty(),
    guestName = guestName,
    roomNumber = roomNumber,
    operatorName = operatorName,
    reservationId = reservationId,
    reference = reference,
    createdAt = IsoFormat.parseInstant(createdAtUtc),
    isReversed = isReversed ?: false,
)

fun MethodTotalDto.toDomain(): MethodTotal = MethodTotal(
    method = PaymentMethod.fromWire(method) ?: PaymentMethod.OTHER,
    amountRial = amountRial ?: 0L,
    count = count ?: 0,
)

fun CashBoxDto.toDomain(): CashBoxSummary = CashBoxSummary(
    date = IsoFormat.parseDate(date) ?: TehranTime.today(),
    receivedRial = receivedRial ?: 0L,
    paidRial = paidRial ?: 0L,
    discountsRial = discountsRial ?: 0L,
    netRial = netRial ?: ((receivedRial ?: 0L) - (paidRial ?: 0L)),
    transactionCount = transactionCount ?: 0,
    byMethod = byMethod.orEmpty().map { it.toDomain() },
    openBalanceRial = openBalanceRial ?: 0L,
)

fun DailyPointDto.toDomain(): DailyPoint? {
    val date = IsoFormat.parseDate(date) ?: return null
    return DailyPoint(date = date, receivedRial = receivedRial ?: 0L, paidRial = paidRial ?: 0L)
}

fun FinanceSummaryDto.toDomain(): FinanceSummary = FinanceSummary(
    from = IsoFormat.parseDate(from) ?: TehranTime.today(),
    to = IsoFormat.parseDate(to) ?: TehranTime.today(),
    receivedRial = receivedRial ?: 0L,
    paidRial = paidRial ?: 0L,
    discountsRial = discountsRial ?: 0L,
    netRial = netRial ?: ((receivedRial ?: 0L) - (paidRial ?: 0L)),
    byMethod = byMethod.orEmpty().map { it.toDomain() },
    dailySeries = dailySeries.orEmpty().mapNotNull { it.toDomain() },
)

fun DebtorDto.toDomain(): DebtorItem = DebtorItem(
    guestId = guestId.orEmpty(),
    guestName = guestName.orEmpty(),
    phone = phone,
    roomNumber = roomNumber,
    balanceRial = balanceRial ?: 0L,
    lastActivityAt = IsoFormat.parseInstant(lastActivityAtUtc),
)

fun StaffDto.toDomain(): StaffMember = StaffMember(
    id = id.orEmpty(),
    fullName = fullName.orEmpty(),
    roleCode = roleCode,
    roleTitleFa = roleTitle,
    phone = phone,
    isActive = isActive ?: true,
)

fun HousekeepingTaskDto.toDomain(): HousekeepingTask = HousekeepingTask(
    id = id.orEmpty(),
    roomId = roomId.orEmpty(),
    roomNumber = roomNumber.orEmpty(),
    type = CleaningType.fromWire(type) ?: CleaningType.CHECKOUT,
    status = HousekeepingTaskStatus.fromWire(status) ?: HousekeepingTaskStatus.PENDING,
    assignedToId = assignedToId,
    assignedToName = assignedToName,
    notes = notes,
    createdAt = IsoFormat.parseInstant(createdAtUtc),
    startedAt = IsoFormat.parseInstant(startedAtUtc),
    finishedAt = IsoFormat.parseInstant(finishedAtUtc),
    approvedAt = IsoFormat.parseInstant(approvedAtUtc),
    approvedByName = approvedByName,
)

fun PerformanceDto.toDomain(): HousekeepingPerformance = HousekeepingPerformance(
    employeeId = employeeId.orEmpty(),
    employeeName = employeeName.orEmpty(),
    completedCount = completedCount ?: 0,
    averageMinutes = averageMinutes ?: 0,
    approvedCount = approvedCount ?: 0,
    rejectedCount = rejectedCount ?: 0,
)

fun MaintenanceTicketDto.toDomain(): MaintenanceTicket = MaintenanceTicket(
    id = id.orEmpty(),
    code = code.orEmpty(),
    roomId = roomId.orEmpty(),
    roomNumber = roomNumber.orEmpty(),
    title = title.orEmpty(),
    description = description,
    priority = MaintenancePriority.fromWire(priority) ?: MaintenancePriority.NORMAL,
    status = MaintenanceStatus.fromWire(status) ?: MaintenanceStatus.OPEN,
    assigneeId = assigneeId,
    assigneeName = assigneeName,
    costRial = costRial ?: 0L,
    imageUrls = imageUrls.orEmpty(),
    reporterName = reporterName,
    createdAt = IsoFormat.parseInstant(createdAtUtc),
    resolvedAt = IsoFormat.parseInstant(resolvedAtUtc),
    roomReleased = roomReleased ?: false,
)

fun AppUserDto.toDomain(): AppUser = AppUser(
    id = id.orEmpty(),
    fullName = fullName.orEmpty(),
    username = username.orEmpty(),
    roleCode = roleCode.orEmpty(),
    roleTitleFa = roleTitle ?: UserRole.fromWire(roleCode)?.labelFa.orEmpty(),
    phone = phone,
    isActive = isActive ?: true,
    lastLoginAt = IsoFormat.parseInstant(lastLoginAtUtc),
)

fun RoleDto.toDomain(): RoleInfo = RoleInfo(
    code = code.orEmpty(),
    titleFa = titleFa ?: UserRole.fromWire(code)?.labelFa.orEmpty(),
    permissions = permissions.orEmpty().toSet(),
    userCount = userCount ?: 0,
)

fun AuditLogDto.toDomain(): AuditLogEntry = AuditLogEntry(
    id = id.orEmpty(),
    action = action.orEmpty(),
    entity = entity,
    entityId = entityId,
    description = description,
    actorName = actorName,
    occurredAt = IsoFormat.parseInstant(occurredAtUtc),
)

fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id = id.orEmpty(),
    title = title.orEmpty(),
    body = body,
    severity = AlertSeverity.fromWire(severity),
    createdAt = IsoFormat.parseInstant(createdAtUtc),
    isRead = isRead ?: false,
    kind = ActivityKind.fromWire(kind),
)

fun ActivityDto.toDomain(): ActivityItem = ActivityItem(
    id = id.orEmpty(),
    title = title.orEmpty(),
    description = description,
    occurredAt = IsoFormat.parseInstant(occurredAtUtc),
    kind = ActivityKind.fromWire(kind),
    actorName = actorName,
)

fun AlertDto.toDomain(): AlertItem = AlertItem(
    id = id.orEmpty(),
    title = title.orEmpty(),
    message = message,
    severity = AlertSeverity.fromWire(severity),
    createdAt = IsoFormat.parseInstant(createdAtUtc),
)

fun OccupancyPointDto.toDomain(): OccupancyPoint? {
    val date = IsoFormat.parseDate(date) ?: return null
    return OccupancyPoint(date = date, occupied = occupied ?: 0, total = total ?: 0)
}

fun DashboardSummaryDto.toDomain(): DashboardSnapshot {
    val occupied = occupiedRooms ?: 0
    val total = totalRooms ?: 0
    val computedPercent = if (total > 0) (occupied * 100) / total else 0
    return DashboardSnapshot(
        totalRooms = total,
        freeRooms = freeRooms ?: 0,
        reservedRooms = reservedRooms ?: 0,
        occupiedRooms = occupied,
        cleaningRooms = cleaningRooms ?: 0,
        maintenanceRooms = maintenanceRooms ?: 0,
        outOfServiceRooms = outOfServiceRooms ?: 0,
        todayCheckIns = todayCheckIns ?: 0,
        todayCheckOuts = todayCheckOuts ?: 0,
        revenueTodayRial = revenueTodayRial ?: 0L,
        occupancyPercent = occupancyPercent ?: computedPercent,
        occupancySeries = occupancySeries.orEmpty().mapNotNull { it.toDomain() },
        recentActivities = recentActivities.orEmpty().map { it.toDomain() },
        alerts = alerts.orEmpty().map { it.toDomain() },
        businessDate = IsoFormat.parseDate(businessDate) ?: TehranTime.today(),
    )
}

fun ReportDto.toDomain(fallbackType: ReportType): ReportTable {
    val parsedType = ReportType.fromKey(type) ?: fallbackType
    return ReportTable(
        type = parsedType,
        title = title ?: parsedType.titleFa,
        subtitle = subtitle,
        columns = columns.orEmpty(),
        rows = rows.orEmpty(),
        totals = totals,
        notes = notes.orEmpty(),
        from = IsoFormat.parseDate(from),
        to = IsoFormat.parseDate(to),
        generatedAt = IsoFormat.parseInstant(generatedAtUtc) ?: TehranTime.nowInstant(),
    )
}

fun SessionUserDto.toDomain(): SessionUser? {
    val userId = id?.takeIf { it.isNotBlank() } ?: return null
    val role = UserRole.fromWire(role)
    return SessionUser(
        id = userId,
        fullName = fullName.orEmpty(),
        username = username.orEmpty(),
        roleCode = role?.wire ?: "",
        roleTitleFa = roleTitle ?: role?.labelFa.orEmpty(),
        permissions = permissions.orEmpty().toSet(),
    )
}

/** Localisation helper kept here so screens never show an empty string for a missing value. */
fun String?.displayOrDash(): String = this?.takeIf { it.isNotBlank() } ?: "\u2014"

fun AppError.localisedOrNull(): AppError? = this

/** Jalali short date for a business date, used by list rows. */
fun java.time.LocalDate?.jalaliShort(): String = this?.toJalali()?.formatShortFa() ?: "\u2014"
