package ir.ahvaz.mehra.feature.guests

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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliDateTimeFa
import ir.ahvaz.mehra.core.date.formatJalaliFaOrDash
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
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
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.Guest

@Composable
fun GuestsScreen(
    onOpenGuest: (String) -> Unit,
    onAddGuest: () -> Unit,
    viewModel: GuestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val guests = state.guests

    MehraScreen(
        title = "مدیریت مهمانان",
        subtitle = "پرونده مهمانان و سوابق اقامت",
        offline = guests.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            if (state.canManage) {
                FloatingActionButton(onClick = onAddGuest) {
                    Icon(Icons.Rounded.Add, contentDescription = "مهمان جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                MehraSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "نام، کد ملی یا شماره تماس…",
                )
                FreshnessRow(
                    updatedAtMillis = guests.updatedAtMillis,
                    isRefreshing = guests.isRefreshing,
                    onRefresh = viewModel::refresh,
                )
            }
            when {
                guests.isLoading && guests.data == null -> LoadingState()
                guests.error != null && guests.data == null -> ErrorState(guests.error!!, viewModel::refresh)
                guests.data.isNullOrEmpty() -> EmptyState(
                    title = "مهمانی یافت نشد",
                    message = "با ثبت اولین مهمان، پرونده او در این فهرست نمایش داده می‌شود.",
                    icon = Icons.Rounded.Person,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(guests.data.orEmpty(), key = { it.id }) { guest ->
                        GuestRow(
                            guest = guest,
                            showNationalId = state.canViewPii,
                            onClick = { onOpenGuest(guest.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestRow(guest: Guest, showNationalId: Boolean, onClick: () -> Unit) {
    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guest.fullName.ifBlank { "بدون نام" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOfNotNull(
                        guest.phone,
                        guest.city,
                        if (showNationalId && !guest.nationalId.isNullOrBlank()) "کد ملی: ${(guest.nationalId).toPersianDigits()}" else null,
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "اقامت‌ها: ${(guest.totalStays.toString()).toPersianDigits()}" +
                        (guest.lastStayAt?.let { " • آخرین: ${it.formatJalaliDateTimeFa()}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (guest.balanceRial != 0L) {
                StatusPill(
                    text = formatMoneyFa(guest.balanceRial),
                    tint = if (guest.balanceRial > 0) MehraTheme.colors.danger else MehraTheme.colors.success,
                )
            }
        }
    }
}

@Composable
fun GuestDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCreateReservation: (String) -> Unit,
    viewModel: GuestDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detailState = state.detail
    val detail = detailState.data

    MehraScreen(
        title = detail?.guest?.fullName ?: "پرونده مهمان",
        subtitle = detail?.guest?.phone,
        onBack = onBack,
        actions = {},
    ) { padding ->
        when {
            detailState.isLoading && detail == null -> LoadingState()
            detailState.error != null && detail == null -> ErrorState(detailState.error!!, viewModel::refresh)
            detail == null -> EmptyState(title = "پرونده یافت نشد")
            else -> {
                val guest = detail.guest
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    MehraCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مشخصات",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = viewModel::refresh) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
                            }
                        }
                        InfoRow("نام", guest.firstName.ifBlank { "—" })
                        InfoRow("نام خانوادگی", guest.lastName.ifBlank { "—" })
                        InfoRow(
                            label = "کد ملی",
                            value = if (!state.canViewPii) "دسترسی محدود" else if (state.revealNationalId) {
                                guest.nationalId.orEmpty().ifBlank { "—" }
                            } else {
                                maskNationalId(guest.nationalId)
                            },
                        )
                        InfoRow("تلفن", guest.phone ?: "—")
                        InfoRow("شهر", guest.city ?: "—")
                        InfoRow("نشانی", guest.address ?: "—")
                        InfoRow("مانده حساب", formatMoneyFa(guest.balanceRial))
                        InfoRow("تعداد اقامت", (guest.totalStays.toString()).toPersianDigits())
                        if (state.canViewPii) {
                            Spacer(modifier = Modifier.height(6.dp))
                            CompactButton(
                                text = if (state.revealNationalId) "پنهان کردن کد ملی" else "نمایش کد ملی",
                                onClick = viewModel::toggleNationalIdVisibility,
                                icon = Icons.Rounded.Visibility,
                            )
                        }
                    }

                    if (guest.companions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "همراهان")
                        guest.companions.forEach { companion ->
                            MehraCard(modifier = Modifier.fillMaxWidth()) {
                                Text(companion.fullName, style = MaterialTheme.typography.bodyLarge)
                                if (!companion.relation.isNullOrBlank() || !companion.nationalId.isNullOrBlank()) {
                                    Text(
                                        text = listOfNotNull(companion.relation, companion.nationalId).joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "سوابق اقامت")
                    if (detail.stays.isEmpty()) {
                        Text(
                            text = "سابقه اقامتی ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detail.stays.forEach { stay ->
                            MehraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${stay.checkIn.formatJalaliFaOrDash()} تا ${stay.checkOut.formatJalaliFaOrDash()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = formatMoneyFa(stay.totalRial),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusPill(text = stay.status.labelFa, tint = stay.status.tint())
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "رزروها")
                    if (detail.reservations.isEmpty()) {
                        Text(
                            text = "رزروی برای این مهمان ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detail.reservations.forEach { reservation ->
                            MehraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reservation.code, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${reservation.checkIn.formatJalaliFaOrDash()} تا ${reservation.checkOut.formatJalaliFaOrDash()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusPill(text = reservation.status.labelFa, tint = reservation.status.tint())
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "گردش مالی")
                    if (detail.ledger.isEmpty()) {
                        Text(
                            text = "تراکنشی ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        detail.ledger.forEach { transaction ->
                            MehraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(transaction.description, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${transaction.type.labelFa} • ${transaction.method.labelFa} • ${transaction.createdAt.formatJalaliDateTimeFa()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = formatMoneyFa(transaction.amountRial),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = transaction.type.tint(),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            text = "رزرو جدید",
                            onClick = { onCreateReservation(guest.id) },
                            modifier = Modifier.weight(1f),
                        )
                        CompactButton(
                            text = "ویرایش پرونده",
                            onClick = { onEdit(guest.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun maskNationalId(nationalId: String?): String {
    if (nationalId.isNullOrBlank()) return "—"
    val visible = nationalId.takeLast(2)
    return "******${(visible).toPersianDigits()}"
}

@Composable
fun GuestFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: GuestFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft

    if (state.saved) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onSaved() }
    }

    MehraScreen(
        title = if (draft.isEdit) "ویرایش مهمان" else "مهمان جدید",
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MehraTextField(
                    label = "نام",
                    value = draft.firstName,
                    onValueChange = { value -> viewModel.updateDraft { it.copy(firstName = value) } },
                    modifier = Modifier.weight(1f),
                    error = state.fieldErrors["firstName"],
                )
                MehraTextField(
                    label = "نام خانوادگی",
                    value = draft.lastName,
                    onValueChange = { value -> viewModel.updateDraft { it.copy(lastName = value) } },
                    modifier = Modifier.weight(1f),
                    error = state.fieldErrors["lastName"],
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "کد ملی",
                value = draft.nationalId,
                onValueChange = { value -> viewModel.updateDraft { it.copy(nationalId = value) } },
                numeric = true,
                error = state.fieldErrors["nationalId"],
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "شماره تماس",
                value = draft.phone,
                onValueChange = { value -> viewModel.updateDraft { it.copy(phone = value) } },
                numeric = true,
                error = state.fieldErrors["phone"],
                placeholder = "۰۹۱۲۳۴۵۶۷۸۹",
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "شهر",
                value = draft.city,
                onValueChange = { value -> viewModel.updateDraft { it.copy(city = value) } },
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "نشانی",
                value = draft.address,
                onValueChange = { value -> viewModel.updateDraft { it.copy(address = value) } },
                singleLine = false,
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "یادداشت",
                value = draft.notes,
                onValueChange = { value -> viewModel.updateDraft { it.copy(notes = value) } },
                singleLine = false,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "همراهان",
                actionLabel = "افزودن",
                onAction = viewModel::addCompanion,
            )
            draft.companions.forEachIndexed { index, companion ->
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "همراه ${((index + 1).toString()).toPersianDigits()}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.removeCompanion(index) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "حذف همراه")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MehraTextField(
                            label = "کد ملی",
                            value = companion.nationalId.orEmpty(),
                            onValueChange = { value ->
                                viewModel.updateCompanion(index) { it.copy(nationalId = value) }
                            },
                            modifier = Modifier.weight(1f),
                            numeric = true,
                        )
                        MehraTextField(
                            label = "نسبت",
                            value = companion.relation.orEmpty(),
                            onValueChange = { value ->
                                viewModel.updateCompanion(index) { it.copy(relation = value) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = if (draft.isEdit) "ذخیره تغییرات" else "ثبت مهمان",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
