package ir.ahvaz.mehra.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliLongFa
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.JalaliDateField
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.ReportType

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val table = state.table

    MehraScreen(
        title = "گزارش‌ها",
        subtitle = "گزارش‌های مدیریتی با خروجی PDF و اکسل",
        offline = table.fromCache,
        actions = {
            IconButton(onClick = viewModel::load) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
        ) {
            if (state.actionMessage != null) {
                InlineMessage(text = state.actionMessage!!, isError = true)
                Spacer(modifier = Modifier.height(8.dp))
            }

            DropdownField(
                label = "نوع گزارش",
                options = ReportType.entries.toList(),
                selected = state.selectedType,
                onSelect = viewModel::onTypeChange,
                optionLabel = { it.titleFa },
            )
            Text(
                text = state.selectedType.descriptionFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 4.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JalaliDateField(
                    label = "از تاریخ",
                    value = state.from,
                    onDateSelected = { viewModel.onRangeChange(it, state.to) },
                    modifier = Modifier.weight(1f),
                )
                JalaliDateField(
                    label = "تا تاریخ",
                    value = state.to,
                    onDateSelected = { viewModel.onRangeChange(state.from, it) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FreshnessRow(
                updatedAtMillis = table.updatedAtMillis,
                isRefreshing = table.isRefreshing,
                onRefresh = viewModel::load,
            )

            when {
                table.isLoading && table.data == null -> LoadingState(message = "در حال دریافت گزارش…")
                table.error != null && table.data == null -> ErrorState(table.error!!, viewModel::load)
                table.data == null -> EmptyState(
                    title = "گزارشی بارگذاری نشده",
                    message = "نوع گزارش و بازه تاریخی را انتخاب کنید.",
                    icon = Icons.Rounded.Description,
                )

                else -> {
                    val data = table.data!!
                    Spacer(modifier = Modifier.height(12.dp))
                    MehraCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = listOfNotNull(
                                data.from?.formatJalaliLongFa(),
                                data.to?.formatJalaliLongFa(),
                            ).joinToString(" تا "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (data.isEmpty) {
                            Text(
                                text = "داده‌ای برای این بازه ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            ReportTablePreview(data)
                        }
                        if (!data.notes.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            data.notes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (state.canExport) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "خروجی و ارسال")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactButton(
                                text = if (state.isExporting) "در حال آماده‌سازی…" else "PDF و ارسال",
                                onClick = viewModel::exportPdf,
                                icon = Icons.Rounded.Description,
                                enabled = !state.isExporting && !data.isEmpty,
                                modifier = Modifier.weight(1f),
                            )
                            CompactButton(
                                text = "اکسل و ارسال",
                                onClick = viewModel::exportXlsx,
                                icon = Icons.Rounded.GridOn,
                                enabled = !state.isExporting && !data.isEmpty,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Horizontally scrollable preview of the server-rendered table. Columns keep a fixed width. */
@Composable
private fun ReportTablePreview(table: ReportTable) {
    val columnWidth = if (table.columns.size > 6) 108.dp else 132.dp
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            table.columns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(columnWidth)
                        .background(MehraTheme.colors.gold.copy(alpha = 0.10f))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = column,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
        table.rows.take(60).forEachIndexed { index, row ->
            Row(
                modifier = Modifier.background(
                    if (index % 2 == 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
            ) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                    ) {
                        Text(text = cell, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (table.rows.size > 60) {
            Text(
                text = "نمایش ۶۰ سطر از ${toPersianDigits(table.rows.size.toString())} سطر — برای فهرست کامل، خروجی PDF یا اکسل بگیرید.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        table.totals?.let { totals ->
            Row(modifier = Modifier.background(MehraTheme.colors.gold.copy(alpha = 0.14f))) {
                totals.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
