package ir.ahvaz.mehra.feature.finance

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ahvaz.mehra.core.common.formatMoneyCompactFa
import ir.ahvaz.mehra.core.common.formatMoneyFa
import ir.ahvaz.mehra.core.common.toPersianDigits
import ir.ahvaz.mehra.core.date.formatJalaliDateTimeFa
import ir.ahvaz.mehra.core.date.formatJalaliLongFa
import ir.ahvaz.mehra.core.date.formatJalaliShortFa
import ir.ahvaz.mehra.core.ui.components.AmountField
import ir.ahvaz.mehra.core.ui.components.BarSeriesChart
import ir.ahvaz.mehra.core.ui.components.CompactButton
import ir.ahvaz.mehra.core.ui.components.DropdownField
import ir.ahvaz.mehra.core.ui.components.EmptyState
import ir.ahvaz.mehra.core.ui.components.ErrorState
import ir.ahvaz.mehra.core.ui.components.FilterChipRow
import ir.ahvaz.mehra.core.ui.components.FreshnessRow
import ir.ahvaz.mehra.core.ui.components.InfoRow
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.JalaliDateField
import ir.ahvaz.mehra.core.ui.components.LoadingState
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraSearchField
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.StatTile
import ir.ahvaz.mehra.core.ui.components.StatusPill
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.tint
import ir.ahvaz.mehra.domain.model.PaymentMethod
import ir.ahvaz.mehra.domain.model.Transaction
import ir.ahvaz.mehra.domain.model.TransactionType

