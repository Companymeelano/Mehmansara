using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;

namespace Mehra.Desktop.Views;

/// <summary>
/// Simple Persian confirmation dialog (v1.2.4) for delete / reverse / data-reset actions.
/// </summary>
public class ConfirmDialog : Window
{
    public ConfirmDialog(string title, string message, string confirmText = "حذف")
    {
        Title = title;
        Width = 400;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var panel = new StackPanel { Margin = new Thickness(22), Spacing = 14 };
        panel.Children.Add(new TextBlock
        {
            Text = title,
            FontSize = 16,
            Classes = { "bold" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D"))
        });
        panel.Children.Add(new TextBlock
        {
            Text = message,
            TextWrapping = TextWrapping.Wrap,
            Foreground = new SolidColorBrush(Color.Parse("#3A4556")),
            FontSize = 13
        });

        var buttons = new StackPanel
        {
            Orientation = Orientation.Horizontal,
            Spacing = 10,
            HorizontalAlignment = HorizontalAlignment.Left
        };
        var ok = new Button
        {
            Content = confirmText,
            Background = new SolidColorBrush(Color.Parse("#B3261E")),
            Foreground = Brushes.White
        };
        var cancel = new Button
        {
            Content = "انصراف",
            Classes = { "ghost" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D")),
            BorderBrush = new SolidColorBrush(Color.Parse("#D8CBA6"))
        };
        ok.Click += (_, _) => Close(true);
        cancel.Click += (_, _) => Close(false);
        buttons.Children.Add(ok);
        buttons.Children.Add(cancel);
        panel.Children.Add(buttons);

        Content = new Border
        {
            Background = new SolidColorBrush(Color.Parse("#F7F3E9")),
            BorderBrush = new SolidColorBrush(Color.Parse("#C9A24B")),
            BorderThickness = new Thickness(1),
            Child = panel
        };
    }
}
