using Avalonia.Controls;
using Mehra.Desktop.Views.Pages;

namespace Mehra.Desktop.Views;

public partial class MainWindow : Window
{
    private readonly Session _session;

    public MainWindow(Session session)
    {
        _session = session;
        InitializeComponent();
        this.FindControl<TextBlock>("UserText")!.Text = session.User.FullName;
        this.FindControl<TextBlock>("RoleText")!.Text =
            (session.User.RoleTitle.Length > 0 ? session.User.RoleTitle + " — " : "") + session.Api.ServerLabel;
        this.FindControl<TextBlock>("StatusText")!.Text = "";
        var nav = this.FindControl<ListBox>("NavList")!;
        nav.SelectedIndex = 0;
    }

    private void OnNavChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (sender is not ListBox list) return;
        if (list.SelectedItem is not ListBoxItem item) return;
        var tag = item.Tag as string;
        var area = this.FindControl<ContentControl>("ContentArea")!;
        var status = this.FindControl<TextBlock>("StatusText")!;
        switch (tag)
        {
            case "home": area.Content = new HomePage(_session); status.Text = Fa.DateLong(DateTime.Today); break;
            case "rooms": area.Content = new RoomsPage(_session); status.Text = "فهرست اتاق‌ها"; break;
            case "reservations": area.Content = new ReservationsPage(_session); status.Text = "فهرست رزروها"; break;
            case "reception": area.Content = new ReceptionPage(_session); status.Text = "پذیرش — اقامت‌های فعال"; break;
            case "settings": area.Content = new SettingsPage(_session); status.Text = "تنظیمات"; break;
        }
    }

    private void OnLogout(object? sender, Avalonia.Interactivity.RoutedEventArgs e)
    {
        new LoginWindow().Show();
        Close();
    }
}
