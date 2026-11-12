package ir.ahvaz.mehra.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.ahvaz.mehra.R
import ir.ahvaz.mehra.core.ui.components.Icon3D
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.feature.dashboard.DashboardScreen
import ir.ahvaz.mehra.feature.finance.FinanceScreen
import ir.ahvaz.mehra.feature.guests.GuestDetailScreen
import ir.ahvaz.mehra.feature.guests.GuestFormScreen
import ir.ahvaz.mehra.feature.guests.GuestsScreen
import ir.ahvaz.mehra.feature.housekeeping.HousekeepingScreen
import ir.ahvaz.mehra.feature.maintenance.MaintenanceDetailScreen
import ir.ahvaz.mehra.feature.maintenance.MaintenanceFormScreen
import ir.ahvaz.mehra.feature.maintenance.MaintenanceScreen
import ir.ahvaz.mehra.feature.more.MoreScreen
import ir.ahvaz.mehra.feature.notifications.NotificationsScreen
import ir.ahvaz.mehra.feature.reception.CheckInScreen
import ir.ahvaz.mehra.feature.reception.CheckOutScreen
import ir.ahvaz.mehra.feature.reception.ReceptionScreen
import ir.ahvaz.mehra.feature.reports.ReportsScreen
import ir.ahvaz.mehra.feature.reservations.NewReservationScreen
import ir.ahvaz.mehra.feature.reservations.ReservationDetailScreen
import ir.ahvaz.mehra.feature.reservations.ReservationsScreen
import ir.ahvaz.mehra.feature.rooms.RoomDetailScreen
import ir.ahvaz.mehra.feature.rooms.RoomFormScreen
import ir.ahvaz.mehra.feature.rooms.RoomsScreen
import ir.ahvaz.mehra.feature.settings.SettingsScreen
import ir.ahvaz.mehra.feature.users.UsersScreen
import ir.ahvaz.mehra.domain.model.SessionUser
import ir.ahvaz.mehra.domain.model.Permissions

private data class BottomDestination(
    val route: String,
    val label: String,
    @androidx.annotation.DrawableRes val iconRes: Int,
    val permission: String?,
)

// The bottom bar carries ONLY the high-usage daily sections (1.4.1): home, rooms, guests,
// reservations, reception. Everything else lives in the «بخش‌ها» grid reachable from home.
private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "خانه", R.drawable.ic_mehra_dashboard, Permissions.DASHBOARD_VIEW),
    BottomDestination(Routes.ROOMS, "اتاق‌ها", R.drawable.ic_mehra_rooms, Permissions.ROOMS_VIEW),
    BottomDestination(Routes.GUESTS, "مهمانان", R.drawable.ic_mehra_guests, Permissions.GUESTS_VIEW),
    BottomDestination(Routes.RESERVATIONS, "رزروها", R.drawable.ic_mehra_reservations, Permissions.RESERVATIONS_VIEW),
    BottomDestination(Routes.RECEPTION, "پذیرش", R.drawable.ic_mehra_reception, Permissions.RECEPTION_OPERATE),
)

