package ir.ahvaz.mehra.data.remote.api

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import ir.ahvaz.mehra.core.network.ApiEnvelope
import ir.ahvaz.mehra.core.network.PagedData
import ir.ahvaz.mehra.data.remote.dto.AppUserDto
import ir.ahvaz.mehra.data.remote.dto.AssignRequestDto
import ir.ahvaz.mehra.data.remote.dto.AuditLogDto
import ir.ahvaz.mehra.data.remote.dto.AuthResponseDto
import ir.ahvaz.mehra.data.remote.dto.AvailableRoomDto
import ir.ahvaz.mehra.data.remote.dto.CalendarEntryDto
import ir.ahvaz.mehra.data.remote.dto.CancelReservationRequestDto
import ir.ahvaz.mehra.data.remote.dto.CashBoxDto
import ir.ahvaz.mehra.data.remote.dto.ChangePasswordRequestDto
import ir.ahvaz.mehra.data.remote.dto.CheckInRequestDto
import ir.ahvaz.mehra.data.remote.dto.CheckOutRequestDto
import ir.ahvaz.mehra.data.remote.dto.CompleteTaskRequestDto
import ir.ahvaz.mehra.data.remote.dto.CostRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateMaintenanceRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateReservationRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateTaskRequestDto
import ir.ahvaz.mehra.data.remote.dto.CreateUserRequestDto
import ir.ahvaz.mehra.data.remote.dto.DailyPointDto
import ir.ahvaz.mehra.data.remote.dto.DashboardSummaryDto
import ir.ahvaz.mehra.data.remote.dto.DebtorDto
import ir.ahvaz.mehra.data.remote.dto.DiscountRequestDto
import ir.ahvaz.mehra.data.remote.dto.ExtendStayRequestDto
import ir.ahvaz.mehra.data.remote.dto.FinanceSummaryDto
import ir.ahvaz.mehra.data.remote.dto.GuestDetailDto
import ir.ahvaz.mehra.data.remote.dto.GuestDraftDto
import ir.ahvaz.mehra.data.remote.dto.GuestDto
import ir.ahvaz.mehra.data.remote.dto.HousekeepingTaskDto
import ir.ahvaz.mehra.data.remote.dto.LoginRequestDto
import ir.ahvaz.mehra.data.remote.dto.LogoutRequestDto
import ir.ahvaz.mehra.data.remote.dto.MaintenanceStatusRequestDto
import ir.ahvaz.mehra.data.remote.dto.MaintenanceTicketDto
import ir.ahvaz.mehra.data.remote.dto.MoveRoomRequestDto
import ir.ahvaz.mehra.data.remote.dto.NewTransactionRequestDto
import ir.ahvaz.mehra.data.remote.dto.NotificationDto
import ir.ahvaz.mehra.data.remote.dto.PerformanceDto
import ir.ahvaz.mehra.data.remote.dto.PromoteWaitlistRequestDto
import ir.ahvaz.mehra.data.remote.dto.QuoteDto
import ir.ahvaz.mehra.data.remote.dto.QuoteRequestDto
import ir.ahvaz.mehra.data.remote.dto.ReceiptDto
import ir.ahvaz.mehra.data.remote.dto.RefreshRequestDto
import ir.ahvaz.mehra.data.remote.dto.ReportDto
import ir.ahvaz.mehra.data.remote.dto.ReservationDto
import ir.ahvaz.mehra.data.remote.dto.ResetPasswordRequestDto
import ir.ahvaz.mehra.data.remote.dto.RoleDto
import ir.ahvaz.mehra.data.remote.dto.RoomDraftDto
import ir.ahvaz.mehra.data.remote.dto.RoomDto
import ir.ahvaz.mehra.data.remote.dto.RoomHistoryDto
import ir.ahvaz.mehra.data.remote.dto.RoomLookupsDto
import ir.ahvaz.mehra.data.remote.dto.RoomStatusRequestDto
import ir.ahvaz.mehra.data.remote.dto.ServerInfoDto
import ir.ahvaz.mehra.data.remote.dto.SessionUserDto
import ir.ahvaz.mehra.data.remote.dto.StaffDto
import ir.ahvaz.mehra.data.remote.dto.StayDto
import ir.ahvaz.mehra.data.remote.dto.ToggleActiveRequestDto
import ir.ahvaz.mehra.data.remote.dto.TransactionDto
import ir.ahvaz.mehra.data.remote.dto.WaitlistEntryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit definitions for the MEHRA backend.
 *
 * Every path here is part of the published contract in docs/API_CONTRACT.md. Sensitive
 * mutations require an `Idempotency-Key` header so a network retry can never double-post a
 * payment, a check-in or a reservation.
 */
