package ir.ahvaz.mehra.feature.rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliFaOrDash
import ir.ahvaz.mehra.core.ui.components.AmountField
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FilterChipRow
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InfoRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraSearchField
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.PrimaryButton
import ir.ahvaz.mehra.core.ui.components.SecondaryButton
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.components.SwitchRow
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.CleaningType
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.RoomStatus

@Composable
fun RoomsScreen(
    onOpenRoom: (String) -> Unit,
    onCreateRoom: () -> Unit,
    viewModel: RoomsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rooms = state.rooms

    MehraScreen(
        title = "مدیریت اتاق‌ها",
        subtitle = if (state.floor != null) "طبقه ${(state.floor.toString()).toPersianDigits()}" else null,
        offline = rooms.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            if (state.canManage) {
                FloatingActionButton(onClick = onCreateRoom) {
                    Icon(Icons.Rounded.Add, contentDescription = "اتاق جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                MehraSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "جست‌وجوی شماره اتاق…",
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilterChipRow(
                    items = RoomStatus.entries,
                    selected = state.status,
                    onSelect = viewModel::onStatusChange,
                    label = { it.labelFa },
                )
                if (state.floors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FilterChipRow(
                        items = state.floors,
                        selected = state.floor,
                        onSelect = viewModel::onFloorChange,
                        label = { "طبقه ${(it.toString()).toPersianDigits()}" },
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FreshnessRow(
                    updatedAtMillis = rooms.updatedAtMillis,
                    isRefreshing = rooms.isRefreshing,
                    onRefresh = viewModel::refresh,
                )
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                }
            }

            when {
                rooms.isLoading && rooms.data == null -> LoadingState()
                rooms.error != null && rooms.data == null -> ErrorState(error = rooms.error!!, onRetry = viewModel::refresh)
                rooms.data.isNullOrEmpty() -> {
                    EmptyState(
                        title = "اتاقی یافت نشد",
                        message = if (state.query.isBlank()) "برای شروع، اتاق‌های مجموعه را ثبت کنید." else "نتیجه‌ای برای جست‌وجو پیدا نشد.",
                        icon = Icons.Rounded.Bed,
                    )
                    if (state.canManage && state.query.isBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            text = "بارگذاری پیش‌فرض اتاق‌ها (۱ تا ۱۷، ۲۰۷، ۲۱۱)",
                            onClick = viewModel::seedDefaultRooms,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            loading = state.actionInProgress,
                        )
                    }
                }

                else -> {
                    val all = rooms.data.orEmpty()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RoomSummaryStrip(rooms = all)
                        }
                        all.groupBy { it.floor }
                            .toSortedMap()
                            .forEach { (floor, floorRooms) ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader(
                                        title = "طبقه ${(floor.toString()).toPersianDigits()}",
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                                    )
                                }
                                items(
                                    floorRooms.sortedWith(
                                        compareBy({ it.roomNumber.toIntOrNull() ?: Int.MAX_VALUE }, { it.roomNumber })
                                    ),
                                    key = { it.id },
                                ) { room ->
                                    LuxuryRoomCard(room = room, onClick = { onOpenRoom(room.id) })
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxuryRoomCard(room: Room, onClick: () -> Unit) {
    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = room.roomNumber.toPersianDigits(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MehraTheme.colors.gold,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "اتاق",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        StatusPill(text = room.status.labelFa, tint = room.status.tint())
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = room.roomTypeName.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${(room.capacity.toString()).toPersianDigits()} تخت • طبقه ${(room.floor.toString()).toPersianDigits()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatMoneyFa(room.ratePerNightRial) + " / شب",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MehraTheme.colors.gold,
        )
        if (!room.currentGuestName.isNullOrBlank()) {
            Text(
                text = "مهمان: ${room.currentGuestName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RoomSummaryStrip(rooms: List<Room>) {
    val total = rooms.size
    val free = rooms.count { it.status == RoomStatus.FREE }
    val occupied = rooms.count { it.status == RoomStatus.OCCUPIED }
    val reserved = rooms.count { it.status == RoomStatus.RESERVED }
    val cleaning = rooms.count { it.status == RoomStatus.CLEANING }
    val blocked = rooms.count { it.status == RoomStatus.MAINTENANCE || it.status == RoomStatus.OUT_OF_SERVICE }
    val occupancy = if (total == 0) 0 else occupied * 100 / total
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryChip(label = "کل اتاق‌ها", value = total.toString().toPersianDigits(), tint = MehraTheme.colors.gold)
            SummaryChip(label = "آزاد", value = free.toString().toPersianDigits(), tint = RoomStatus.FREE.tint())
            SummaryChip(label = "اشغال", value = occupied.toString().toPersianDigits(), tint = RoomStatus.OCCUPIED.tint())
            SummaryChip(label = "رزرو", value = reserved.toString().toPersianDigits(), tint = RoomStatus.RESERVED.tint())
            SummaryChip(label = "نظافت", value = cleaning.toString().toPersianDigits(), tint = RoomStatus.CLEANING.tint())
            SummaryChip(label = "تعمیر", value = blocked.toString().toPersianDigits(), tint = RoomStatus.MAINTENANCE.tint())
        }
        if (total > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "درصد اشغال",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${occupancy.toString().toPersianDigits()}٪",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MehraTheme.colors.gold,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { occupancy / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MehraTheme.colors.gold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = if (MehraTheme.colors.isDark) 0.20f else 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RoomDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: RoomDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val roomState = state.room
    val room = roomState.data

    var showCleaningDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }

    MehraScreen(
        title = if (room != null) "اتاق ${room.roomNumber}" else "جزئیات اتاق",
        subtitle = room?.roomTypeName,
        onBack = onBack,
        actions = {
            if (state.canManage && room != null) {
                IconButton(onClick = { onEdit(room.id) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "ویرایش")
                }
            }
        },
    ) { padding ->
        when {
            roomState.isLoading && room == null -> LoadingState()
            roomState.error != null && room == null -> ErrorState(roomState.error!!, viewModel::refresh)
            room == null -> EmptyState(title = "اتاق یافت نشد")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
            ) {
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "وضعیت فعلی",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(text = room.status.labelFa, tint = room.status.tint())
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
                        }
                    }
                    InfoRow("شماره اتاق", (room.roomNumber).toPersianDigits())
                    InfoRow("نوع اتاق", room.roomTypeName.ifBlank { "—" })
                    InfoRow("طبقه", (room.floor.toString()).toPersianDigits())
                    InfoRow("ظرفیت", "${(room.capacity.toString()).toPersianDigits()} نفر")
                    InfoRow("تعرفه شبانه", formatMoneyFa(room.ratePerNightRial))
                    if (room.amenities.isNotEmpty()) {
                        InfoRow("امکانات", room.amenities.joinToString("، "))
                    }
                    if (!room.notes.isNullOrBlank()) InfoRow("یادداشت", room.notes)
                    InfoRow("مهمان فعلی", room.currentGuestName ?: "—")
                    InfoRow("رزرو بعدی", room.nextReservationAt.formatJalaliFaOrDash())
                }

                if (state.canManage) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "تغییر وضعیت اتاق")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoomStatus.entries.take(3).forEach { status ->
                            CompactButton(
                                text = status.labelFa,
                                onClick = { viewModel.changeStatus(status) },
                                modifier = Modifier.weight(1f),
                                enabled = status != room.status && !state.actionInProgress,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoomStatus.entries.drop(3).forEach { status ->
                            CompactButton(
                                text = status.labelFa,
                                onClick = { viewModel.changeStatus(status) },
                                modifier = Modifier.weight(1f),
                                enabled = status != room.status && !state.actionInProgress,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "عملیات")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            text = "درخواست نظافت",
                            onClick = { showCleaningDialog = true },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.CleaningServices,
                        )
                        SecondaryButton(
                            text = "ثبت خرابی",
                            onClick = { showMaintenanceDialog = true },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Handyman,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "سوابق اشغال")
                when {
                    state.history.isLoading -> LoadingState()
                    state.history.error != null -> InlineMessage(text = state.history.error!!.userMessageFa)
                    state.history.data.isNullOrEmpty() -> Text(
                        text = "سابقه‌ای برای این اتاق ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> state.history.data.orEmpty().forEach { entry ->
                        MehraCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.guestName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${entry.checkIn.formatJalaliFaOrDash()} تا ${entry.checkOut.formatJalaliFaOrDash()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatMoneyFa(entry.totalRial),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    StatusPill(text = entry.status.labelFa, tint = entry.status.tint())
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCleaningDialog && room != null) {
        CleaningRequestDialog(
            roomNumber = room.roomNumber,
            onDismiss = { showCleaningDialog = false },
            onSubmit = { type, notes ->
                viewModel.requestCleaning(type, notes)
                showCleaningDialog = false
            },
        )
    }

    if (showMaintenanceDialog && room != null) {
        MaintenanceRequestDialog(
            roomNumber = room.roomNumber,
            onDismiss = { showMaintenanceDialog = false },
            onSubmit = { title, description, priority ->
                viewModel.reportMaintenance(title, description, priority)
                showMaintenanceDialog = false
            },
        )
    }
}

@Composable
private fun CleaningRequestDialog(
    roomNumber: String,
    onDismiss: () -> Unit,
    onSubmit: (CleaningType, String?) -> Unit,
) {
    var type by remember { mutableStateOf(CleaningType.CHECKOUT) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("درخواست نظافت اتاق $roomNumber") },
        text = {
            Column {
                DropdownField(
                    label = "نوع نظافت",
                    options = CleaningType.entries,
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it.labelFa },
                )
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "توضیحات (اختیاری)",
                    value = notes,
                    onValueChange = { notes = it },
                    singleLine = false,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(type, notes.takeIf { it.isNotBlank() }) }) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun MaintenanceRequestDialog(
    roomNumber: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, MaintenancePriority) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(MaintenancePriority.NORMAL) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت خرابی اتاق $roomNumber") },
        text = {
            Column {
                MehraTextField(label = "عنوان خرابی", value = title, onValueChange = { title = it })
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "توضیحات",
                    value = description,
                    onValueChange = { description = it },
                    singleLine = false,
                )
                Spacer(modifier = Modifier.height(10.dp))
                DropdownField(
                    label = "اولویت",
                    options = MaintenancePriority.entries,
                    selected = priority,
                    onSelect = { priority = it },
                    optionLabel = { it.labelFa },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onSubmit(title, description, priority) },
                enabled = title.isNotBlank(),
            ) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun RoomFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RoomFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft

    if (state.saved) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onSaved() }
    }

    MehraScreen(
        title = if (draft.isEdit) "ویرایش اتاق" else "اتاق جدید",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (state.errorMessage != null) {
                InlineMessage(text = state.errorMessage!!)
                Spacer(modifier = Modifier.height(8.dp))
            }
            MehraTextField(
                label = "شماره اتاق",
                value = draft.roomNumber,
                onValueChange = { value -> viewModel.updateDraft { it.copy(roomNumber = value) } },
                error = state.fieldErrors["roomNumber"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MehraTextField(
                    label = "طبقه",
                    value = draft.floor,
                    onValueChange = { value -> viewModel.updateDraft { it.copy(floor = value) } },
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    error = state.fieldErrors["floor"],
                )
                MehraTextField(
                    label = "ظرفیت",
                    value = draft.capacity,
                    onValueChange = { value -> viewModel.updateDraft { it.copy(capacity = value) } },
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    error = state.fieldErrors["capacity"],
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DropdownField(
                label = "نوع اتاق",
                options = state.lookups.roomTypes,
                selected = state.lookups.roomTypes.firstOrNull { it.id == draft.roomTypeId },
                onSelect = { item -> viewModel.updateDraft { it.copy(roomTypeId = item.id) } },
                optionLabel = { it.title },
                error = state.fieldErrors["roomTypeId"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            AmountField(
                label = "تعرفه شبانه (ریال)",
                value = draft.ratePerNightRial,
                onValueChange = { value -> viewModel.updateDraft { it.copy(ratePerNightRial = value) } },
                error = state.fieldErrors["rate"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            DropdownField(
                label = "وضعیت",
                options = RoomStatus.entries,
                selected = draft.status,
                onSelect = { status -> viewModel.updateDraft { it.copy(status = status) } },
                optionLabel = { it.labelFa },
            )

            if (state.lookups.amenities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "امکانات")
                state.lookups.amenities.forEach { amenity ->
                    SwitchRow(
                        title = amenity.title,
                        checked = draft.amenities.contains(amenity.id),
                        onCheckedChange = { viewModel.toggleAmenity(amenity.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "یادداشت",
                value = draft.notes,
                onValueChange = { value -> viewModel.updateDraft { it.copy(notes = value) } },
                singleLine = false,
            )

            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = if (draft.isEdit) "ذخیره تغییرات" else "ثبت اتاق",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
        }
    }
}
