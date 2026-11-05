package ir.ahvaz.mehra.feature.reservations

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventNote
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliFaOrDash
import ir.ahvaz.mehra.core.date.formatJalaliLongFa
import ir.ahvaz.mehra.core.date.nightsLabelFa
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FilterChipRow
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InfoRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.JalaliDateField
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
import ir.ahvaz.mehra.domain.model.AvailableRoom
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Reservation
import ir.ahvaz.mehra.domain.model.ReservationStatus

@Composable
fun ReservationsScreen(
    onOpenReservation: (String) -> Unit,
    onNewReservation: () -> Unit,
    viewModel: ReservationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reservations = state.reservations
    var pendingCancel by remember { mutableStateOf<Reservation?>(null) }

    MehraScreen(
        title = "رزروها",
        subtitle = "مدیریت رزرو و لیست انتظار",
        offline = reservations.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            if (state.canManage) {
                FloatingActionButton(onClick = onNewReservation) {
                    Icon(Icons.Rounded.Add, contentDescription = "رزرو جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                MehraSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "نام مهمان یا کد رزرو…",
                )
                Spacer(modifier = Modifier.height(6.dp))
                FilterChipRow(
                    items = ReservationStatus.entries,
                    selected = state.status,
                    onSelect = viewModel::onStatusChange,
                    label = { it.labelFa },
                )
                FreshnessRow(
                    updatedAtMillis = reservations.updatedAtMillis,
                    isRefreshing = reservations.isRefreshing,
                    onRefresh = viewModel::refresh,
                )
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                }
            }

            when {
                reservations.isLoading && reservations.data == null -> LoadingState()
                reservations.error != null && reservations.data == null ->
                    ErrorState(reservations.error!!, viewModel::refresh)

                reservations.data.isNullOrEmpty() && state.waitlist.isEmpty() -> EmptyState(
                    title = "رزروی ثبت نشده است",
                    message = "با دکمه + رزرو جدید، اولین رزرو را ثبت کنید.",
                    icon = Icons.Rounded.EventNote,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(reservations.data.orEmpty(), key = { it.id }) { reservation ->
                        ReservationRow(
                            reservation = reservation,
                            canManage = state.canManage,
                            onClick = { onOpenReservation(reservation.id) },
                            onConfirm = { viewModel.confirm(reservation) },
                            onCancel = { pendingCancel = reservation },
                        )
                    }
                    if (state.waitlist.isNotEmpty()) {
                        item { SectionHeader(title = "لیست انتظار") }
                        items(state.waitlist, key = { it.id }) { entry ->
                            MehraCard(modifier = Modifier.fillMaxWidth()) {
                                Text(entry.guestName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = listOfNotNull(
                                        entry.guestPhone,
                                        entry.requestedCheckIn.formatJalaliFaOrDash() + " تا " +
                                            entry.requestedCheckOut.formatJalaliFaOrDash(),
                                        "${(entry.guestsCount.toString()).toPersianDigits()} نفر",
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingCancel != null) {
        CancelReservationDialog(
            code = pendingCancel!!.code,
            onDismiss = { pendingCancel = null },
            onConfirm = { reason ->
                viewModel.cancel(pendingCancel!!, reason)
                pendingCancel = null
            },
        )
    }
}

@Composable
private fun ReservationRow(
    reservation: Reservation,
    canManage: Boolean,
    onClick: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reservation.guestName.ifBlank { "مهمان" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(text = reservation.status.labelFa, tint = reservation.status.tint())
                }
                Text(
                    text = "کد ${reservation.code} • اتاق ${reservation.roomNumber ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${reservation.checkIn.formatJalaliFaOrDash()} تا ${reservation.checkOut.formatJalaliFaOrDash()} • ${nightsLabelFa(reservation.nights)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "مبلغ: ${formatMoneyFa(reservation.payableRial)} • مانده: ${formatMoneyFa(reservation.remainingRial)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MehraTheme.colors.gold,
                )
            }
        }
        if (canManage && reservation.status.isOpen) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reservation.status == ReservationStatus.PENDING) {
                    CompactButton(text = "تأیید", onClick = onConfirm, modifier = Modifier.weight(1f))
                }
                CompactButton(text = "لغو رزرو", onClick = onCancel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CancelReservationDialog(code: String, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("لغو رزرو $code") },
        text = {
            Column {
                Text(
                    text = "دلیل لغو برای پیگیری در سوابق ثبت می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MehraTextField(
                    label = "دلیل لغو",
                    value = reason,
                    onValueChange = { reason = it },
                    singleLine = false,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason.takeIf { it.isNotBlank() }) }) { Text("لغو رزرو") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun ReservationDetailScreen(
    onBack: () -> Unit,
    onCheckIn: (String) -> Unit,
    viewModel: ReservationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reservationState = state.reservation
    val reservation = reservationState.data

    MehraScreen(
        title = reservation?.code?.let { "رزرو $it" } ?: "جزئیات رزرو",
        subtitle = reservation?.guestName,
        onBack = onBack,
    ) { padding ->
        when {
            reservationState.isLoading && reservation == null -> LoadingState()
            reservationState.error != null && reservation == null ->
                ErrorState(reservationState.error!!, viewModel::refresh)

            reservation == null -> EmptyState(title = "رزرو یافت نشد")
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
                        StatusPill(text = reservation.status.labelFa, tint = reservation.status.tint())
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("مهمان", reservation.guestName)
                    InfoRow("تلفن", reservation.guestPhone ?: "—")
                    InfoRow("اتاق", reservation.roomNumber ?: "—")
                    InfoRow("نوع اتاق", reservation.roomTypeName ?: "—")
                    InfoRow("ورود", reservation.checkIn.formatJalaliLongFa())
                    InfoRow("خروج", reservation.checkOut.formatJalaliLongFa())
                    InfoRow("مدت اقامت", nightsLabelFa(reservation.nights))
                    InfoRow("تعداد نفرات", (reservation.guestsCount.toString()).toPersianDigits())
                    InfoRow("تعرفه شبانه", formatMoneyFa(reservation.nightlyRateRial))
                    InfoRow("مبلغ کل", formatMoneyFa(reservation.totalRial))
                    InfoRow("تخفیف", formatMoneyFa(reservation.discountRial))
                    InfoRow("پرداخت‌شده", formatMoneyFa(reservation.paidRial))
                    InfoRow("مانده", formatMoneyFa(reservation.remainingRial))
                    if (!reservation.notes.isNullOrBlank()) InfoRow("یادداشت", reservation.notes)
                }

                if (state.canManage) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (reservation.status == ReservationStatus.PENDING) {
                            PrimaryButton(
                                text = "تأیید رزرو",
                                onClick = viewModel::confirm,
                                modifier = Modifier.weight(1f),
                                enabled = !state.actionInProgress,
                            )
                        }
                        if (reservation.status.isOpen) {
                            SecondaryButton(
                                text = "لغو رزرو",
                                onClick = { viewModel.cancel(null) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.actionInProgress,
                            )
                        }
                    }
                    if (reservation.status == ReservationStatus.CONFIRMED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PrimaryButton(
                            text = "انتقال به پذیرش",
                            onClick = { onCheckIn(reservation.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun NewReservationScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: NewReservationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft

    LaunchedEffect(state.createdReservation) {
        state.createdReservation?.let { onCreated(it.id) }
    }

    MehraScreen(title = "رزرو جدید", onBack = onBack) { padding ->
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

            SectionHeader(title = "مهمان")
            MehraSearchField(
                value = state.guestSearch,
                onValueChange = viewModel::onGuestSearchChange,
                placeholder = "جست‌وجوی مهمان…",
            )
            Spacer(modifier = Modifier.height(8.dp))
            DropdownField(
                label = "انتخاب مهمان",
                options = state.guests,
                selected = state.guests.firstOrNull { it.id == draft.guestId },
                onSelect = viewModel::selectGuest,
                optionLabel = { if (it.fullName.isBlank()) it.phone.orEmpty() else it.fullName },
                error = state.fieldErrors["guestId"],
            )
            if (state.guests.isEmpty()) {
                Text(
                    text = "مهمانی یافت نشد. ابتدا پرونده مهمان را در بخش مهمانان ثبت کنید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "زمان اقامت")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JalaliDateField(
                    label = "تاریخ ورود",
                    value = draft.checkIn,
                    onDateSelected = { date ->
                        viewModel.updateDraft { it.copy(checkIn = date) }
                        viewModel.refreshAvailability()
                    },
                    modifier = Modifier.weight(1f),
                    error = state.fieldErrors["checkIn"],
                )
                JalaliDateField(
                    label = "تاریخ خروج",
                    value = draft.checkOut,
                    onDateSelected = { date ->
                        viewModel.updateDraft { it.copy(checkOut = date) }
                        viewModel.refreshAvailability()
                    },
                    modifier = Modifier.weight(1f),
                    error = state.fieldErrors["checkOut"],
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "تعداد نفرات",
                value = draft.guestsCount,
                onValueChange = { value ->
                    viewModel.updateDraft { it.copy(guestsCount = value) }
                    viewModel.refreshAvailability()
                },
                numeric = true,
                error = state.fieldErrors["guestsCount"],
            )

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(
                title = "اتاق",
                actionLabel = "بازخوانی اتاق‌های آزاد",
                onAction = viewModel::refreshAvailability,
            )
            if (state.isAvailabilityLoading) {
                LoadingState(message = "در حال بررسی اتاق‌های آزاد…")
            } else {
                DropdownField(
                    label = "اتاق آزاد",
                    options = state.availableRooms,
                    selected = state.availableRooms.firstOrNull { it.roomId == draft.roomId },
                    onSelect = { room ->
                        viewModel.updateDraft {
                            it.copy(roomId = room.roomId, roomNumber = room.roomNumber)
                        }
                    },
                    optionLabel = { room: AvailableRoom ->
                        "اتاق ${room.roomNumber} • ${room.roomTypeName} • ${formatMoneyFa(room.ratePerNightRial)}"
                    },
                    error = state.fieldErrors["roomId"],
                )
                if (state.availableRooms.isEmpty()) {
                    Text(
                        text = "برای این بازه اتاق آزادی ثبت نشده است؛ می‌توانید رزرو را در لیست انتظار قرار دهید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MehraTheme.colors.warning,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SwitchRow(
                title = "ثبت در لیست انتظار",
                description = "وقتی اتاق آزاد نیست، رزرو در لیست انتظار ثبت می‌شود.",
                checked = draft.joinWaitlist,
                onCheckedChange = { checked -> viewModel.updateDraft { it.copy(joinWaitlist = checked) } },
            )

            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "مبلغ و پرداخت")
            MehraTextField(
                label = "تخفیف (ریال)",
                value = draft.discountRial,
                onValueChange = { value -> viewModel.updateDraft { it.copy(discountRial = value) } },
                numeric = true,
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "پیش‌پرداخت (ریال)",
                value = draft.prepaidRial,
                onValueChange = { value -> viewModel.updateDraft { it.copy(prepaidRial = value) } },
                numeric = true,
            )
            Spacer(modifier = Modifier.height(10.dp))
            DropdownField(
                label = "روش پرداخت",
                options = PaymentMethod.entries,
                selected = draft.paymentMethod,
                onSelect = { method -> viewModel.updateDraft { it.copy(paymentMethod = method) } },
                optionLabel = { it.labelFa },
            )

            if (state.quote != null) {
                Spacer(modifier = Modifier.height(12.dp))
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Text("برآورد هزینه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    InfoRow("شب‌ها", nightsLabelFa(state.quote!!.nights))
                    InfoRow("جمع اتاق", formatMoneyFa(state.quote!!.roomTotalRial))
                    InfoRow("تخفیف", formatMoneyFa(state.quote!!.discountRial))
                    InfoRow("مبلغ نهایی", formatMoneyFa(state.quote!!.totalRial))
                    InfoRow("مانده پس از پیش‌پرداخت", formatMoneyFa(state.quote!!.remainingRial))
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
                text = if (draft.joinWaitlist) "ثبت در لیست انتظار" else "ثبت رزرو",
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
            if (state.pendingConfirmation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "این عملیات تا برقراری اتصال، «در انتظار تأیید سرور» باقی می‌ماند و در فهرست عملیات در انتظار قابل پیگیری است.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MehraTheme.colors.warning,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