interface AuthApi {

    @POST("/api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<ApiEnvelope<AuthResponseDto>>

    @POST("/api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): Response<ApiEnvelope<AuthResponseDto>>

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): Response<ApiEnvelope<JsonElement>>

    @POST("/api/v1/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): Response<ApiEnvelope<JsonElement>>

    @GET("/api/v1/auth/me")
    suspend fun me(): Response<ApiEnvelope<SessionUserDto>>
}

interface SystemApi {

    @GET("/api/v1/system/health")
    suspend fun health(): Response<ApiEnvelope<ServerInfoDto>>

    @GET("/api/v1/notifications")
    suspend fun notifications(
        @Query("unreadOnly") unreadOnly: Boolean,
    ): Response<ApiEnvelope<List<NotificationDto>>>

    @POST("/api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<ApiEnvelope<JsonElement>>

    @POST("/api/v1/notifications/read-all")
    suspend fun markAllRead(): Response<ApiEnvelope<JsonElement>>
}

interface DashboardApi {

    @GET("/api/v1/dashboard/summary")
    suspend fun summary(@Query("date") date: String?): Response<ApiEnvelope<DashboardSummaryDto>>
}

interface RoomsApi {

    @GET("/api/v1/rooms")
    suspend fun list(
        @Query("search") search: String?,
        @Query("status") status: String?,
        @Query("floor") floor: Int?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<ApiEnvelope<PagedData<RoomDto>>>

    @GET("/api/v1/rooms/{id}")
    suspend fun get(@Path("id") id: String): Response<ApiEnvelope<RoomDto>>

    @GET("/api/v1/rooms/lookups")
    suspend fun lookups(): Response<ApiEnvelope<RoomLookupsDto>>

    @GET("/api/v1/rooms/{id}/history")
    suspend fun history(@Path("id") id: String): Response<ApiEnvelope<List<RoomHistoryDto>>>

    @POST("/api/v1/rooms")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RoomDraftDto,
    ): Response<ApiEnvelope<RoomDto>>

    @PUT("/api/v1/rooms/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: RoomDraftDto,
    ): Response<ApiEnvelope<RoomDto>>

    @PATCH("/api/v1/rooms/{id}/status")
    suspend fun changeStatus(
        @Path("id") id: String,
        @Body body: RoomStatusRequestDto,
    ): Response<ApiEnvelope<RoomDto>>

    @POST("/api/v1/housekeeping/tasks")
    suspend fun requestCleaning(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateTaskRequestDto,
    ): Response<ApiEnvelope<HousekeepingTaskDto>>

    @POST("/api/v1/maintenance/tickets")
    suspend fun createMaintenance(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMaintenanceRequestDto,
    ): Response<ApiEnvelope<MaintenanceTicketDto>>
}

interface GuestsApi {

    @GET("/api/v1/guests")
    suspend fun list(
        @Query("search") search: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<ApiEnvelope<PagedData<GuestDto>>>

    @GET("/api/v1/guests/{id}")
    suspend fun get(@Path("id") id: String): Response<ApiEnvelope<GuestDetailDto>>

    @POST("/api/v1/guests")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: GuestDraftDto,
    ): Response<ApiEnvelope<GuestDto>>

    @PUT("/api/v1/guests/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: GuestDraftDto,
    ): Response<ApiEnvelope<GuestDto>>
}

interface ReservationsApi {

    @GET("/api/v1/reservations")
    suspend fun list(
        @Query("search") search: String?,
        @Query("status") status: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<ApiEnvelope<PagedData<ReservationDto>>>

    @GET("/api/v1/reservations/{id}")
    suspend fun get(@Path("id") id: String): Response<ApiEnvelope<ReservationDto>>

    @GET("/api/v1/reservations/availability")
    suspend fun availability(
        @Query("checkIn") checkIn: String,
        @Query("checkOut") checkOut: String,
        @Query("guestsCount") guestsCount: Int,
        @Query("roomTypeId") roomTypeId: String?,
    ): Response<ApiEnvelope<List<AvailableRoomDto>>>

    @POST("/api/v1/reservations/quote")
    suspend fun quote(@Body body: QuoteRequestDto): Response<ApiEnvelope<QuoteDto>>

    @POST("/api/v1/reservations")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateReservationRequestDto,
    ): Response<ApiEnvelope<ReservationDto>>

    @POST("/api/v1/reservations/{id}/confirm")
    suspend fun confirm(@Path("id") id: String): Response<ApiEnvelope<ReservationDto>>

    @POST("/api/v1/reservations/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String,
        @Body body: CancelReservationRequestDto,
    ): Response<ApiEnvelope<ReservationDto>>

    @GET("/api/v1/reservations/waitlist")
    suspend fun waitlist(): Response<ApiEnvelope<List<WaitlistEntryDto>>>

    @POST("/api/v1/reservations/waitlist/{id}/promote")
    suspend fun promoteWaitlist(
        @Path("id") id: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PromoteWaitlistRequestDto,
    ): Response<ApiEnvelope<ReservationDto>>

    @GET("/api/v1/reservations/calendar")
    suspend fun calendar(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiEnvelope<List<CalendarEntryDto>>>
}

interface ReceptionApi {

    @GET("/api/v1/reception/reservations")
    suspend fun searchReservations(
        @Query("query") query: String,
    ): Response<ApiEnvelope<List<ReservationDto>>>

    @GET("/api/v1/reception/stays")
    suspend fun activeStays(
        @Query("query") query: String?,
    ): Response<ApiEnvelope<List<StayDto>>>

    @GET("/api/v1/reception/stays/{id}")
    suspend fun stay(@Path("id") id: String): Response<ApiEnvelope<StayDto>>

    @POST("/api/v1/reception/{reservationId}/check-in")
    suspend fun checkIn(
        @Path("reservationId") reservationId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CheckInRequestDto,
    ): Response<ApiEnvelope<StayDto>>

    @POST("/api/v1/reception/stays/{stayId}/check-out")
    suspend fun checkOut(
        @Path("stayId") stayId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CheckOutRequestDto,
    ): Response<ApiEnvelope<ReceiptDto>>

    @POST("/api/v1/reception/stays/{stayId}/extend")
    suspend fun extend(
        @Path("stayId") stayId: String,
        @Body body: ExtendStayRequestDto,
    ): Response<ApiEnvelope<StayDto>>

    @POST("/api/v1/reception/stays/{stayId}/move-room")
    suspend fun moveRoom(
        @Path("stayId") stayId: String,
        @Body body: MoveRoomRequestDto,
    ): Response<ApiEnvelope<StayDto>>

    @GET("/api/v1/reception/stays/{stayId}/receipt")
    suspend fun receipt(@Path("stayId") stayId: String): Response<ApiEnvelope<ReceiptDto>>

    @GET("/api/v1/reception/document-types")
    suspend fun documentTypes(): Response<ApiEnvelope<List<String>>>
}

interface FinanceApi {

    @GET("/api/v1/finance/transactions")
    suspend fun transactions(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("type") type: String?,
        @Query("method") method: String?,
        @Query("search") search: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<ApiEnvelope<PagedData<TransactionDto>>>

    @GET("/api/v1/finance/cashbox")
    suspend fun cashBox(@Query("date") date: String): Response<ApiEnvelope<CashBoxDto>>

    @GET("/api/v1/finance/summary")
    suspend fun summary(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiEnvelope<FinanceSummaryDto>>

    @GET("/api/v1/finance/debtors")
    suspend fun debtors(): Response<ApiEnvelope<List<DebtorDto>>>

    @POST("/api/v1/finance/transactions")
    suspend fun register(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: NewTransactionRequestDto,
    ): Response<ApiEnvelope<TransactionDto>>

    @POST("/api/v1/finance/discounts")
    suspend fun discount(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DiscountRequestDto,
    ): Response<ApiEnvelope<TransactionDto>>
}

interface HousekeepingApi {

    @GET("/api/v1/housekeeping/tasks")
    suspend fun tasks(@Query("status") status: String?): Response<ApiEnvelope<List<HousekeepingTaskDto>>>

    @GET("/api/v1/housekeeping/staff")
    suspend fun staff(): Response<ApiEnvelope<List<StaffDto>>>

    @POST("/api/v1/housekeeping/tasks")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateTaskRequestDto,
    ): Response<ApiEnvelope<HousekeepingTaskDto>>

    @POST("/api/v1/housekeeping/tasks/{id}/assign")
    suspend fun assign(
        @Path("id") id: String,
        @Body body: AssignRequestDto,
    ): Response<ApiEnvelope<HousekeepingTaskDto>>

    @POST("/api/v1/housekeeping/tasks/{id}/start")
    suspend fun start(@Path("id") id: String): Response<ApiEnvelope<HousekeepingTaskDto>>

    @POST("/api/v1/housekeeping/tasks/{id}/complete")
    suspend fun complete(
        @Path("id") id: String,
        @Body body: CompleteTaskRequestDto,
    ): Response<ApiEnvelope<HousekeepingTaskDto>>

    @POST("/api/v1/housekeeping/tasks/{id}/approve")
    suspend fun approve(@Path("id") id: String): Response<ApiEnvelope<HousekeepingTaskDto>>

    @GET("/api/v1/housekeeping/performance")
    suspend fun performance(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiEnvelope<List<PerformanceDto>>>
}

interface MaintenanceApi {

    @GET("/api/v1/maintenance/tickets")
    suspend fun tickets(
        @Query("status") status: String?,
        @Query("priority") priority: String?,
    ): Response<ApiEnvelope<List<MaintenanceTicketDto>>>

    @GET("/api/v1/maintenance/tickets/{id}")
    suspend fun ticket(@Path("id") id: String): Response<ApiEnvelope<MaintenanceTicketDto>>

    @GET("/api/v1/maintenance/technicians")
    suspend fun technicians(): Response<ApiEnvelope<List<StaffDto>>>

    @POST("/api/v1/maintenance/tickets")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMaintenanceRequestDto,
    ): Response<ApiEnvelope<MaintenanceTicketDto>>

    @POST("/api/v1/maintenance/tickets/{id}/assign")
    suspend fun assign(
        @Path("id") id: String,
        @Body body: AssignRequestDto,
    ): Response<ApiEnvelope<MaintenanceTicketDto>>

    @PATCH("/api/v1/maintenance/tickets/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: MaintenanceStatusRequestDto,
    ): Response<ApiEnvelope<MaintenanceTicketDto>>

    @POST("/api/v1/maintenance/tickets/{id}/cost")
    suspend fun registerCost(
        @Path("id") id: String,
        @Body body: CostRequestDto,
    ): Response<ApiEnvelope<MaintenanceTicketDto>>

    @POST("/api/v1/maintenance/tickets/{id}/release-room")
    suspend fun releaseRoom(@Path("id") id: String): Response<ApiEnvelope<MaintenanceTicketDto>>

    /**
     * Fault photos are uploaded as multipart form data after the ticket exists.
     * The endpoint returns the stored photo URLs.
     */
    @Multipart
    @POST("/api/v1/maintenance/tickets/{id}/photos")
    suspend fun uploadPhotos(
        @Path("id") id: String,
        @Part photos: List<MultipartBody.Part>,
    ): Response<ApiEnvelope<List<String>>>
}

interface UsersApi {

    @GET("/api/v1/users")
    suspend fun list(@Query("search") search: String?): Response<ApiEnvelope<PagedData<AppUserDto>>>

    @GET("/api/v1/roles")
    suspend fun roles(): Response<ApiEnvelope<List<RoleDto>>>

    @POST("/api/v1/users")
    suspend fun create(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateUserRequestDto,
    ): Response<ApiEnvelope<AppUserDto>>

    @PUT("/api/v1/users/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: CreateUserRequestDto,
    ): Response<ApiEnvelope<AppUserDto>>

    @PATCH("/api/v1/users/{id}/active")
    suspend fun setActive(
        @Path("id") id: String,
        @Body body: ToggleActiveRequestDto,
    ): Response<ApiEnvelope<AppUserDto>>

    @POST("/api/v1/users/{id}/reset-password")
    suspend fun resetPassword(
        @Path("id") id: String,
        @Body body: ResetPasswordRequestDto,
    ): Response<ApiEnvelope<JsonElement>>

    @GET("/api/v1/audit-logs")
    suspend fun auditLogs(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiEnvelope<List<AuditLogDto>>>
}

interface ReportsApi {

    @GET("/api/v1/reports/{key}")
    suspend fun report(
        @Path("key") key: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiEnvelope<ReportDto>>
}
