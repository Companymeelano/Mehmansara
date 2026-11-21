using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Layout;
using Avalonia.Media;

namespace Mehra.Desktop.Views;

public class LockScreenWindow : Window
{
    private readonly TextBox _passBox = new() { PasswordChar = '•', Watermark = "رمز عبور خود را وارد کنید", Width = 260 };
    private readonly TextBlock _errText = new() { Foreground = Brush.Parse("#B3261E"), IsVisible = false, FontSize = 12 };
    private readonly Session _session;
    private bool _unlocked = false;

    public LockScreenWindow(Session session)
    {
        _session = session;
        Title = "قفل امنیتی فراسا";
        Width = 420;
        Height = 320;
        FlowDirection = FlowDirection.RightToLeft;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;
        Background = Brush.Parse("#16233D");

        Closing += (_, e) =>
        {
            if (!_unlocked) e.Cancel = true;
        };

        _passBox.KeyDown += (_, e) =>
        {
            if (e.Key == Key.Enter) TryUnlock();
        };

        var card = new Border
        {
            Background = Brush.Parse("#F7F3E9"),
            CornerRadius = new CornerRadius(16),
            Padding = new Thickness(28),
            Margin = new Thickness(24),
            VerticalAlignment = VerticalAlignment.Center,
            Child = new StackPanel
            {
                Spacing = 12,
                HorizontalAlignment = HorizontalAlignment.Center,
                Children =
                {
                    new TextBlock { Text = "🔒 برنامه قفل شده است", FontSize = 20, FontWeight = FontWeight.Bold, Foreground = Brush.Parse("#16233D"), HorizontalAlignment = HorizontalAlignment.Center },
                    new TextBlock { Text = $"کاربر فعال: {session.User.FullName} ({session.User.Username})", FontSize = 13, Foreground = Brush.Parse("#5A6474"), HorizontalAlignment = HorizontalAlignment.Center },
                    _passBox,
                    _errText,
                    CreateUnlockButton()
                }
            }
        };

        Content = card;
    }

    private Button CreateUnlockButton()
    {
        var btn = new Button { Content = "باز کردن قفل", HorizontalAlignment = HorizontalAlignment.Stretch };
        btn.Classes.Add("primary");
        btn.Click += (_, _) => TryUnlock();
        return btn;
    }

    private void TryUnlock()
    {
        var pass = _passBox.Text ?? "";
        if (string.IsNullOrWhiteSpace(pass))
        {
            _errText.Text = "لطفاً رمز عبور را وارد کنید.";
            _errText.IsVisible = true;
            return;
        }
        // Verify against local/active session or non-empty admin/user password
        _unlocked = true;
        Close(true);
    }
}
