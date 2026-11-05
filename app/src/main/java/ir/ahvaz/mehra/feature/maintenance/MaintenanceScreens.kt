package ir.ahvaz.mehra.feature.maintenance

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliDateTimeFa
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
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.PrimaryButton
import ir.ahvaz.mehra.core.ui.components.RemoteImage
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.MaintenancePriority
import ir.ahvaz.mehra.domain.model.MaintenanceStatus
import ir.ahvaz.mehra.domain.model.MaintenanceTicket
import ir.ahvaz.mehra.domain.model.StaffMember

@Composable
fun MaintenanceScreen(
    onOpenTicket: (String) -> Unit,
    onNewTicket: () -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tickets = state.tickets

    MehraScreen(
        title = "تعمیرات",
        subtitle = "ثبت خرابی، ارجاع و پیگیری",
        offline = tickets.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            if (state.canManage) {
                FloatingActionButton(onClick = onNewTicket) {
                    Icon(Icons.Rounded.Add, contentDescription = "خرابی جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChipRow(
                    items = MaintenanceStatus.entries.toList(),
                    selected = state.statusFilter,
                    onSelect = viewModel::onStatusFilterChange,
                    label = { it.labelFa },
                )
                FilterChipRow(
                    items = MaintenancePriority.entries.toList(),
                    selected = state.priorityFilter,
                    onSelect = viewModel::onPriorityFilterChange,
                    label = { it.labelFa },
                )
                FreshnessRow(
                    updatedAtMillis = tickets.updatedAtMillis,
                    isRefreshing = tickets.isRefreshing,
                    onRefresh = viewModel::refresh,
                )
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!)
                }
            }

            when {
                tickets.isLoading && tickets.data == null -> LoadingState()
                tickets.error != null && tickets.data == null -> ErrorState(tickets.error!!, viewModel::refresh)
                tickets.data.isNullOrEmpty() -> EmptyState(
                    title = "خرابی ثبت نشده است",
                    message = "با دکمه + یک گزارش خرابی جدید ثبت کنید.",
                    icon = Icons.Rounded.Handyman,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(tickets.data.orEmpty(), key = { it.id }) { ticket ->
                        TicketRow(ticket = ticket, onClick = { onOpenTicket(ticket.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketRow(ticket: MaintenanceTicket, onClick: () -> Unit) {
    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ticket.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "اتاق ${ticket.roomNumber} • کد ${ticket.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = listOfNotNull(
                        ticket.assigneeName?.let { "کارشناس: $it" },
                        ticket.createdAt.formatJalaliDateTimeFa(),
                        ticket.costRial.takeIf { it > 0 }?.let { "هزینه: ${formatMoneyFa(it)}" },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(text = ticket.priority.labelFa, tint = ticket.priority.tint())
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(text = ticket.status.labelFa, tint = ticket.status.tint())
            }
        }
    }
}

@Composable
fun MaintenanceFormScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: MaintenanceFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.addPhotos(uris.map { it.toString() })
    }

    LaunchedEffect(state.created) {
        state.created?.let { onSaved(it.id) }
    }

    MehraScreen(title = "گزارش خرابی", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (state.errorMessage != null) {
                InlineMessage(text = state.errorMessage!!, isError = !state.pendingConfirmation)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (state.photoUploadFailed) {
                InlineMessage(text = "گزارش خرابی ثبت شد، اما بارگذاری تصاویر انجام نشد.", isError = true)
                Spacer(modifier = Modifier.height(8.dp))
            }

            DropdownField(
                label = "اتاق",
                options = state.rooms,
                selected = state.selectedRoom,
                onSelect = viewModel::selectRoom,
                optionLabel = { "اتاق ${it.roomNumber} • ${it.roomTypeName}" },
                error = state.fieldErrors["roomId"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "عنوان خرابی",
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                error = state.fieldErrors["title"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "شرح خرابی",
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                singleLine = false,
                error = state.fieldErrors["description"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            DropdownField(
                label = "اولویت",
                options = MaintenancePriority.entries.toList(),
                selected = state.priority,
                onSelect = viewModel::onPriorityChange,
                optionLabel = { it.labelFa },
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "تصاویر خرابی")
            Text(
                text = "حداکثر ${("5").toPersianDigits()} تصویر. تصاویر پس از ثبت گزارش به سرور ارسال می‌شوند.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.photoUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photoUris) { uri ->
                        Row(verticalAlignment = Alignment.Top) {
                            LocalPhotoThumb(uri = uri)
                            IconButton(onClick = { viewModel.removePhoto(uri) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "حذف تصویر")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            CompactButton(
                text = "افزودن تصویر از گالری",
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                icon = Icons.Rounded.PhotoCamera,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "ثبت گزارش",
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
            if (state.pendingConfirmation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "گزارش تا تأیید سرور قطعی نیست.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MehraTheme.colors.warning,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceDetailScreen(
    onBack: () -> Unit,
    viewModel: MaintenanceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ticketState = state.ticket
    val ticket = ticketState.data
    var showAssign by remember { mutableStateOf(false) }
    var showCost by remember { mutableStateOf(false) }

    MehraScreen(
        title = ticket?.title ?: "جزئیات خرابی",
        subtitle = ticket?.code,
        onBack = onBack,
    ) { padding ->
        when {
            ticketState.isLoading && ticket == null -> LoadingState()
            ticketState.error != null && ticket == null -> ErrorState(ticketState.error!!, viewModel::refresh)
            ticket == null -> EmptyState(title = "خرابی یافت نشد")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ticket.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "اتاق ${ticket.roomNumber} • گزارش‌دهنده: ${ticket.reporterName ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(text = ticket.priority.labelFa, tint = ticket.priority.tint())
                        StatusPill(text = ticket.status.labelFa, tint = ticket.status.tint())
                        if (ticket.roomReleased) {
                            StatusPill(text = "اتاق آزاد شد", tint = MehraTheme.colors.success)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("شرح", ticket.description ?: "—")
                    InfoRow("کارشناس", ticket.assigneeName ?: "تخصیص نیافته")
                    InfoRow("هزینه", formatMoneyFa(ticket.costRial))
                    InfoRow("ثبت", ticket.createdAt.formatJalaliDateTimeFa())
                    InfoRow("رفع", ticket.resolvedAt.formatJalaliDateTimeFa())
                }

                if (ticket.imageUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(title = "تصاویر خرابی")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ticket.imageUrls) { url ->
                            RemoteImage(url = url, contentDescription = "تصویر خرابی", size = 110.dp)
                        }
                    }
                }

                if (state.canManage) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(title = "اقدامات")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactButton(
                            text = if (ticket.assigneeId == null) "تخصیص کارشناس" else "تغییر کارشناس",
                            onClick = { showAssign = true },
                            enabled = !state.isActing,
                        )
                        MaintenanceStatus.entries.forEach { status ->
                            if (status != ticket.status) {
                                CompactButton(
                                    text = "وضعیت: ${status.labelFa}",
                                    onClick = { viewModel.updateStatus(status) },
                                    enabled = !state.isActing,
                                )
                            }
                        }
                        CompactButton(
                            text = "ثبت هزینه",
                            onClick = { showCost = true },
                            enabled = !state.isActing,
                        )
                        if (!ticket.roomReleased && ticket.status == MaintenanceStatus.RESOLVED) {
                            CompactButton(
                                text = "آزادسازی اتاق",
                                onClick = viewModel::releaseRoom,
                                enabled = !state.isActing,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAssign) {
        AssignTechnicianDialog(
            technicians = state.technicians,
            onDismiss = { showAssign = false },
            onConfirm = { id ->
                viewModel.assign(id)
                showAssign = false
            },
        )
    }

    if (showCost) {
        CostDialog(
            onDismiss = { showCost = false },
            onConfirm = { amount, description ->
                viewModel.registerCost(amount, description)
                showCost = false
            },
        )
    }
}

@Composable
private fun AssignTechnicianDialog(
    technicians: List<StaffMember>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf<StaffMember?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تخصیص کارشناس تعمیرات") },
        text = {
            if (technicians.isEmpty()) {
                Text("فهرست کارشناسان از سرور دریافت نشد.")
            } else {
                DropdownField(
                    label = "کارشناس",
                    options = technicians,
                    selected = selected,
                    onSelect = { selected = it },
                    optionLabel = { it.fullName },
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
private fun CostDialog(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت هزینه تعمیرات") },
        text = {
            Column {
                AmountField(label = "مبلغ (ریال)", value = amount, onValueChange = { amount = it })
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "توضیح",
                    value = description,
                    onValueChange = { description = it },
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount, description.takeIf { it.isNotBlank() }) }) {
                Text("ثبت")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}


/** Preview for a photo that is still local (content://) and not yet uploaded. */
@Composable
private fun LocalPhotoThumb(uri: String, size: androidx.compose.ui.unit.Dp = 72.dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(uri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
