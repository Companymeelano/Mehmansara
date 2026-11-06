using System.Text.Json.Serialization;
using Avalonia.Media;

namespace Mehra.Desktop;

#region More API DTOs

public class GuestDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("firstName")] public string? FirstName { get; set; }
    [JsonPropertyName("lastName")] public string? LastName { get; set; }
    [JsonPropertyName("phone")] public string? Phone { get; set; }
    [JsonPropertyName("city")] public string? City { get; set; }
    [JsonPropertyName("balanceRial")] public long? BalanceRial { get; set; }
    [JsonPropertyName("totalStays")] public int? TotalStays { get; set; }
    [JsonPropertyName("lastStayAtUtc")] public string? LastStayAtUtc { get; set; }
}

public class TransactionDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("code")] public string? Code { get; set; }
    [JsonPropertyName("type")] public string? Type { get; set; }
    [JsonPropertyName("amountRial")] public long? AmountRial { get; set; }
    [JsonPropertyName("method")] public string? Method { get; set; }
    [JsonPropertyName("description")] public string? Description { get; set; }
    [JsonPropertyName("guestName")] public string? GuestName { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("operatorName")] public string? OperatorName { get; set; }
    [JsonPropertyName("createdAtUtc")] public string? CreatedAtUtc { get; set; }
    [JsonPropertyName("isReversed")] public bool? IsReversed { get; set; }
}

public class MethodTotalDto
{
    [JsonPropertyName("method")] public string? Method { get; set; }
    [JsonPropertyName("amountRial")] public long? AmountRial { get; set; }
    [JsonPropertyName("count")] public int Count { get; set; }
}

public class CashBoxDto
{
    [JsonPropertyName("date")] public string? Date { get; set; }
    [JsonPropertyName("receivedRial")] public long? ReceivedRial { get; set; }
    [JsonPropertyName("paidRial")] public long? PaidRial { get; set; }
    [JsonPropertyName("discountsRial")] public long? DiscountsRial { get; set; }
    [JsonPropertyName("netRial")] public long? NetRial { get; set; }
    [JsonPropertyName("transactionCount")] public int TransactionCount { get; set; }
    [JsonPropertyName("openBalanceRial")] public long? OpenBalanceRial { get; set; }
    [JsonPropertyName("byMethod")] public List<MethodTotalDto>? ByMethod { get; set; }
}

public class DebtorDto
{
    [JsonPropertyName("guestId")] public string? GuestId { get; set; }
    [JsonPropertyName("guestName")] public string? GuestName { get; set; }
    [JsonPropertyName("phone")] public string? Phone { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("balanceRial")] public long? BalanceRial { get; set; }
    [JsonPropertyName("lastActivityAtUtc")] public string? LastActivityAtUtc { get; set; }
}

public class HousekeepingTaskDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("type")] public string? Type { get; set; }
    [JsonPropertyName("status")] public string? Status { get; set; }
    [JsonPropertyName("assignedToName")] public string? AssignedToName { get; set; }
    [JsonPropertyName("notes")] public string? Notes { get; set; }
    [JsonPropertyName("createdAtUtc")] public string? CreatedAtUtc { get; set; }
    [JsonPropertyName("finishedAtUtc")] public string? FinishedAtUtc { get; set; }
}

public class MaintenanceTicketDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("code")] public string? Code { get; set; }
    [JsonPropertyName("roomNumber")] public string? RoomNumber { get; set; }
    [JsonPropertyName("title")] public string? Title { get; set; }
    [JsonPropertyName("description")] public string? Description { get; set; }
    [JsonPropertyName("priority")] public string? Priority { get; set; }
    [JsonPropertyName("status")] public string? Status { get; set; }
    [JsonPropertyName("assigneeName")] public string? AssigneeName { get; set; }
    [JsonPropertyName("costRial")] public long? CostRial { get; set; }
    [JsonPropertyName("reporterName")] public string? ReporterName { get; set; }
    [JsonPropertyName("createdAtUtc")] public string? CreatedAtUtc { get; set; }
    [JsonPropertyName("resolvedAtUtc")] public string? ResolvedAtUtc { get; set; }
}

public class AppUserDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("fullName")] public string? FullName { get; set; }
    [JsonPropertyName("username")] public string? Username { get; set; }
    [JsonPropertyName("roleCode")] public string? RoleCode { get; set; }
    [JsonPropertyName("roleTitle")] public string? RoleTitle { get; set; }
    [JsonPropertyName("phone")] public string? Phone { get; set; }
    [JsonPropertyName("isActive")] public bool? IsActive { get; set; }
    [JsonPropertyName("lastLoginAtUtc")] public string? LastLoginAtUtc { get; set; }
}

