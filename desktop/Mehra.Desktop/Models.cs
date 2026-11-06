using System.Text.Json.Serialization;
using Avalonia.Media;

namespace Mehra.Desktop;

#region API DTOs (mirror of docs/API.md envelope + DTO field names)

public class ApiEnvelope<T>
{
    [JsonPropertyName("success")] public bool Success { get; set; }
    [JsonPropertyName("data")] public T? Data { get; set; }
    [JsonPropertyName("error")] public ApiErrorBody? Error { get; set; }
    [JsonPropertyName("traceId")] public string? TraceId { get; set; }
}

public class ApiErrorBody
{
    [JsonPropertyName("code")] public string? Code { get; set; }
    [JsonPropertyName("message")] public string? Message { get; set; }
}

public class Paged<T>
{
    [JsonPropertyName("items")] public List<T>? Items { get; set; }
    [JsonPropertyName("totalCount")] public int TotalCount { get; set; }
}

public class SessionUserDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("fullName")] public string? FullName { get; set; }
    [JsonPropertyName("username")] public string? Username { get; set; }
    [JsonPropertyName("role")] public string? Role { get; set; }
    [JsonPropertyName("roleTitle")] public string? RoleTitle { get; set; }
    [JsonPropertyName("permissions")] public List<string>? Permissions { get; set; }
}

public class LoginResponseDto
{
    [JsonPropertyName("accessToken")] public string? AccessToken { get; set; }
    [JsonPropertyName("refreshToken")] public string? RefreshToken { get; set; }
    [JsonPropertyName("user")] public SessionUserDto? User { get; set; }
}

public class RoomDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("floor")] public int Floor { get; set; }
    [JsonPropertyName("capacity")] public int Capacity { get; set; }
    [JsonPropertyName("roomTypeName")] public string? RoomTypeName { get; set; }
    [JsonPropertyName("ratePerNightRial")] public long? RatePerNightRial { get; set; }
    [JsonPropertyName("status")] public string? Status { get; set; }
    [JsonPropertyName("currentGuestName")] public string? CurrentGuestName { get; set; }
}

public class ReservationDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("code")] public string? Code { get; set; }
    [JsonPropertyName("guestName")] public string? GuestName { get; set; }
    [JsonPropertyName("guestPhone")] public string? GuestPhone { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("checkInDate")] public string? CheckInDate { get; set; }
    [JsonPropertyName("checkOutDate")] public string? CheckOutDate { get; set; }
    [JsonPropertyName("guestsCount")] public int GuestsCount { get; set; }
    [JsonPropertyName("totalRial")] public long? TotalRial { get; set; }
    [JsonPropertyName("paidRial")] public long? PaidRial { get; set; }
    [JsonPropertyName("status")] public string? Status { get; set; }
}

public class StayDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("reservationCode")] public string? ReservationCode { get; set; }
    [JsonPropertyName("guestName")] public string? GuestName { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("checkInAtUtc")] public string? CheckInAtUtc { get; set; }
    [JsonPropertyName("plannedCheckOut")] public string? PlannedCheckOut { get; set; }
    [JsonPropertyName("guestsCount")] public int GuestsCount { get; set; }
    [JsonPropertyName("status")] public string? Status { get; set; }
}

public class OccupancyPointDto
{
    [JsonPropertyName("date")] public string? Date { get; set; }
    [JsonPropertyName("occupied")] public int Occupied { get; set; }
    [JsonPropertyName("total")] public int Total { get; set; }
}

public class DashboardAlertDto
{
    [JsonPropertyName("title")] public string? Title { get; set; }
    [JsonPropertyName("message")] public string? Message { get; set; }
    [JsonPropertyName("severity")] public string? Severity { get; set; }
}

public class DashboardActivityDto
{
    [JsonPropertyName("title")] public string? Title { get; set; }
    [JsonPropertyName("createdAtUtc")] public string? CreatedAtUtc { get; set; }
}

public class DashboardDto
{
    [JsonPropertyName("businessDate")] public string? BusinessDate { get; set; }
    [JsonPropertyName("totalRooms")] public int TotalRooms { get; set; }
    [JsonPropertyName("freeRooms")] public int FreeRooms { get; set; }
    [JsonPropertyName("reservedRooms")] public int ReservedRooms { get; set; }
    [JsonPropertyName("occupiedRooms")] public int OccupiedRooms { get; set; }
    [JsonPropertyName("cleaningRooms")] public int CleaningRooms { get; set; }
    [JsonPropertyName("maintenanceRooms")] public int MaintenanceRooms { get; set; }
    [JsonPropertyName("outOfServiceRooms")] public int OutOfServiceRooms { get; set; }
    [JsonPropertyName("todayCheckIns")] public int TodayCheckIns { get; set; }
    [JsonPropertyName("todayCheckOuts")] public int TodayCheckOuts { get; set; }
    [JsonPropertyName("revenueTodayRial")] public long RevenueTodayRial { get; set; }
    [JsonPropertyName("occupancyPercent")] public double OccupancyPercent { get; set; }
    [JsonPropertyName("occupancySeries")] public List<OccupancyPointDto>? OccupancySeries { get; set; }
    [JsonPropertyName("recentActivities")] public List<DashboardActivityDto>? RecentActivities { get; set; }
    [JsonPropertyName("alerts")] public List<DashboardAlertDto>? Alerts { get; set; }
}

#endregion

#region Session + display models

public class SessionUser
{
    public string FullName { get; set; } = "";
    public string Username { get; set; } = "";
    public string RoleTitle { get; set; } = "";
}

public class Session
{
    public IHotelApi Api { get; }
    public SessionUser User { get; }
    public Session(IHotelApi api, SessionUser user) { Api = api; User = user; }
}

public class StatCard
{
    public string Label { get; set; } = "";
    public string Value { get; set; } = "";
    public string Caption { get; set; } = "";
    public IBrush Accent { get; set; } = Brushes.Black;
}

public class AlertItem
{
    public string Title { get; set; } = "";
    public string Message { get; set; } = "";
    public IBrush Accent { get; set; } = Brushes.Gray;
}

public class ActivityItem
{
    public string Title { get; set; } = "";
    public string TimeFa { get; set; } = "";
}

public class RoomCard
{
    public string Number { get; set; } = "";
    public string TypeName { get; set; } = "";
    public string Details { get; set; } = "";
    public string RateFa { get; set; } = "";
    public string StatusFa { get; set; } = "";
    public IBrush StatusBrush { get; set; } = Brushes.Gray;
    public string GuestName { get; set; } = "";
}

public class ReservationRow
{
    public string Code { get; set; } = "";
    public string GuestName { get; set; } = "";
    public string PhoneFa { get; set; } = "";
    public string RoomNumber { get; set; } = "";
    public string DatesFa { get; set; } = "";
    public string GuestsFa { get; set; } = "";
    public string AmountFa { get; set; } = "";
    public string PaidFa { get; set; } = "";
    public string StatusFa { get; set; } = "";
    public IBrush StatusBrush { get; set; } = Brushes.Gray;
}

public class StayRow
{
    public string Code { get; set; } = "";
    public string GuestName { get; set; } = "";
    public string RoomNumber { get; set; } = "";
    public string SinceFa { get; set; } = "";
    public string PlannedOutFa { get; set; } = "";
    public string GuestsFa { get; set; } = "";
    public string StatusFa { get; set; } = "";
    public IBrush StatusBrush { get; set; } = Brushes.Gray;
}

#endregion
