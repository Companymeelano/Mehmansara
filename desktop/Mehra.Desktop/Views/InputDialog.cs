using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Layout;
using Avalonia.Media;

namespace Mehra.Desktop.Views;

/// <summary>
/// Responsive code-built input dialog (v1.2.4): 1-column for <= 6 fields, 2-column grid for > 6 fields,
/// wrapped in a ScrollViewer with MaxHeight=520 and OK/Cancel pinned at the bottom so large forms
/// (like full guest registration) never overflow the screen.
/// </summary>
public class InputDialog : Window
{
    public Dictionary<string, string> Values { get; } = new();

    public InputDialog(string title, params (string Label, string Default)[] fields)
    {
        Title = title;
        Width = fields.Length > 6 ? 740 : 440;
        SizeToContent = SizeToContent.Height;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        CanResize = false;

        var root = new StackPanel { Margin = new Thickness(20), Spacing = 14 };
        root.Children.Add(new TextBlock
        {
            Text = title,
            FontSize = 16,
            Classes = { "bold" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D"))
        });

        var boxes = new List<TextBox>();
        Control fieldsContainer;
        if (fields.Length > 6)
        {
            var grid = new UniformGrid { Columns = 2 };
            foreach (var (label, def) in fields)
            {
                var cell = new StackPanel { Margin = new Thickness(6, 4), Spacing = 4 };
                cell.Children.Add(new TextBlock { Text = label, FontWeight = FontWeight.Bold, FontSize = 12 });
                var tb = new TextBox { Text = def ?? "", HorizontalAlignment = HorizontalAlignment.Stretch };
                boxes.Add(tb);
                cell.Children.Add(tb);
                grid.Children.Add(cell);
            }
            fieldsContainer = grid;
        }
        else
        {
            var stack = new StackPanel { Spacing = 8 };
            foreach (var (label, def) in fields)
            {
                stack.Children.Add(new TextBlock { Text = label, FontWeight = FontWeight.Bold, FontSize = 12.5 });
                var tb = new TextBox { Text = def ?? "", HorizontalAlignment = HorizontalAlignment.Stretch };
                boxes.Add(tb);
                stack.Children.Add(tb);
            }
            fieldsContainer = stack;
        }

        root.Children.Add(new ScrollViewer
        {
            MaxHeight = 520,
            Content = fieldsContainer
        });

        var err = new TextBlock { Foreground = Brushes.Red, IsVisible = false, TextWrapping = TextWrapping.Wrap };
        root.Children.Add(err);

        var buttons = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, HorizontalAlignment = HorizontalAlignment.Left };
        var ok = new Button { Content = "تأیید", Classes = { "primary" } };
        var cancel = new Button
        {
            Content = "انصراف",
            Classes = { "ghost" },
            Foreground = new SolidColorBrush(Color.Parse("#16233D")),
            BorderBrush = new SolidColorBrush(Color.Parse("#D8CBA6"))
        };
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
        root.Children.Add(buttons);

        Content = new Border
        {
            Background = new SolidColorBrush(Color.Parse("#F7F3E9")),
            BorderBrush = new SolidColorBrush(Color.Parse("#C9A24B")),
            BorderThickness = new Thickness(1),
            Child = root
        };
    }

    /// <summary>Which fields are mandatory (parallel to the fields array).</summary>
    public bool[]? Required { get; set; }
}
