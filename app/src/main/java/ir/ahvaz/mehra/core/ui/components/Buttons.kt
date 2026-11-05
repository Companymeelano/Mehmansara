package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.darken
import ir.ahvaz.mehra.core.ui.theme.lighten

/**
 * Buttons are rendered as 3D gradient tiles: a rounded box filled with the active theme's
 * accent gradient, a soft shadow and a ripple — the icon inside is the matching SVG-derived
 * glyph from [ir.ahvaz.mehra.core.ui.icons.MehraIcons] (or any Material icon).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = MehraTheme.colors
    val active = enabled && !loading
    val shape = RoundedCornerShape(16.dp)
    val brush = Brush.linearGradient(listOf(colors.accentStart, colors.accentMid, colors.accentEnd))
    Box(
        modifier = modifier
            .height(54.dp)
            .then(if (active) Modifier.shadow(8.dp, shape) else Modifier)
            .clip(shape)
            .background(brush)
            .alpha(if (active) 1f else 0.45f)
            .clickable(
                enabled = active,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = colors.onAccent,
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onAccent)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onAccent,
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = MehraTheme.colors
    val scheme = MaterialTheme.colorScheme
    val active = enabled && !loading
    val shape = RoundedCornerShape(16.dp)
    val brush = Brush.linearGradient(listOf(colors.accentStart, colors.accentMid, colors.accentEnd))
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .border(androidx.compose.foundation.BorderStroke(1.5.dp, brush), shape)
            .background(scheme.surface.copy(alpha = if (colors.isDark) 0.55f else 0.75f))
            .alpha(if (active) 1f else 0.45f)
            .clickable(
                enabled = active,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = scheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = scheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
            )
        }
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = MehraTheme.colors
    val danger = colors.danger
    val shape = RoundedCornerShape(16.dp)
    val brush = Brush.linearGradient(listOf(danger.lighten(0.18f), danger, danger.darken(0.22f)))
    Box(
        modifier = modifier
            .height(54.dp)
            .then(if (enabled) Modifier.shadow(8.dp, shape) else Modifier)
            .clip(shape)
            .background(brush)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
fun CompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = MehraTheme.colors
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
                shape,
            )
            .background(scheme.surface.copy(alpha = if (colors.isDark) 0.6f else 0.8f))
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.accentMid,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
            )
        }
    }
}

/** Row of primary/secondary actions pinned at the bottom of a form screen. */
@Composable
fun FormActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        content = content,
    )
}