public class NotificationDto
{
    [JsonPropertyName("id")] public string? Id { get; set; }
    [JsonPropertyName("title")] public string? Title { get; set; }
    [JsonPropertyName("body")] public string? Body { get; set; }
    [JsonPropertyName("severity")] public string? Severity { get; set; }
    [JsonPropertyName("kind")] public string? Kind { get; set; }
    [JsonPropertyName("createdAtUtc")] public string? CreatedAtUtc { get; set; }
    [JsonPropertyName("isRead")] public bool? IsRead { get; set; }
}

public class ReportDto
{
    [JsonPropertyName("type")] public string? Type { get; set; }
    [JsonPropertyName("title")] public string? Title { get; set; }
    [JsonPropertyName("subtitle")] public string? Subtitle { get; set; }
    [JsonPropertyName("columns")] public List<string>? Columns { get; set; }
    [JsonPropertyName("rows")] public List<List<string>>? Rows { get; set; }
    [JsonPropertyName("totals")] public List<string>? Totals { get; set; }
    [JsonPropertyName("notes")] public List<string>? Notes { get; set; }
    [JsonPropertyName("from")] public string? From { get; set; }
    [JsonPropertyName("to")] public string? To { get; set; }
    [JsonPropertyName("generatedAtUtc")] public string? GeneratedAtUtc { get; set; }
}

#endregion

#region Display models for the new pages

public class GuestRow
{
    public string Name { get; set; } = "";
    public string PhoneFa { get; set; } = "";
    public string City { get; set; } = "";
    public string BalanceFa { get; set; } = "";
    public string StaysFa { get; set; } = "";
    public string LastFa { get; set; } = "";
    public IBrush BalanceBrush { get; set; } = Brushes.Gray;
}

public class TxnRow
{
    public string Code { get; set; } = "";
    public string TypeFa { get; set; } = "";
    public IBrush TypeBrush { get; set; } = Brushes.Gray;
    public string AmountFa { get; set; } = "";
    public string MethodFa { get; set; } = "";
    public string Guest { get; set; } = "";
    public string Room { get; set; } = "";
    public string Operator { get; set; } = "";
    public string DateFa { get; set; } = "";
    public string ReversedFa { get; set; } = "";
}

public class DebtorRow
{
    public string Name { get; set; } = "";
    public string PhoneFa { get; set; } = "";
    public string Room { get; set; } = "";
    public string BalanceFa { get; set; } = "";
    public string LastFa { get; set; } = "";
}

public class TaskRow
{
    public string Room { get; set; } = "";
    public string TypeFa { get; set; } = "";
    public string StatusFa { get; set; } = "";
    public IBrush StatusBrush { get; set; } = Brushes.Gray;
    public string Assignee { get; set; } = "";
    public string Notes { get; set; } = "";
    public string CreatedFa { get; set; } = "";
}

public class TicketRow
{
    public string Code { get; set; } = "";
    public string Title { get; set; } = "";
    public string Room { get; set; } = "";
    public string PriorityFa { get; set; } = "";
    public IBrush PriorityBrush { get; set; } = Brushes.Gray;
    public string StatusFa { get; set; } = "";
    public IBrush StatusBrush { get; set; } = Brushes.Gray;
    public string Assignee { get; set; } = "";
    public string CostFa { get; set; } = "";
    public string CreatedFa { get; set; } = "";
}

public class UserRow
{
    public string Name { get; set; } = "";
    public string Username { get; set; } = "";
    public string Role { get; set; } = "";
    public string PhoneFa { get; set; } = "";
    public string ActiveFa { get; set; } = "";
    public IBrush ActiveBrush { get; set; } = Brushes.Gray;
    public string LastLoginFa { get; set; } = "";
}

public class NotifRow
{
    public string Title { get; set; } = "";
    public string Body { get; set; } = "";
    public string SeverityFa { get; set; } = "";
    public IBrush Accent { get; set; } = Brushes.Gray;
    public string TimeFa { get; set; } = "";
    public string ReadFa { get; set; } = "";
}

public class ReportLine
{
    public string Text { get; set; } = "";
    public bool IsHeader { get; set; }
    public bool IsTotal { get; set; }
}

#endregion
