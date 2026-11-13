package ir.ahvaz.mehra.navigation

/** Every destination of the app. Argument names match the `SavedStateHandle` keys of the view models. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val ROOMS = "rooms"
    const val ROOM_DETAIL = "room/{roomId}"
    const val ROOM_FORM = "room-form?roomId={roomId}"
    const val RESERVATIONS = "reservations"
    const val RESERVATION_DETAIL = "reservation/{reservationId}"
    const val RESERVATION_NEW = "reservation-new?guestId={guestId}"
    const val RECEPTION = "reception"
    const val CHECK_IN = "check-in/{reservationId}"
    const val CHECK_OUT = "check-out/{stayId}"
    const val GUESTS = "guests"
    const val GUEST_DETAIL = "guest/{guestId}"
    const val GUEST_FORM = "guest-form?guestId={guestId}"
    const val FINANCE = "finance"
    const val HOUSEKEEPING = "housekeeping"
    const val MAINTENANCE = "maintenance"
    const val MAINTENANCE_DETAIL = "maintenance/{ticketId}"
    const val MAINTENANCE_NEW = "maintenance-new"
    const val USERS = "users"
    const val REPORTS = "reports"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val MORE = "more"
    const val DBTOOL = "dbtool"

    fun roomDetail(roomId: String) = "room/$roomId"
    fun roomForm(roomId: String?) = if (roomId.isNullOrBlank()) "room-form?roomId=" else "room-form?roomId=$roomId"
    fun reservationDetail(id: String) = "reservation/$id"
    fun reservationNew(guestId: String? = null) =
        if (guestId.isNullOrBlank()) "reservation-new?guestId=" else "reservation-new?guestId=$guestId"
    fun checkIn(reservationId: String) = "check-in/$reservationId"
    fun checkOut(stayId: String) = "check-out/$stayId"
    fun guestDetail(guestId: String) = "guest/$guestId"
    fun guestForm(guestId: String?) = if (guestId.isNullOrBlank()) "guest-form?guestId=" else "guest-form?guestId=$guestId"
    fun maintenanceDetail(ticketId: String) = "maintenance/$ticketId"
}

/** Destinations that keep the bottom navigation bar visible. */
val TopLevelRoutes = setOf(
    Routes.DASHBOARD,
    Routes.ROOMS,
    Routes.GUESTS,
    Routes.RESERVATIONS,
    Routes.RECEPTION,
    Routes.MORE,
)
