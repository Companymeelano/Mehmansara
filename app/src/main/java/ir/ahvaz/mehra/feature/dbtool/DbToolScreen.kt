package ir.ahvaz.mehra.feature.dbtool

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.data.DbGrid
import ir.ahvaz.mehra.core.data.DbTable
import ir.ahvaz.mehra.core.data.DirectDb
import ir.ahvaz.mehra.core.data.DirectDbDefaults
import ir.ahvaz.mehra.core.ui.components.InlineMessage
import ir.ahvaz.mehra.core.ui.components.MehraCard
import ir.ahvaz.mehra.core.ui.components.MehraScreen
import ir.ahvaz.mehra.core.ui.components.MehraTextField
import ir.ahvaz.mehra.core.ui.components.PrimaryButton
import ir.ahvaz.mehra.core.ui.components.SectionHeader
import ir.ahvaz.mehra.core.ui.components.SecondaryButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DbToolState(
    val host: String = "",
    val port: String = DirectDbDefaults.PORT,
    val user: String = DirectDbDefaults.USER,
    val password: String = DirectDbDefaults.PASSWORD,
    val database: String = DirectDbDefaults.DATABASE,
    val customQuery: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val databases: List<String> = emptyList(),
    val selectedDb: String? = null,
    val tables: List<DbTable> = emptyList(),
    val grid: DbGrid? = null,
    val gridTitle: String? = null,
) {
    val canConnect: Boolean get() = host.isNotBlank() && port.isNotBlank() && user.isNotBlank()
}

@HiltViewModel
class DbToolViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(DbToolState())
    val state: StateFlow<DbToolState> = _state.asStateFlow()

    fun update(patch: (DbToolState) -> DbToolState) = _state.update(patch)

    fun connectAndListDatabases() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null, databases = emptyList(), tables = emptyList(), grid = null, selectedDb = null) }
        when (val r = DirectDb.listDatabases(s.host.trim(), s.port.trim(), s.user.trim(), s.password)) {
            is AppResult.Success -> {
                val dbs = r.data
                val sel = if (dbs.contains(s.database)) s.database else dbs.firstOrNull()
                _state.update { it.copy(loading = false, databases = dbs, selectedDb = sel) }
                if (sel != null) loadTables(sel)
            }
            is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error.userMessageFa) }
        }
    }

    fun selectDatabase(db: String) = viewModelScope.launch {
        _state.update { it.copy(selectedDb = db, tables = emptyList(), grid = null, error = null) }
        loadTables(db)
    }

    private fun loadTables(db: String) = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        when (val r = DirectDb.listTables(s.host.trim(), s.port.trim(), db, s.user.trim(), s.password)) {
            is AppResult.Success -> _state.update { it.copy(loading = false, tables = r.data) }
            is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error.userMessageFa) }
        }
    }

    fun openTable(table: DbTable) = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null, grid = null) }
        val sql = "SELECT TOP 200 * FROM [${table.schemaName}].[${table.name}]"
        when (val r = DirectDb.query(s.host.trim(), s.port.trim(), s.selectedDb ?: s.database, s.user.trim(), s.password, sql)) {
            is AppResult.Success -> _state.update { it.copy(loading = false, grid = r.data, gridTitle = table.fullName) }
            is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error.userMessageFa) }
        }
    }

    fun runQuery() = viewModelScope.launch {
        val s = _state.value
        val q = s.customQuery.trim()
        if (q.isEmpty()) return@launch
        _state.update { it.copy(loading = true, error = null, grid = null) }
        when (val r = DirectDb.query(s.host.trim(), s.port.trim(), s.selectedDb ?: s.database, s.user.trim(), s.password, q)) {
            is AppResult.Success -> _state.update { it.copy(loading = false, grid = r.data, gridTitle = "نتیجهٔ query") }
            is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error.userMessageFa) }
        }
    }
}

/**
 * Personal test screen: direct connection to SQL Server over the LAN/WiFi on port 1433
 * (no app login, no security layer — raw TDS). Shows databases, tables and query results.
 * Defaults are pre-filled from [DirectDbDefaults] for the owner's test server.
 */
@Composable
fun DbToolScreen(
    onBack: () -> Unit,
    viewModel: DbToolViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MehraScreen(
        title = "دیتابیس — اتصال مستقیم",
        subtitle = "حالت تست شخصی • WiFi/LAN • پورت ۱۴۳۳",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InlineMessage(
                    text = "این بخش فقط برای تست شخصی روی شبکهٔ داخلی (WiFi) است و هیچ لایهٔ امنیتی ندارد؛ مقادیر پیش‌فرض (adminan / St@R2022$ / پورت 1433) داخل فایل برنامه قابل استخراج هستند.",
                    isError = true,
                )
            }
            item {
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    MehraTextField(
                        label = "آدرس سرور (IP روی WiFi)",
                        value = state.host,
                        onValueChange = { v -> viewModel.update { it.copy(host = v) } },
                        placeholder = "192.168.1.20",
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MehraTextField(
                            label = "پورت",
                            value = state.port,
                            onValueChange = { v -> viewModel.update { it.copy(port = v) } },
                            numeric = true,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        MehraTextField(
                            label = "نام پایگاه داده",
                            value = state.database,
                            onValueChange = { v -> viewModel.update { it.copy(database = v) } },
                            modifier = Modifier.weight(1.4f),
                            singleLine = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    MehraTextField(
                        label = "نام کاربری SQL",
                        value = state.user,
                        onValueChange = { v -> viewModel.update { it.copy(user = v) } },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MehraTextField(
                        label = "رمز عبور SQL",
                        value = state.password,
                        onValueChange = { v -> viewModel.update { it.copy(password = v) } },
                        password = true,
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(
                        text = if (state.loading) "در حال اتصال…" else "تست اتصال و نمایش دیتابیس‌ها",
                        onClick = viewModel::connectAndListDatabases,
                        enabled = state.canConnect && !state.loading,
                        loading = state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.error != null) {
                item { InlineMessage(text = state.error!!, isError = true) }
            }
            if (state.databases.isNotEmpty()) {
                item { SectionHeader(title = "دیتابیس‌های سرور (${state.databases.size})") }
                items(state.databases, key = { it }) { db ->
                    MehraCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.selectDatabase(db) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = db + if (db == state.selectedDb) "  ●" else "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (db == state.selectedDb) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            if (db == state.selectedDb) {
                                Text("انتخاب‌شده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            if (state.tables.isNotEmpty()) {
                item { SectionHeader(title = "جدول‌های ${state.selectedDb} (${state.tables.size})") }
                items(state.tables, key = { it.fullName }) { t ->
                    MehraCard(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.openTable(t) }) {
                        Text(text = t.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item {
                MehraCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "query دلخواه (فقط SELECT)")
                    MehraTextField(
                        label = "Query",
                        value = state.customQuery,
                        onValueChange = { v -> viewModel.update { it.copy(customQuery = v) } },
                        placeholder = "SELECT TOP 50 * FROM dbo.guests",
                        singleLine = false,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        text = "اجرای query",
                        onClick = viewModel::runQuery,
                        enabled = state.customQuery.isNotBlank() && !state.loading,
                    )
                }
            }
            if (state.grid != null) {
                item { SectionHeader(title = "نتیجه: ${state.gridTitle} — ${state.grid!!.rows.size} ردیف") }
                item {
                    MehraCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Row {
                                state.grid!!.columns.forEach { c ->
                                    Text(
                                        text = c,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(140.dp),
                                    )
                                }
                            }
                            state.grid!!.rows.forEach { row ->
                                Row {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell ?: "NULL",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.width(140.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
