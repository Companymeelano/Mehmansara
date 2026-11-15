package ir.ahvaz.mehra.feature.guests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.IRAN_PROVINCES
import ir.ahvaz.mehra.core.common.PROVINCE_NAMES
import ir.ahvaz.mehra.core.common.VEHICLE_COLORS
import ir.ahvaz.mehra.core.common.citiesOf
import ir.ahvaz.mehra.core.common.VEHICLE_TYPES
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliDateTimeFa
import ir.ahvaz.mehra.core.date.formatJalaliFaOrDash
import ir.ahvaz.mehra.core.date.relativeTimeFa
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.components.InfoRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.AmountField
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.IranPlateInput
import ir.ahvaz.mehra.core.ui.components.MehraSearchField
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.PrimaryButton
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.Guest
import ir.ahvaz.mehra.domain.model.LoyaltyTier

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
                        guest.carPlate?.let { "پلاک: $it" },
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
                    GuestHeroCard(guest = guest)
                    Spacer(modifier = Modifier.height(12.dp))
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
                        InfoRow("پلاک خودرو", guest.carPlate ?: "—")
                        guest.vehicleType?.let { InfoRow("نوع خودرو", it) }
                        guest.vehicleColor?.let { InfoRow("رنگ خودرو", it) }
                        guest.province?.let { InfoRow("استان", it) }
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

/**
 * Gamified guest header (v1.5.0): gradient hero with the 3D avatar chip, a golden VIP star
 * for loyal guests (۵+ اقامت), a colour-coded balance and a stat strip — the profile feels
 * like a loyalty card, not a form.
 */
@Composable
private fun GuestHeroCard(guest: Guest) {
    val colors = MehraTheme.colors
    val tier = LoyaltyTier.tierOf(guest.totalStays)
    val nextTier = LoyaltyTier.nextTier(guest.totalStays)
    val isVip = tier.minStays >= LoyaltyTier.GOLD.minStays
    val balanceColor = when {
        guest.balanceRial > 0 -> colors.danger
        guest.balanceRial < 0 -> colors.success
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(colors.topBarStart, colors.topBarEnd)),
                RoundedCornerShape(24.dp),
            )
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon3D(icon = Icons.Rounded.Person, contentDescription = null, size = 56.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = guest.fullName.ifBlank { "بدون نام" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                        )
                        if (isVip) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "سطح ${tier.labelFa}",
                                tint = colors.gold,
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tier.labelFa,
                            color = colors.gold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = guest.phone?.let { "تلفن: ${it.toPersianDigits()}" } ?: "بدون شماره تماس",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = formatMoneyFa(guest.balanceRial),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(balanceColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroChip(label = "اقامت‌ها", value = guest.totalStays.toString().toPersianDigits(), modifier = Modifier.weight(1f))
                HeroChip(
                    label = "آخرین اقامت",
                    value = relativeTimeFa(guest.lastStayAt).ifBlank { "—" },
                    modifier = Modifier.weight(1.4f),
                )
                HeroChip(label = "همراهان", value = guest.companions.size.toString().toPersianDigits(), modifier = Modifier.weight(1f))
            }
            if (nextTier != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = (guest.totalStays.toFloat() / nextTier.minStays).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = colors.gold,
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = (nextTier.minStays - guest.totalStays).toString().toPersianDigits() +
                        " اقامت تا سطح ${nextTier.labelFa}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun HeroChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
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
            MehraCard(modifier = Modifier.fillMaxWidth()) {
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
            Text(
                text = "پلاک خودرو (اختیاری)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            IranPlateInput(
                value = draft.carPlate,
                onValueChange = { value -> viewModel.updateDraft { it.copy(carPlate = value) } },
            )
            if (state.fieldErrors["carPlate"] != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.fieldErrors["carPlate"]!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "برای شناسایی خودروی مهمان؛ با پلاک هم می‌توان مهمان را جست‌وجو کرد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            MehraSearchField(
                value = state.vehicleSearch,
                onValueChange = viewModel::onVehicleSearchChange,
                placeholder = "جست‌وجوی نوع خودرو…",
            )
            Spacer(modifier = Modifier.height(8.dp))
            DropdownField(
                label = "نوع خودرو",
                options = VEHICLE_TYPES.filter { state.vehicleSearch.isBlank() || it.contains(state.vehicleSearch) },
                selected = draft.vehicleType.ifBlank { null },
                onSelect = { viewModel.updateDraft { d -> d.copy(vehicleType = it) } },
                optionLabel = { it },
            )
            Spacer(modifier = Modifier.height(10.dp))
            DropdownField(
                label = "رنگ خودرو",
                options = VEHICLE_COLORS,
                selected = draft.vehicleColor.ifBlank { null },
                onSelect = { viewModel.updateDraft { d -> d.copy(vehicleColor = it) } },
                optionLabel = { it },
            )
            Spacer(modifier = Modifier.height(10.dp))
            DropdownField(
                label = "استان",
                options = PROVINCE_NAMES,
                selected = draft.province.ifBlank { null },
                onSelect = { province -> viewModel.updateDraft { it.copy(province = province, city = "") } },
                optionLabel = { it },
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraSearchField(
                value = state.citySearch,
                onValueChange = viewModel::onCitySearchChange,
                placeholder = "جست‌وجوی شهر…",
            )
            Spacer(modifier = Modifier.height(8.dp))
            DropdownField(
                label = "شهر",
                options = run {
                    val base =
                        if (draft.province.isBlank()) IRAN_PROVINCES.values.flatten().distinct()
                        else citiesOf(draft.province)
                    base.filter { state.citySearch.isBlank() || it.contains(state.citySearch) }
                },
                selected = draft.city.ifBlank { null },
                onSelect = { city -> viewModel.updateDraft { it.copy(city = city) } },
                optionLabel = { it },
                placeholder = "انتخاب یا جست‌وجوی شهر",
            )
            Spacer(modifier = Modifier.height(10.dp))
            MehraTextField(
                label = "نشانی",
                value = draft.address,
                onValueChange = { value -> viewModel.updateDraft { it.copy(address = value) } },
                singleLine = false,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "اقامت (اختیاری)")
            DropdownField(
                label = "اتاق / سوئیت",
                options = state.rooms,
                selected = state.rooms.firstOrNull { it.id == draft.roomId },
                onSelect = { room -> viewModel.updateDraft { it.copy(roomId = room.id) } },
                optionLabel = { "اتاق ${it.roomNumber.toPersianDigits()} — ${it.roomTypeName}" },
                placeholder = "بدون تخصیص اتاق",
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MehraTextField(
                    label = "تعداد روز اقامت",
                    value = draft.stayNights,
                    onValueChange = { value -> viewModel.updateDraft { it.copy(stayNights = value) } },
                    modifier = Modifier.weight(1f),
                    numeric = true,
                    placeholder = "۱",
                )
                AmountField(
                    label = "تخفیف (ریال)",
                    value = draft.discountRial,
                    onValueChange = { value -> viewModel.updateDraft { it.copy(discountRial = value) } },
                    modifier = Modifier.weight(1f),
                )
            }
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
