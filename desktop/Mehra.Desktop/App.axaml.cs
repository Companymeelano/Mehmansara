using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Mehra.Desktop.Views;

namespace Mehra.Desktop;

public partial class App : Application
{
    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            desktop.MainWindow = new IntroWindow();
        }
        base.OnFrameworkInitializationCompleted();
    }
}
