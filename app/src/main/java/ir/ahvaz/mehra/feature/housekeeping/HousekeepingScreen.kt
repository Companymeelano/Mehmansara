package ir.ahvaz.mehra.feature.housekeeping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliDateTimeFa
import ir.ahvaz.mehra.core.date.relativeTimeFa
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FilterChipRow
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.components.HorizontalBarList
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.CleaningType
import ir.ahvaz.mehra.domain.model.HousekeepingTask
import ir.ahvaz.mehra.domain.model.HousekeepingTaskStatus
import ir.ahvaz.mehra.domain.model.Room
import ir.ahvaz.mehra.domain.model.StaffMember

@Composable
fun HousekeepingScreen(viewModel: HousekeepingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var taskToAssign by remember { mutableStateOf<HousekeepingTask?>(null) }
    var taskToComplete by remember { mutableStateOf<HousekeepingTask?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    MehraScreen(
        title = "خدمات و نظافت",
        subtitle = "درخواست نظافت، تخصیص و تأیید",
        offline = state.tasks.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = {
                viewModel.refresh()
                viewModel.loadPerformance()
            }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChipRow(
                    items = listOf(0, 1, 2),
                    selected = tab,
                    onSelect = { tab = it ?: 0 },
                    label = { when (it) { 0 -> "درخواست‌ها"; 1 -> "نیازمند نظافت"; else -> "عملکرد" } },
                    allLabel = null,
                )
                if (tab == 0) {
                    FilterChipRow(
                        items = HousekeepingTaskStatus.entries.toList(),
                        selected = state.statusFilter,
                        onSelect = viewModel::onStatusFilterChange,
                        label = { it.labelFa },
                    )
                    FreshnessRow(
                        updatedAtMillis = state.tasks.updatedAtMillis,
                        isRefreshing = state.tasks.isRefreshing,
                        onRefresh = viewModel::refresh,
                    )
                }
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                }
                if (state.canManage && tab != 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactButton(
                        text = "ثبت درخواست نظافت",
                        onClick = { showCreate = true },
                        icon = Icons.Rounded.CleaningServices,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            when (tab) {
                0 -> TasksTab(
                    state = state,
                    viewModel = viewModel,
                    onAssign = { taskToAssign = it },
                    onComplete = { taskToComplete = it },
                )

                1 -> CleaningRoomsTab(rooms = state.cleaningRooms, canManage = state.canManage, onCreate = { showCreate = true })
                else -> PerformanceTab(state = state)
            }
        }
    }

    taskToAssign?.let { task ->
        AssignDialog(
            task = task,
            staff = state.staff,
            onDismiss = { taskToAssign = null },
            onConfirm = { staffId ->
                viewModel.assign(task, staffId)
                taskToAssign = null
            },
        )
    }

    taskToComplete?.let { task ->
        CompleteDialog(
            task = task,
            onDismiss = { taskToComplete = null },
            onConfirm = { note ->
                viewModel.complete(task, note)
                taskToComplete = null
            },
        )
    }

    if (showCreate) {
        CreateCleaningDialog(
            rooms = state.allRooms,
            staff = state.staff,
            onDismiss = { showCreate = false },
            onConfirm = { roomId, type, assignee, notes ->
                viewModel.createCleaning(roomId, type, assignee, notes)
                showCreate = false
            },
        )
    }
}

@Composable
private fun TasksTab(
    state: HousekeepingUiState,
    viewModel: HousekeepingViewModel,
    onAssign: (HousekeepingTask) -> Unit,
    onComplete: (HousekeepingTask) -> Unit,
) {
    val tasks = state.tasks
    when {
        tasks.isLoading && tasks.data == null -> LoadingState()
        tasks.error != null && tasks.data == null -> ErrorState(tasks.error!!, viewModel::refresh)
        tasks.data.isNullOrEmpty() -> EmptyState(
            title = "درخواستی وجود ندارد",
            message = "درخواست نظافت جدیدی برای اتاق‌ها ثبت نشده است.",
            icon = Icons.Rounded.CleaningServices,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tasks.data.orEmpty(), key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    canManage = state.canManage,
                    busy = state.actionInProgress,
                    onAssign = { onAssign(task) },
                    onStart = { viewModel.start(task) },
                    onComplete = { onComplete(task) },
                    onApprove = { viewModel.approve(task) },
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: HousekeepingTask,
    canManage: Boolean,
    busy: Boolean,
    onAssign: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onApprove: () -> Unit,
) {
    MehraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "اتاق ${task.roomNumber} • ${task.type.labelFa}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOfNotNull(
                        task.assignedToName?.let { "نظافت‌کار: $it" } ?: "بدون تخصیص",
                        task.createdAt.relativeTimeFa().takeIf { it.isNotBlank() },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!task.notes.isNullOrBlank()) {
                    Text(
                        text = task.notes!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (task.finishedAt != null) {
                    Text(
                        text = "پایان: ${task.finishedAt.formatJalaliDateTimeFa()}" +
                            (task.approvedByName?.let { " • تأیید: $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusPill(text = task.status.labelFa, tint = task.status.tint())
        }

        if (canManage) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (task.status) {
                    HousekeepingTaskStatus.PENDING -> CompactButton(
                        text = "تخصیص",
                        onClick = onAssign,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )

                    HousekeepingTaskStatus.ASSIGNED -> Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompactButton(text = "شروع", onClick = onStart, enabled = !busy, modifier = Modifier.weight(1f))
                        CompactButton(text = "تغییر تخصیص", onClick = onAssign, enabled = !busy, modifier = Modifier.weight(1f))
                    }

                    HousekeepingTaskStatus.IN_PROGRESS -> CompactButton(
                        text = "پایان نظافت",
                        onClick = onComplete,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )

                    HousekeepingTaskStatus.DONE -> CompactButton(
                        text = "تأیید آماده‌سازی",
                        onClick = onApprove,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CleaningRoomsTab(rooms: List<Room>, canManage: Boolean, onCreate: () -> Unit) {
    if (rooms.isEmpty()) {
        EmptyState(
            title = "اتاقی نیازمند نظافت نیست",
            message = "اتاق‌های دارای وضعیت «در حال نظافت» در این فهرست نمایش داده می‌شوند.",
            icon = Icons.Rounded.CleaningServices,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rooms, key = { it.id }) { room ->
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = room.titleFa,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "طبقه ${toPersianDigits(room.floor.toString())} • ${room.roomTypeName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(text = room.status.labelFa, tint = room.status.tint())
                }
                if (canManage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactButton(text = "ثبت درخواست نظافت", onClick = onCreate, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PerformanceTab(state: HousekeepingUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionHeader(title = "عملکرد این ماه")
        if (state.performance.isEmpty()) {
            Text(
                text = "داده عملکردی برای این ماه ثبت نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        HorizontalBarList(
            items = state.performance.map { it.employeeName to it.completedCount.toLong() },
            valueFormatter = { "${toPersianDigits(it.toString())} مورد" },
        )
        Spacer(modifier = Modifier.height(16.dp))
        state.performance.forEach { performance ->
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = performance.employeeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "تکمیل‌شده: ${toPersianDigits(performance.completedCount.toString())} • " +
                        "میانگین زمان: ${toPersianDigits(performance.averageMinutes.toString())} دقیقه • " +
                        "نرخ تأیید: ${toPersianDigits(performance.approvalRatePercent.toString())}٪",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AssignDialog(
    task: HousekeepingTask,
    staff: List<StaffMember>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf<StaffMember?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تخصیص نظافت اتاق ${task.roomNumber}") },
        text = {
            if (staff.isEmpty()) {
                Text("فهرست نظافت‌کاران از سرور دریافت نشد. اتصال را بررسی کنید.")
            } else {
                DropdownField(
                    label = "نظافت‌کار",
                    options = staff,
                    selected = selected,
                    onSelect = { selected = it },
                    optionLabel = { member -> listOfNotNull(member.fullName, member.roleTitleFa).joinToString(" — ") },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = selected != null, onClick = { selected?.let { onConfirm(it.id) } }) {
                Text("تخصیص")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun CompleteDialog(task: HousekeepingTask, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پایان نظافت اتاق ${task.roomNumber}") },
        text = {
            MehraTextField(
                label = "یادداشت (اختیاری)",
                value = note,
                onValueChange = { note = it },
                singleLine = false,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note.takeIf { it.isNotBlank() }) }) { Text("ثبت پایان") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun CreateCleaningDialog(
    rooms: List<Room>,
    staff: List<StaffMember>,
    onDismiss: () -> Unit,
    onConfirm: (String, CleaningType, String?, String?) -> Unit,
) {
    var room by remember { mutableStateOf<Room?>(rooms.firstOrNull()) }
    var type by remember { mutableStateOf(CleaningType.CHECKOUT) }
    var assignee by remember { mutableStateOf<StaffMember?>(null) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("درخواست نظافت") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DropdownField(
                    label = "اتاق",
                    options = rooms,
                    selected = room,
                    onSelect = { room = it },
                    optionLabel = { it.titleFa },
                )
                if (rooms.isEmpty()) {
                    Text(
                        text = "فهرست اتاق‌ها از سرور دریافت نشد؛ اتصال را بررسی کنید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MehraTheme.colors.warning,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                DropdownField(
                    label = "نوع نظافت",
                    options = CleaningType.entries.toList(),
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it.labelFa },
                )
                Spacer(modifier = Modifier.height(10.dp))
                DropdownField(
                    label = "نظافت‌کار (اختیاری)",
                    options = staff,
                    selected = assignee,
                    onSelect = { assignee = it },
                    optionLabel = { it.fullName },
                )
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "یادداشت",
                    value = notes,
                    onValueChange = { notes = it },
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = room != null,
                onClick = {
                    room?.let { onConfirm(it.id, type, assignee?.id, notes.takeIf { text -> text.isNotBlank() }) }
                },
            ) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