@Composable
fun MehraNavGraph(
    user: SessionUser?,
    onSessionInvalid: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TopLevelRoutes

    val can = { permission: String? -> permission == null || user?.can(permission) == true }
    val destinations = bottomDestinations.filter { can(it.permission) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                MehraBottomBar(
                    destinations = destinations,
                    backStackEntry = backStackEntry,
                    onSelect = { destination ->
                        if (currentRoute != destination.route) {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (can(Permissions.DASHBOARD_VIEW)) Routes.DASHBOARD else Routes.RECEPTION,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenRooms = { navController.navigate(Routes.ROOMS) },
                    onOpenReservations = { navController.navigate(Routes.RESERVATIONS) },
                    onOpenReception = { navController.navigate(Routes.RECEPTION) },
                    onOpenFinance = { navController.navigate(Routes.FINANCE) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenMaintenance = { navController.navigate(Routes.MAINTENANCE) },
                    onOpenHousekeeping = { navController.navigate(Routes.HOUSEKEEPING) },
                    onOpenGuests = { navController.navigate(Routes.GUESTS) },
                    onOpenMore = { navController.navigate(Routes.MORE) },
                    onCreateRoom = { navController.navigate(Routes.roomForm(null)) },
                    onCreateGuest = { navController.navigate(Routes.guestForm(null)) },
                    onNewReservation = { navController.navigate(Routes.reservationNew()) },
                )
            }

            composable(Routes.ROOMS) {
                RoomsScreen(
                    onOpenRoom = { navController.navigate(Routes.roomDetail(it)) },
                    onCreateRoom = { navController.navigate(Routes.roomForm(null)) },
                )
            }
            composable(
                route = Routes.ROOM_DETAIL,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
            ) {
                RoomDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.roomForm(it)) },
                )
            }
            composable(
                route = Routes.ROOM_FORM,
                arguments = listOf(
                    navArgument("roomId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                RoomFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.RESERVATIONS) {
                ReservationsScreen(
                    onOpenReservation = { navController.navigate(Routes.reservationDetail(it)) },
                    onNewReservation = { navController.navigate(Routes.reservationNew()) },
                )
            }
            composable(
                route = Routes.RESERVATION_NEW,
                arguments = listOf(
                    navArgument("guestId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                NewReservationScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.reservationDetail(id))
                    },
                )
            }
            composable(
                route = Routes.RESERVATION_DETAIL,
                arguments = listOf(navArgument("reservationId") { type = NavType.StringType }),
            ) {
                ReservationDetailScreen(
                    onBack = { navController.popBackStack() },
                    onCheckIn = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.checkIn(id))
                    },
                )
            }

            composable(Routes.RECEPTION) {
                ReceptionScreen(
                    onCheckIn = { navController.navigate(Routes.checkIn(it)) },
                    onCheckOut = { navController.navigate(Routes.checkOut(it)) },
                )
            }
            composable(
                route = Routes.CHECK_IN,
                arguments = listOf(navArgument("reservationId") { type = NavType.StringType }),
            ) {
                CheckInScreen(
                    onBack = { navController.popBackStack() },
                    onCompleted = {
                        navController.popBackStack(Routes.RECEPTION, inclusive = false)
                    },
                )
            }
            composable(
                route = Routes.CHECK_OUT,
                arguments = listOf(navArgument("stayId") { type = NavType.StringType }),
            ) {
                CheckOutScreen(
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.popBackStack(Routes.RECEPTION, inclusive = false) },
                )
            }

            composable(Routes.GUESTS) {
                GuestsScreen(
                    onOpenGuest = { navController.navigate(Routes.guestDetail(it)) },
                    onAddGuest = { navController.navigate(Routes.guestForm(null)) },
                )
            }
            composable(
                route = Routes.GUEST_FORM,
                arguments = listOf(
                    navArgument("guestId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                GuestFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.GUEST_DETAIL,
                arguments = listOf(navArgument("guestId") { type = NavType.StringType }),
            ) {
                GuestDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.guestForm(it)) },
                    onCreateReservation = { navController.navigate(Routes.reservationNew(it)) },
                )
            }

            composable(Routes.FINANCE) { FinanceScreen() }
            composable(Routes.HOUSEKEEPING) { HousekeepingScreen() }
            composable(Routes.MAINTENANCE) {
                MaintenanceScreen(
                    onOpenTicket = { navController.navigate(Routes.maintenanceDetail(it)) },
                    onNewTicket = { navController.navigate(Routes.MAINTENANCE_NEW) },
                )
            }
            composable(Routes.MAINTENANCE_NEW) {
                MaintenanceFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { ticketId ->
                        navController.popBackStack()
                        navController.navigate(Routes.maintenanceDetail(ticketId))
                    },
                )
            }
            composable(
                route = Routes.MAINTENANCE_DETAIL,
                arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
            ) {
                MaintenanceDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.USERS) { UsersScreen() }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onLoggedOut = onSessionInvalid)
            }
            composable(Routes.MORE) {
                MoreScreen(
                    user = user,
                    onOpenGuests = { navController.navigate(Routes.GUESTS) },
                    onOpenFinance = { navController.navigate(Routes.FINANCE) },
                    onOpenHousekeeping = { navController.navigate(Routes.HOUSEKEEPING) },
                    onOpenMaintenance = { navController.navigate(Routes.MAINTENANCE) },
                    onOpenUsers = { navController.navigate(Routes.USERS) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
        }
    }
}

/**
 * Floating luxury bottom bar: a rounded glass pill with a gradient border and soft shadow.
 * The selected destination gets a full 3D gradient icon chip ([Icon3D]) in the theme accent;
 * unselected ones show the plain glyph in the muted surface colour.
 */
@Composable
private fun MehraBottomBar(
    destinations: List<BottomDestination>,
    backStackEntry: NavBackStackEntry?,
    onSelect: (BottomDestination) -> Unit,
) {
    val colors = MehraTheme.colors
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, shape),
            shape = shape,
            color = scheme.surface.copy(alpha = if (colors.isDark) 0.88f else 0.92f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        colors.accentStart.copy(alpha = 0.5f),
                        colors.cardBorder,
                        colors.accentEnd.copy(alpha = 0.5f),
                    )
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val selected = backStackEntry?.destination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    val icon = ImageVector.vectorResource(destination.iconRes)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(destination) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (selected) {
                            Icon3D(
                                icon = icon,
                                contentDescription = destination.label,
                                size = 40.dp,
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = destination.label,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) colors.accentEnd else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
