package ir.ahvaz.mehra.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    val icon: ImageVector,
    val permission: String?,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "داشبورد", Icons.Rounded.Dashboard, Permissions.DASHBOARD_VIEW),
    BottomDestination(Routes.ROOMS, "اتاق‌ها", Icons.Rounded.Bed, Permissions.ROOMS_VIEW),
    BottomDestination(Routes.RESERVATIONS, "رزروها", Icons.Rounded.EventNote, Permissions.RESERVATIONS_VIEW),
    BottomDestination(Routes.RECEPTION, "پذیرش", Icons.Rounded.Hotel, Permissions.RECEPTION_OPERATE),
    BottomDestination(Routes.MORE, "بیشتر", Icons.Rounded.Apps, null),
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
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
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
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
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
