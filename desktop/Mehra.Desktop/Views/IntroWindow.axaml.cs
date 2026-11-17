using Avalonia.Controls;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Threading;

namespace Mehra.Desktop.Views;

public partial class IntroWindow : Window
{
    private readonly DispatcherTimer _timer;
    private readonly DateTime _startUtc = DateTime.UtcNow;
    private bool _finished;
    private const double DurationSec = 3.6;

    public IntroWindow()
    {
        InitializeComponent();
        _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(16) };
        _timer.Tick += OnTick;
        _timer.Start();
    }

    private void OnTick(object? sender, EventArgs e)
    {
        var t = (DateTime.UtcNow - _startUtc).TotalSeconds;
        var p = Math.Clamp(t / DurationSec, 0, 1);

        this.FindControl<Border>("LogoRing")!.Opacity = Clamp01(t / 0.6);
        this.FindControl<TextBlock>("BrandText")!.Opacity = Clamp01((t - 0.25) / 0.6);
        this.FindControl<Border>("Divider")!.Opacity = Clamp01((t - 0.45) / 0.5);
        this.FindControl<StackPanel>("CreatorPanel")!.Opacity = Clamp01((t - 0.7) / 0.7);
        this.FindControl<Border>("ContactCard")!.Opacity = Clamp01((t - 1.1) / 0.7);
        this.FindControl<Border>("ProgressFill")!.Width = 240 * p;

        if (t >= DurationSec)
            Finish();
    }

    private static double Clamp01(double v) => v < 0 ? 0 : (v > 1 ? 1 : v);

    private void OnSkip(object? sender, Avalonia.Interactivity.RoutedEventArgs e) => Finish();

    private void Finish()
    {
        if (_finished) return;
        _finished = true;
        _timer.Stop();
        var login = new LoginWindow();
        if (Avalonia.Application.Current?.ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
            desktop.MainWindow = login;
        login.Show();
        Close();
    }
}
