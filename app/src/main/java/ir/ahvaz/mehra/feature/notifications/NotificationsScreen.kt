package ir.ahvaz.mehra.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.date.relativeTimeFa
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.components.SwitchRow
import ir.ahvaz.mehra.core.ui.theme.tint

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = state.items

    MehraScreen(
        title = "اعلان‌ها",
        subtitle = if (state.unreadCount > 0) "${state.unreadCount} اعلان خوانده‌نشده" else "همه اعلان‌ها خوانده شده‌اند",
        offline = items.fromCache,
        actions = {
            IconButton(onClick = viewModel::markAllRead) {
                Icon(Icons.Rounded.DoneAll, contentDescription = "خواندن همه")
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    title = "فقط خوانده‌نشده‌ها",
                    checked = state.unreadOnly,
                    onCheckedChange = viewModel::setUnreadOnly,
                )
                FreshnessRow(
                    updatedAtMillis = items.updatedAtMillis,
                    isRefreshing = items.isRefreshing,
                    onRefresh = viewModel::refresh,
                )
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                }
            }
            when {
                items.isLoading && items.data == null -> LoadingState()
                items.error != null && items.data == null -> ErrorState(items.error!!, viewModel::refresh)
                items.data.isNullOrEmpty() -> EmptyState(
                    title = "اعلانی وجود ندارد",
                    message = "اعلان‌های سرور و رویدادهای زنده در این بخش نمایش داده می‌شوند.",
                    icon = Icons.Rounded.NotificationsNone,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items.data.orEmpty(), key = { it.id }) { item ->
                        MehraCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.markRead(item) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                    )
                                    item.body?.let { body ->
                                        Text(
                                            text = body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = relativeTimeFa(item.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                StatusPill(text = item.severity.labelFa, tint = item.severity.tint())
                            }
                        }
                    }
                }
            }
        }
    }
}
