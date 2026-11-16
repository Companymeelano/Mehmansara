using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;

namespace Mehra.Desktop.Views;

/// <summary>
/// Simple code-built input dialog (v1.2.1 write forms): title + a list of labeled text fields
/// + OK/انصراف. Returns the entered values via <see cref="Values"/> when the dialog closes with OK.
/// </summary>
public class InputDialog : Window
{
    public Dictionary<string, string> Values { get; } = new();

    public InputDialog(string title, params (string Label, string Default)[] fields)
    {
        Title = title;
        Width = 420;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var panel = new StackPanel { Margin = new Thickness(20), Spacing = 10 };
        var boxes = new List<TextBox>();
        foreach (var (label, def) in fields)
        {
            panel.Children.Add(new TextBlock { Text = label, FontWeight = Avalonia.Media.FontWeight.Bold });
            var tb = new TextBox { Text = def ?? "", HorizontalAlignment = HorizontalAlignment.Stretch };
            boxes.Add(tb);
            panel.Children.Add(tb);
        }
        var err = new TextBlock { Foreground = Avalonia.Media.Brushes.Red, IsVisible = false, TextWrapping = Avalonia.Media.TextWrapping.Wrap };
        panel.Children.Add(err);
        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, HorizontalAlignment = HorizontalAlignment.Left };
        var ok = new Button { Content = "تأیید", Classes = { "primary" } };
        var cancel = new Button { Content = "انصراف", Classes = { "ghost" } };
        ok.Click += (_, _) =>
        {
            if (Required != null && boxes.Zip(Required).Any(p => string.IsNullOrWhiteSpace(p.First.Text) && p.Second))
            {
                err.Text = "فیلدهای ستاره‌دار را پر کنید.";
                err.IsVisible = true;
                return;
            }
            for (var i = 0; i < fields.Length; i++)
                Values[fields[i].Label] = boxes[i].Text ?? "";
            Close(true);
        };
        cancel.Click += (_, _) => Close(false);
        buttons.Children.Add(ok);
        buttons.Children.Add(cancel);
        panel.Children.Add(buttons);

        Content = new Border
        {
            Background = new Avalonia.Media.SolidColorBrush(Avalonia.Media.Color.Parse("#F6EFE0")),
            Child = panel
        };
    }

    /// <summary>Which fields are mandatory (parallel to the fields array).</summary>
    public bool[]? Required { get; set; }
}