@Composable
fun FinanceScreen(viewModel: FinanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var showNewTransaction by remember { mutableStateOf(false) }
    var showDiscount by remember { mutableStateOf(false) }

    MehraScreen(
        title = "مالی",
        subtitle = "صندوق، تراکنش‌ها و بدهکاران",
        offline = state.transactions.fromCache || !state.isOnline,
        pendingCount = state.pendingOperations,
        actions = {
            if (state.canOperate) {
                IconButton(onClick = { showDiscount = true }) {
                    Icon(Icons.Rounded.Percent, contentDescription = "ثبت تخفیف")
                }
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            if (state.canOperate) {
                FloatingActionButton(onClick = { showNewTransaction = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "تراکنش جدید")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilterChipRow(
                    items = listOf(0, 1, 2),
                    selected = tab,
                    onSelect = { tab = it ?: 0 },
                    label = { when (it) { 0 -> "تراکنش‌ها"; 1 -> "صندوق روز"; else -> "بدهکاران" } },
                    allLabel = null,
                )
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
                if (state.actionMessage != null) {
                    InlineMessage(text = state.actionMessage!!, isError = false)
                }
            }

            when (tab) {
                0 -> TransactionsTab(state = state, viewModel = viewModel)
                1 -> CashBoxTab(state = state, viewModel = viewModel)
                else -> DebtorsTab(state = state)
            }
        }
    }

    if (showNewTransaction) {
        NewTransactionDialog(
            isSaving = state.isRegistering,
            onDismiss = { showNewTransaction = false },
            onConfirm = { type, amount, method, description, guest, reference ->
                viewModel.registerTransaction(type, amount, method, description, guest, reference)
                showNewTransaction = false
            },
        )
    }

    if (showDiscount) {
        DiscountDialog(
            onDismiss = { showDiscount = false },
            onConfirm = { amount, reason, guestId ->
                viewModel.applyDiscount(amount, reason, guestId)
                showDiscount = false
            },
        )
    }
}

@Composable
private fun TransactionsTab(state: FinanceUiState, viewModel: FinanceViewModel) {
    val transactions = state.transactions
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            FilterChipRow(
                items = TransactionType.entries.toList(),
                selected = state.typeFilter,
                onSelect = viewModel::onTypeFilterChange,
                label = { it.labelFa },
            )
            MehraSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "جست‌وجو در شرح، کد یا نام مهمان…",
            )
            FreshnessRow(
                updatedAtMillis = transactions.updatedAtMillis,
                isRefreshing = transactions.isRefreshing,
                onRefresh = viewModel::refresh,
            )
        }
        when {
            transactions.isLoading && transactions.data == null -> LoadingState()
            transactions.error != null && transactions.data == null ->
                ErrorState(transactions.error!!, viewModel::refresh)

            transactions.data.isNullOrEmpty() -> EmptyState(
                title = "تراکنشی ثبت نشده است",
                message = "برای این بازه تاریخی تراکنشی وجود ندارد.",
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(transactions.data.orEmpty(), key = { it.id }) { transaction ->
                    TransactionRow(transaction)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    MehraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = listOfNotNull(
                        transaction.guestName,
                        transaction.roomNumber?.let { "اتاق $it" },
                        transaction.method.labelFa,
                        transaction.operatorName,
                        transaction.createdAt.formatJalaliDateTimeFa(),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "کد ${transaction.code}" + (transaction.reference?.let { " • مرجع: $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.type == TransactionType.RECEIVE) "+" else "−") +
                        formatMoneyFa(transaction.amountRial),
                    style = MaterialTheme.typography.titleSmall,
                    color = transaction.type.tint(),
                )
                StatusPill(text = transaction.type.labelFa, tint = transaction.type.tint())
            }
        }
    }
}

@Composable
private fun CashBoxTab(state: FinanceUiState, viewModel: FinanceViewModel) {
    val cashBox = state.cashBox
    val summary = state.summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        when {
            cashBox.isLoading && cashBox.data == null -> LoadingState()
            cashBox.error != null && cashBox.data == null -> ErrorState(cashBox.error!!, viewModel::refresh)
            cashBox.data == null -> EmptyState(title = "اطلاعات صندوق در دسترس نیست")
            else -> {
                val box = cashBox.data!!
                Text(
                    text = "صندوق روز ${box.date.formatJalaliLongFa()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "دریافتی",
                        value = formatMoneyCompactFa(box.receivedRial),
                        tone = MehraTheme.colors.success,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "پرداختی",
                        value = formatMoneyCompactFa(box.paidRial),
                        tone = MehraTheme.colors.danger,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "تخفیف",
                        value = formatMoneyCompactFa(box.discountsRial),
                        tone = MehraTheme.colors.warning,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "خالص",
                        value = formatMoneyCompactFa(box.netRial),
                        tone = MehraTheme.colors.gold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                StatTile(
                    label = "مانده باز",
                    value = formatMoneyFa(box.openBalanceRial),
                    tone = MehraTheme.colors.info,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (box.byMethod.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(title = "تفکیک روش پرداخت")
                    box.byMethod.forEach { total ->
                        MehraCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(total.method.labelFa, modifier = Modifier.weight(1f))
                                Text(
                                    text = "${formatMoneyFa(total.amountRial)} • ${(total.count.toString()).toPersianDigits()} تراکنش",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        SectionHeader(title = "روند دریافت و پرداخت")
        val series = summary.data?.dailySeries.orEmpty()
        if (series.isEmpty()) {
            Text(
                text = "برای نمایش نمودار، بازه تاریخی را انتخاب کنید.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BarSeriesChart(
                values = series.map { it.receivedRial.toFloat() },
                labels = series.map { it.date.formatJalaliShortFa() },
                barColor = MehraTheme.colors.chartPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            summary.data?.let { data ->
                InfoRow("جمع دریافتی", formatMoneyFa(data.receivedRial))
                InfoRow("جمع پرداختی", formatMoneyFa(data.paidRial))
                InfoRow("جمع تخفیف", formatMoneyFa(data.discountsRial))
                InfoRow("خالص", formatMoneyFa(data.netRial))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DebtorsTab(state: FinanceUiState) {
    if (state.debtors.isEmpty()) {
        EmptyState(
            title = "بدهکاری ثبت نشده است",
            message = "در حال حاضر مهمانی با مانده بدهی وجود ندارد.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.debtors, key = { it.guestId }) { debtor ->
            MehraCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(debtor.guestName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = listOfNotNull(
                                debtor.phone,
                                debtor.roomNumber?.let { "اتاق $it" },
                                debtor.lastActivityAt.formatJalaliDateTimeFa(),
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = formatMoneyFa(debtor.balanceRial),
                        style = MaterialTheme.typography.titleSmall,
                        color = MehraTheme.colors.danger,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewTransactionDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, String, PaymentMethod, String, String?, String?) -> Unit,
) {
    var type by remember { mutableStateOf(TransactionType.RECEIVE) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var description by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت تراکنش مالی") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DropdownField(
                    label = "نوع تراکنش",
                    options = TransactionType.entries.toList(),
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it.labelFa },
                )
                Spacer(modifier = Modifier.height(10.dp))
                AmountField(label = "مبلغ (ریال)", value = amount, onValueChange = { amount = it })
                Spacer(modifier = Modifier.height(10.dp))
                DropdownField(
                    label = "روش پرداخت",
                    options = PaymentMethod.entries.toList(),
                    selected = method,
                    onSelect = { method = it },
                    optionLabel = { it.labelFa },
                )
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "شرح",
                    value = description,
                    onValueChange = { description = it },
                    singleLine = false,
                )
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(label = "نام مهمان (اختیاری)", value = guestName, onValueChange = { guestName = it })
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(label = "شماره مرجع (اختیاری)", value = reference, onValueChange = { reference = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onConfirm(type, amount, method, description, guestName, reference) },
            ) { Text(if (isSaving) "در حال ثبت…" else "ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun DiscountDialog(onDismiss: () -> Unit, onConfirm: (String, String, String?) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var guestId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت تخفیف") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AmountField(label = "مبلغ تخفیف (ریال)", value = amount, onValueChange = { amount = it })
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "دلیل تخفیف",
                    value = reason,
                    onValueChange = { reason = it },
                    singleLine = false,
                )
                Spacer(modifier = Modifier.height(10.dp))
                MehraTextField(
                    label = "شناسه مهمان (اختیاری)",
                    value = guestId,
                    onValueChange = { guestId = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount, reason, guestId) }) { Text("ثبت تخفیف") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
