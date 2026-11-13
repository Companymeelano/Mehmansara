package ir.ahvaz.mehra.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyCompactFa
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliLongFa
import ir.ahvaz.mehra.core.date.formatTimeFa
import ir.ahvaz.mehra.core.date.relativeTimeFa
import ir.ahvaz.mehra.core.date.TehranTime
import ir.ahvaz.mehra.core.ui.components.AnimatedInt
import ir.ahvaz.mehra.core.ui.components.BarSeriesChart
import ir.ahvaz.mehra.core.ui.components.ChartLegend
import ir.ahvaz.mehra.core.ui.components.DonutChart
import ir.ahvaz.mehra.core.ui.components.DonutSegment
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.GradientText
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.clockFa
import ir.ahvaz.mehra.core.ui.components.rememberTehranClock
import ir.ahvaz.mehra.core.ui.components.timeOfDayGreetingFa
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatTile
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.icons.MehraIcons
import ir.ahvaz.mehra.core.ui.theme.LocalAccentBrush
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.DashboardSnapshot

@Composable
fun DashboardScreen(
    onOpenRooms: () -> Unit,
    onOpenReservations: () -> Unit,
    onOpenReception: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenHousekeeping: () -> Unit,
    onOpenGuests: () -> Unit,
    onOpenMore: () -> Unit,
    onCreateRoom: () -> Unit,
    onCreateGuest: () -> Unit,
    onNewReservation: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dashboard = state.dashboard
    val snapshot = dashboard.data

    MehraScreen(
        title = "خانه",
        subtitle = Constants.HOTEL_NAME + " • " + TehranTime.today().formatJalaliLongFa(),
        offline = dashboard.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            IconButton(onClick = onOpenNotifications) {
                if (snapshot != null && snapshot.alerts.isNotEmpty()) {
                    BadgedBox(badge = {
                        Badge { Text(text = snapshot.alerts.size.toString().toPersianDigits()) }
                    }) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "اعلان‌ها")
                    }
                } else {
                    Icon(Icons.Rounded.Notifications, contentDescription = "اعلان‌ها")
                }
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
    ) { padding ->
        when {
            dashboard.isLoading && snapshot == null -> LoadingState()
            dashboard.error != null && snapshot == null -> ErrorState(
                error = dashboard.error!!,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )

            snapshot == null || !snapshot.hasRooms -> WelcomeContent(
                onCreateRoom = onCreateRoom,
                onCreateGuest = onCreateGuest,
                onNewReservation = onNewReservation,
                onOpenReception = onOpenReception,
                onOpenRooms = onOpenRooms,
                onOpenGuests = onOpenGuests,
                onOpenReservations = onOpenReservations,
                onOpenMore = onOpenMore,
                contentPadding = padding,
            )

            else -> DashboardContent(
                snapshot = snapshot,
                updatedAtMillis = dashboard.updatedAtMillis,
                isRefreshing = dashboard.isRefreshing,
                currency = state.currencyUnit,
                onRefresh = viewModel::refresh,
                onOpenRooms = onOpenRooms,
                onOpenReservations = onOpenReservations,
                onOpenReception = onOpenReception,
                onOpenFinance = onOpenFinance,
                onOpenMaintenance = onOpenMaintenance,
                onOpenHousekeeping = onOpenHousekeeping,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    snapshot: DashboardSnapshot,
    updatedAtMillis: Long?,
    isRefreshing: Boolean,
    currency: ir.ahvaz.mehra.core.common.CurrencyUnit,
    onRefresh: () -> Unit,
    onOpenRooms: () -> Unit,
    onOpenReservations: () -> Unit,
    onOpenReception: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenHousekeeping: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    val colors = MehraTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardHero(snapshot = snapshot)
        }

        item {
            FreshnessRow(
                updatedAtMillis = updatedAtMillis,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            )
        }

        item {
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        segments = listOf(
                            DonutSegment(snapshot.occupiedRooms.toFloat(), colors.statusOccupied, "اشغال"),
                            DonutSegment(snapshot.reservedRooms.toFloat(), colors.statusReserved, "رزرو"),
                            DonutSegment(snapshot.freeRooms.toFloat(), colors.statusFree, "آزاد"),
                            DonutSegment(snapshot.cleaningRooms.toFloat(), colors.statusCleaning, "نظافت"),
                            DonutSegment(snapshot.maintenanceRooms.toFloat(), colors.statusMaintenance, "تعمیرات"),
                        ),
                        modifier = Modifier.size(140.dp),
                        centerValue = AnimatedInt(snapshot.occupancyPercent).toString().toPersianDigits() + "٪",
                        centerLabel = "اشغال",
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    ChartLegend(
                        segments = listOf(
                            DonutSegment(snapshot.occupiedRooms.toFloat(), colors.statusOccupied, "اشغال"),
                            DonutSegment(snapshot.reservedRooms.toFloat(), colors.statusReserved, "رزرو‌شده"),
                            DonutSegment(snapshot.freeRooms.toFloat(), colors.statusFree, "آزاد"),
                            DonutSegment(snapshot.cleaningRooms.toFloat(), colors.statusCleaning, "نظافت"),
                            DonutSegment(snapshot.maintenanceRooms.toFloat(), colors.statusMaintenance, "تعمیرات"),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "کل اتاق‌ها",
                    value = (snapshot.totalRooms.toString()).toPersianDigits(),
                    icon = Icons.Rounded.Bed,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenRooms,
                )
                StatTile(
                    label = "آزاد",
                    value = (snapshot.freeRooms.toString()).toPersianDigits(),
                    icon = Icons.Rounded.EventAvailable,
                    tone = colors.statusFree,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenRooms,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "رزرو‌شده",
                    value = (snapshot.reservedRooms.toString()).toPersianDigits(),
                    icon = Icons.Rounded.EventBusy,
                    tone = colors.statusReserved,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReservations,
                )
                StatTile(
                    label = "اشغال",
                    value = (snapshot.occupiedRooms.toString()).toPersianDigits(),
                    icon = Icons.Rounded.Bed,
                    tone = colors.statusOccupied,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReception,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "در حال نظافت",
                    value = (snapshot.cleaningRooms.toString()).toPersianDigits(),
                    icon = Icons.Rounded.CleaningServices,
                    tone = colors.statusCleaning,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHousekeeping,
                )
                StatTile(
                    label = "تعمیراتی",
                    value = (snapshot.maintenanceRooms.toString()).toPersianDigits(),
                    icon = Icons.Rounded.Handyman,
                    tone = colors.statusMaintenance,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMaintenance,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "ورود امروز",
                    value = (snapshot.todayCheckIns.toString()).toPersianDigits(),
                    icon = Icons.Rounded.Login,
                    tone = colors.info,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReception,
                )
                StatTile(
                    label = "خروج امروز",
                    value = (snapshot.todayCheckOuts.toString()).toPersianDigits(),
                    icon = Icons.Rounded.Logout,
                    tone = colors.warning,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReception,
                )
            }
        }
        item {
            StatTile(
                label = "درآمد امروز",
                value = formatMoneyCompactFa(snapshot.revenueTodayRial, currency),
                caption = formatMoneyFa(snapshot.revenueTodayRial, currency),
                icon = Icons.Rounded.Payments,
                tone = colors.gold,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenFinance,
            )
        }

        if (snapshot.occupancySeries.isNotEmpty()) {
            item { SectionHeader(title = "نمودار اشغال اتاق‌ها (۷ روز اخیر)") }
            item {
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    BarSeriesChart(
                        values = snapshot.occupancySeries.map { it.percent.toFloat() },
                        labels = snapshot.occupancySeries.map { it.date.formatJalaliLongFa().split(" ").firstOrNull() ?: "" },
                        barColor = colors.chartPrimary,
                        showValueOnTop = true,
                    )
                }
            }
        }

        if (snapshot.alerts.isNotEmpty()) {
            item { SectionHeader(title = "اعلان‌های مهم") }
            items(snapshot.alerts, key = { it.id }) { alert ->
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(text = alert.severity.labelFa, tint = alert.severity.tint())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!alert.message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionHeader(title = "فعالیت‌های اخیر") }
        if (snapshot.recentActivities.isEmpty()) {
            item {
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "فعالیتی ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(snapshot.recentActivities, key = { it.id }) { activity ->
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                            if (!activity.description.isNullOrBlank()) {
                                Text(
                                    text = activity.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = listOfNotNull(
                                    activity.actorName,
                                    relativeTimeFa(activity.occurredAt),
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusPill(text = activity.kind.labelFa, tint = colors.info)
                            Text(
                                text = activity.occurredAt.formatTimeFa(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "برای مشاهده رزروهای امروز به بخش رزروها مراجعه کنید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


/**
 * Rich first-run dashboard (1.4.1): even with zero data the home screen feels alive —
 * a gradient hero with the 3D brand chip, quick actions for the daily work, a live
 * overview row and a shortcut to the full module grid.
 */
@Composable
private fun WelcomeContent(
    onCreateRoom: () -> Unit,
    onCreateGuest: () -> Unit,
    onNewReservation: () -> Unit,
    onOpenReception: () -> Unit,
    onOpenRooms: () -> Unit,
    onOpenGuests: () -> Unit,
    onOpenReservations: () -> Unit,
    onOpenMore: () -> Unit,
    contentPadding: PaddingValues,
) {
    val colors = MehraTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(colors.topBarStart, colors.topBarEnd)),
                        RoundedCornerShape(24.dp),
                    )
                    .padding(20.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon3D(icon = MehraIcons.Logo, contentDescription = null, size = 64.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    GradientText(
                        text = "مهرآ",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        brush = LocalAccentBrush.current,
                    )
                    Text(
                        text = Constants.HOTEL_NAME,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = TehranTime.today().formatJalaliLongFa(),
                        color = Color.White.copy(alpha = 0.66f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "به سامانهٔ مدیریت مهمان‌پذیر خوش آمدید؛ همهٔ کارها را از اینجا شروع کنید.",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { SectionHeader(title = "اقدام سریع") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile("اتاق جدید", "ثبت اتاق در مجموعه", Icons.Rounded.Bed, Modifier.weight(1f), onCreateRoom)
                    QuickActionTile("مهمان جدید", "پروندهٔ مهمان", Icons.Rounded.PersonAdd, Modifier.weight(1f), onCreateGuest)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile("رزرو جدید", "ثبت رزرو", Icons.Rounded.EventAvailable, Modifier.weight(1f), onNewReservation)
                    QuickActionTile("پذیرش", "ورود و خروج", Icons.Rounded.Login, Modifier.weight(1f), onOpenReception)
                }
            }
        }

        item { SectionHeader(title = "نمای کلی امروز") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(label = "اتاق‌ها", value = "0".toPersianDigits(), icon = Icons.Rounded.Bed, modifier = Modifier.weight(1f), onClick = onOpenRooms)
                StatTile(label = "مهمانان", value = "0".toPersianDigits(), icon = Icons.Rounded.PersonAdd, modifier = Modifier.weight(1f), onClick = onOpenGuests)
            }
        }
        item {
            StatTile(
                label = "رزروهای امروز",
                value = "0".toPersianDigits(),
                icon = Icons.Rounded.EventAvailable,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenReservations,
            )
        }

        item {
            MehraCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenMore) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon3D(icon = Icons.Rounded.MoreHoriz, contentDescription = null, size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "همهٔ بخش‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "مالی، نظافت، تعمیرات، گزارش‌ها، کاربران، اعلانات و تنظیمات",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One quick-action tile of the welcome dashboard: 3D icon chip + two-line label. */
@Composable
private fun QuickActionTile(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MehraCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon3D(icon = icon, contentDescription = null, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Live hero card (v1.5.0): big Jalali date, a Tehran clock that ticks every second, a
 * time-of-day greeting and count-up stats — the dashboard feels alive at a glance.
 */
@Composable
private fun DashboardHero(snapshot: DashboardSnapshot) {
    val colors = MehraTheme.colors
    val now = rememberTehranClock()
    val occupancy = AnimatedInt(snapshot.occupancyPercent)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeOfDayGreetingFa(now.hour),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = TehranTime.today().formatJalaliLongFa(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = now.clockFa(),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(
                    label = "اشغال امروز",
                    value = occupancy.toString().toPersianDigits() + "٪",
                    modifier = Modifier.weight(1f),
                )
                HeroStat(
                    label = "ورود امروز",
                    value = snapshot.todayCheckIns.toString().toPersianDigits(),
                    modifier = Modifier.weight(1f),
                )
                HeroStat(
                    label = "خروج امروز",
                    value = snapshot.todayCheckOuts.toString().toPersianDigits(),
                    modifier = Modifier.weight(1f),
                )
            }
            if (snapshot.alerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = snapshot.alerts.size.toString().toPersianDigits() + " اعلان مهم — از بخش اعلان‌ها ببینید",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
