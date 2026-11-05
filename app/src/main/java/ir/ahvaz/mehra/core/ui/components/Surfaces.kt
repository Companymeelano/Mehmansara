package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.core.ui.icons.MehraIcons
import ir.ahvaz.mehra.core.ui.theme.MehraTheme

/**
 * Standard screen shell: a gradient top bar with the 3D brand chip, an optional status strip
 * for offline / queued operations, and the content slot. The scaffold itself is transparent
 * so the theme's global backdrop gradient (painted in [ir.ahvaz.mehra.core.ui.theme.MehraTheme])
 * shows through every screen.
 */
@Composable
fun MehraScreen(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    offline: Boolean = false,
    pendingCount: Int = 0,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MehraTopBar(title = title, subtitle = subtitle, onBack = onBack, actions = actions)
        },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (offline || pendingCount > 0) {
                SyncStatusStrip(offline = offline, pendingCount = pendingCount)
            }
            content(padding)
        }
    }
}

/**
 * Luxury top bar: horizontal gradient in the preset's colours, the 3D logo chip on root
 * screens, a back arrow on sub-screens, and a thin accent hairline along the bottom edge.
 */
@Composable
fun MehraTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MehraTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(colors.topBarStart, colors.topBarEnd))),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    CompositionLocalProvider(LocalContentColor provides colors.topBarText) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "بازگشت",
                            )
                        }
                    }
                } else {
                    Icon3D(
                        icon = MehraIcons.Logo,
                        contentDescription = null,
                        size = 34.dp,
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.topBarText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.topBarText.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CompositionLocalProvider(LocalContentColor provides colors.topBarText) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.accentStart.copy(alpha = 0.95f),
                                colors.accentMid.copy(alpha = 0.25f),
                                colors.accentEnd.copy(alpha = 0.95f),
                            )
                        )
                    )
            )
        }
    }
}

/** Amber strip shown whenever data is stale or an operation waits for server confirmation. */
@Composable
fun SyncStatusStrip(offline: Boolean, pendingCount: Int) {
    val colors = MehraTheme.colors
    val background = if (offline) colors.warningSoft else colors.infoSoft
    val foreground = if (offline) colors.warning else colors.info
    val message = when {
        offline && pendingCount > 0 -> "حالت آفلاین — $pendingCount عملیات در انتظار ارسال به سرور"
        offline -> "حالت آفلاین — نمایش آخرین اطلاعات ذخیره‌شده"
        else -> "$pendingCount عملیات در انتظار تأیید سرور"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = message, style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}

/**
 * A luxury glass card: translucent surface over the theme backdrop, a 1px gradient border in
 * the accent colours, soft drop shadow and a generous 20dp radius.
 */
@Composable
fun MehraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScopeAlias.() -> Unit,
) {
    val colors = MehraTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val base = modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
    Surface(
        modifier = base.shadow(8.dp, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (colors.isDark) 0.82f else 0.90f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    colors.accentStart.copy(alpha = 0.55f),
                    colors.cardBorder,
                    colors.accentEnd.copy(alpha = 0.55f),
                )
            ),
        ),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Alias so callers can use `MehraCard { }` without importing ColumnScope explicitly. */
typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalContentColor.current,
    valueWeight: FontWeight = FontWeight.Medium,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueWeight,
            color = valueColor,
            modifier = Modifier.weight(1.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    soft: Boolean = true,
) {
    val dark = MehraTheme.colors.isDark
    val background = if (soft) tint.copy(alpha = if (dark) 0.22f else 0.12f) else tint
    val foreground = if (soft) tint else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = background,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun IconLabel(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
