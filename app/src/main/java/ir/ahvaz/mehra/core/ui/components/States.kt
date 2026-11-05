package ir.ahvaz.mehra.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.ahvaz.mehra.core.common.AppError
import ir.ahvaz.mehra.core.common.ErrorKind
import ir.ahvaz.mehra.core.date.formatEpochDateTimeFa
import ir.ahvaz.mehra.core.ui.theme.MehraTheme

@Composable
fun LoadingState(message: String = "در حال دریافت اطلاعات…", modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorState(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MehraTheme.colors
    val (icon, tint) = when (error.kind) {
        ErrorKind.FORBIDDEN -> Icons.Rounded.Lock to colors.warning
        ErrorKind.NETWORK, ErrorKind.TIMEOUT -> Icons.Rounded.CloudOff to colors.warning
        else -> Icons.Rounded.ErrorOutline to colors.danger
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = error.userMessageFa,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!error.code.isNullOrBlank() || error.traceId != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(error.code, error.traceId?.let { "کد پیگیری: $it" }).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (onRetry != null && error.isRetryable) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تلاش دوباره")
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String? = null,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun PermissionDeniedState(message: String = "برای مشاهده این بخش دسترسی ندارید.") {
    EmptyState(
        title = "دسترسی محدود",
        message = message,
        icon = Icons.Rounded.Lock,
    )
}

/** Amber/red inline banner used for action failures that should not replace the screen. */
@Composable
fun InlineMessage(
    text: String,
    isError: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MehraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Inbox,
            contentDescription = null,
            tint = if (isError) colors.danger else colors.info,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) colors.danger else colors.info,
        )
    }
}

/** "آخرین به‌روزرسانی ۱۴:۳۲" + refresh action. */
@Composable
fun FreshnessRow(
    updatedAtMillis: Long?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (updatedAtMillis == null) "به‌روزرسانی نشده" else "آخرین به‌روزرسانی: ${updatedAtMillis.formatEpochDateTimeFa()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isRefreshing) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "به‌روزرسانی",
                    modifier = Modifier
                        .size(18.dp)
                        .clickableIcon(onRefresh),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun Modifier.clickableIcon(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
