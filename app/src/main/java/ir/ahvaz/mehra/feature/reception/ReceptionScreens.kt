package ir.ahvaz.mehra.feature.reception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliFaOrDash
import ir.ahvaz.mehra.core.date.formatJalaliLongFa
import ir.ahvaz.mehra.core.date.nightsLabelFa
import ir.ahvaz.mehra.core.export.ReportExporter
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
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.components.SwitchRow
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Receipt
import ir.ahvaz.mehra.domain.model.Stay

/**
 * Reception desk: current in-house stays plus a reservation search that leads to check-in.
 */
@Composable
fun ReceptionScreen(
    onCheckIn: (String) -> Unit,
    onCheckOut: (String) -> Unit,
    viewModel: ReceptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stays = state.stays
    var mode by remember { mutableStateOf(0) }
    var extendStay by remember { mutableStateOf<Stay?>(null) }
    var moveStay by remember { mutableStateOf<Stay?>(null) }

    MehraScreen(
        title = "پذیرش",
        subtitle = "پذیرش، خروج و تسویه حساب",
        offline = stays.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChipRow(
                    items = listOf(0, 1),
                    selected = mode,
                    onSelect = { mode = it ?: 0 },
                    label = { if (it == 0) "اقامت‌های جاری" else "جست‌وجوی رزرو" },
                    allLabel = null,
                )
                if (mode == 1) {
                    MehraSearchField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        placeholder = "نام مهمان، تلفن یا کد رزرو…",
                    )
                } else {
                    FreshnessRow(
                        updatedAtMillis = stays.updatedAtMillis,
                        isRefreshing = stays.isRefreshing,
                        onRefresh = viewModel::refresh,
                    )
                }
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!)
                }
            }

            if (mode == 0) {
                when {
                    stays.isLoading && stays.data == null -> LoadingState()
                    stays.error != null && stays.data == null -> ErrorState(stays.error!!, viewModel::refresh)
                    stays.data.isNullOrEmpty() -> EmptyState(
                        title = "مهمان در اقامت نیست",
                        message = "برای پذیرش، از تب «جست‌وجوی رزرو» رزرو مورد نظر را پیدا کنید.",
                        icon = Icons.Rounded.Hotel,
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(stays.data.orEmpty(), key = { it.id }) { stay ->
                            StayRow(
                                stay = stay,
                                onCheckOut = { onCheckOut(stay.id) },
                                onExtend = {
                                    viewModel.loadRooms()
                                    extendStay = stay
                                },
                                onMoveRoom = {
                                    viewModel.loadRooms()
                                    moveStay = stay
                                },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.isSearching) {
                        item { LoadingState(message = "در حال جست‌وجو…") }
                    }
                    items(state.searchResults, key = { it.id }) { reservation ->
                        MehraCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reservation.guestName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "کد ${reservation.code} • ${reservation.roomNumber ?: "بدون اتاق"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "${reservation.checkIn.formatJalaliFaOrDash()} تا ${reservation.checkOut.formatJalaliFaOrDash()}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                StatusPill(text = reservation.status.labelFa, tint = reservation.status.tint())
                            }
                            if (reservation.status == ir.ahvaz.mehra.domain.model.ReservationStatus.CONFIRMED ||
                                reservation.status == ir.ahvaz.mehra.domain.model.ReservationStatus.PENDING
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CompactButton(
                                    text = "پذیرش مهمان",
                                    onClick = { onCheckIn(reservation.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    if (!state.isSearching && state.searchResults.isEmpty() && state.searchQuery.length >= 2) {
                        item {
                            EmptyState(
                                title = "رزروی یافت نشد",
                                message = "شماره رزرو یا نام مهمان را بررسی کنید.",
                            )
                        }
                    }
                }
            }
        }
    }

    extendStay?.let { stay ->
        ExtendStayDialog(
            stay = stay,
            onDismiss = { extendStay = null },
            onConfirm = { newDate ->
                viewModel.extendStay(stay.id, newDate.toGregorian())
                extendStay = null
            },
        )
    }

    moveStay?.let { stay ->
        MoveRoomDialog(
            stay = stay,
            rooms = state.rooms.filter { room -> room.id != stay.roomId && room.status.isAssignable },
            onDismiss = { moveStay = null },
            onConfirm = { roomId, reason ->
                viewModel.moveRoom(stay.id, roomId, reason)
                moveStay = null
            },
        )
    }
}

@Composable
private fun StayRow(stay: Stay, onCheckOut: () -> Unit, onExtend: () -> Unit, onMoveRoom: () -> Unit) {
    MehraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stay.guestName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "اتاق ${stay.roomNumber} • ${toPersianDigits(stay.guestsCount.toString())} نفر • خروج برنامه‌ریزی‌شده ${stay.plannedCheckOut.formatJalaliLongFa()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "مانده حساب: ${formatMoneyFa(stay.balanceRial)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stay.balanceRial > 0) MehraTheme.colors.warning else MehraTheme.colors.success,
                )
            }
            StatusPill(text = stay.status.labelFa, tint = stay.status.tint())
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactButton(text = "تسویه و خروج", onClick = onCheckOut, modifier = Modifier.weight(1f))
            CompactButton(text = "تمدید", onClick = onExtend, modifier = Modifier.weight(1f))
            CompactButton(text = "تغییر اتاق", onClick = onMoveRoom, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExtendStayDialog(
    stay: Stay,
    onDismiss: () -> Unit,
    onConfirm: (ir.ahvaz.mehra.core.date.JalaliDate) -> Unit,
) {
    var newDate by remember { mutableStateOf(ir.ahvaz.mehra.core.date.JalaliDate.from(stay.plannedCheckOut).plusDays(1)) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تمدید اقامت ${stay.guestName}") },
        text = {
            Column {
                Text(
                    text = "خروج فعلی: ${stay.plannedCheckOut.formatJalaliLongFa()}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                JalaliDateField(
                    label = "خروج جدید",
                    value = newDate,
                    onDateSelected = { newDate = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newDate) }) { Text("ثبت تمدید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun MoveRoomDialog(
    stay: Stay,
    rooms: List<ir.ahvaz.mehra.domain.model.Room>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var selected by remember { mutableStateOf<ir.ahvaz.mehra.domain.model.Room?>(null) }
    var reason by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر اتاق ${stay.guestName}") },
        text = {
            Column {
                if (rooms.isEmpty()) {
                    Text("اتاق آزاد دیگری برای انتقال وجود ندارد.")
                } else {
                    DropdownField(
                        label = "اتاق جدید",
                        options = rooms,
                        selected = selected,
                        onSelect = { selected = it },
                        optionLabel = { "اتاق ${it.roomNumber} • ${it.roomTypeName}" },
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "دلیل جابه‌جایی",
                    value = reason,
                    onValueChange = { reason = it },
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let { onConfirm(it.id, reason.takeIf { text -> text.isNotBlank() }) } },
            ) { Text("انتقال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun CheckInScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reservation = state.reservation

    if (state.completedStay != null) {
        LaunchedEffect(state.completedStay) { onCompleted() }
    }

    MehraScreen(
        title = "پذیرش مهمان",
        subtitle = reservation?.guestName,
        onBack = onBack,
    ) { padding ->
        when {
            state.isLoading -> LoadingState()
            reservation == null -> ErrorState(
                error = ir.ahvaz.mehra.core.common.AppError.notFound("رزرو یافت نشد."),
                onRetry = null,
            )

            else -> Column(
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

                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Text("خلاصه رزرو", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    InfoRow("کد رزرو", reservation.code)
                    InfoRow("مهمان", reservation.guestName)
                    InfoRow("ورود", reservation.checkIn.formatJalaliLongFa())
                    InfoRow("خروج", reservation.checkOut.formatJalaliLongFa())
                    InfoRow("مدت", nightsLabelFa(reservation.nights))
                    InfoRow("مانده", formatMoneyFa(reservation.remainingRial))
                }

                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "تخصیص اتاق")
                DropdownField(
                    label = "اتاق",
                    options = state.rooms,
                    selected = state.rooms.firstOrNull { it.id == state.selectedRoomId },
                    onSelect = { room -> viewModel.selectRoom(room.id) },
                    optionLabel = { "اتاق ${it.roomNumber} • ${it.roomTypeName} • ${it.status.labelFa}" },
                )
                if (state.rooms.isEmpty()) {
                    Text(
                        text = "هیچ اتاق آزادی برای تخصیص موجود نیست.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MehraTheme.colors.warning,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "مدارک تحویل‌شده")
                if (state.documentTypes.isEmpty()) {
                    Text(
                        text = "فهرست مدارک از سرور دریافت نشد.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.documentTypes.forEach { document ->
                        SwitchRow(
                            title = document,
                            checked = state.selectedDocuments.contains(document),
                            onCheckedChange = { viewModel.toggleDocument(document) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "همراهان", actionLabel = "افزودن", onAction = viewModel::addCompanion)
                state.companions.forEachIndexed { index, companion ->
                    MehraCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "همراه ${toPersianDigits((index + 1).toString())}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.removeCompanion(index) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "حذف")
                            }
                        }
                        MehraTextField(
                            label = "نام و نام خانوادگی",
                            value = companion.fullName,
                            onValueChange = { value ->
                                viewModel.updateCompanion(index) { it.copy(fullName = value) }
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MehraTextField(
                            label = "کد ملی",
                            value = companion.nationalId.orEmpty(),
                            onValueChange = { value ->
                                viewModel.updateCompanion(index) { it.copy(nationalId = value) }
                            },
                            numeric = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "پرداخت اولیه")
                AmountField(
                    label = "مبلغ دریافتی (ریال)",
                    value = state.initialPayment,
                    onValueChange = viewModel::updateInitialPayment,
                    hint = "در صورت عدم پرداخت، خالی بگذارید.",
                )
                Spacer(modifier = Modifier.height(10.dp))
                DropdownField(
                    label = "روش پرداخت",
                    options = PaymentMethod.entries,
                    selected = state.paymentMethod,
                    onSelect = viewModel::selectPaymentMethod,
                    optionLabel = { it.labelFa },
                )
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "یادداشت",
                    value = state.note,
                    onValueChange = viewModel::updateNote,
                    singleLine = false,
                )

                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = "ثبت پذیرش",
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSubmitting,
                    icon = Icons.Rounded.CheckCircle,
                )
                if (state.pendingConfirmation) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "پذیرش تا تأیید سرور قطعی نیست و در فهرست عملیات در انتظار باقی می‌ماند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MehraTheme.colors.warning,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CheckOutScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: CheckOutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val reportExporter = remember(context) { ReportExporter(context) }
    var receiptToShow by remember { mutableStateOf<Receipt?>(null) }

    MehraScreen(
        title = "تسویه و خروج",
        subtitle = state.stay?.guestName,
        onBack = onBack,
    ) { padding ->
        when {
            state.isLoading -> LoadingState()
            state.receipt != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    MehraCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "خروج با موفقیت ثبت شد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MehraTheme.colors.success,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val receipt = state.receipt!!
                        InfoRow("شماره رسید", receipt.receiptNumber)
                        InfoRow("مبلغ کل", formatMoneyFa(receipt.totalRial))
                        InfoRow("پرداخت‌شده", formatMoneyFa(receipt.paidRial))
                        InfoRow("مانده", formatMoneyFa(receipt.balanceRial))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CompactButton(
                                text = "اشتراک رسید PDF",
                                onClick = {
                                    val file = reportExporter.receiptPdf(receipt)
                                    reportExporter.share(file, ReportExporter.MIME_PDF, "رسید تسویه حساب")
                                },
                                icon = Icons.Rounded.Share,
                                modifier = Modifier.weight(1f),
                            )
                            CompactButton(
                                text = "بازگشت به پذیرش",
                                onClick = onFinished,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            state.stay == null -> EmptyState(
                title = "اقامت فعال یافت نشد",
                message = state.errorMessage,
                icon = Icons.Rounded.Hotel,
            )

            else -> {
                val stay = state.stay!!
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
                    MehraCard(modifier = Modifier.fillMaxWidth()) {
                        Text("خلاصه اقامت", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        InfoRow("اتاق", stay.roomNumber)
                        InfoRow("کد رزرو", stay.reservationCode)
                        InfoRow("خروج برنامه‌ریزی‌شده", stay.plannedCheckOut.formatJalaliLongFa())
                        InfoRow("مبلغ کل", formatMoneyFa(stay.totalRial))
                        InfoRow("پرداخت‌شده", formatMoneyFa(stay.paidRial))
                        InfoRow("مانده", formatMoneyFa(stay.balanceRial))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(title = "تسویه حساب")
                    AmountField(
                        label = "مبلغ دریافتی نهایی (ریال)",
                        value = state.finalPayment,
                        onValueChange = viewModel::onFinalPaymentChange,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AmountField(
                        label = "تخفیف (ریال)",
                        value = state.discount,
                        onValueChange = viewModel::onDiscountChange,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DropdownField(
                        label = "روش پرداخت",
                        options = PaymentMethod.entries,
                        selected = state.paymentMethod,
                        onSelect = viewModel::onPaymentMethodChange,
                        optionLabel = { it.labelFa },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SwitchRow(
                        title = "انتقال اتاق به وضعیت نظافت",
                        description = "پس از خروج، درخواست نظافت برای اتاق ثبت می‌شود.",
                        checked = state.moveToCleaning,
                        onCheckedChange = viewModel::onMoveToCleaningChange,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MehraTextField(
                        label = "یادداشت",
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        singleLine = false,
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        text = "ثبت خروج و تسویه",
                        onClick = viewModel::submit,
                        modifier = Modifier.fillMaxWidth(),
                        loading = state.isSubmitting,
                    )
                    if (state.pendingConfirmation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "این عملیات تا تأیید سرور قطعی نیست.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MehraTheme.colors.warning,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    receiptToShow?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToShow = null },
            title = { Text("رسید ${receipt.receiptNumber}") },
            text = { Text("مبلغ کل: ${formatMoneyFa(receipt.totalRial)}") },
            confirmButton = {
                TextButton(onClick = { receiptToShow = null }) { Text("بستن") }
            },
        )
    }
}
