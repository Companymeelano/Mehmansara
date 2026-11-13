package ir.ahvaz.mehra.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.export.ReportExporter
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.domain.model.DashboardSnapshot
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.ReportTable
import ir.ahvaz.mehra.domain.model.ReportType
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.DashboardRepository
import ir.ahvaz.mehra.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val from: JalaliDate = JalaliDate.today().let { JalaliDate(it.year, it.month, 1) },
    val to: JalaliDate = JalaliDate.today(),
    val selectedType: ReportType = ReportType.OCCUPANCY,
    val table: UiState<ReportTable> = UiState(),
    val canExport: Boolean = false,
    val canView: Boolean = false,
    val actionMessage: String? = null,
    val isExporting: Boolean = false,
    val generatedBy: String? = null,
    val dashboard: DashboardSnapshot? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val dashboardRepository: DashboardRepository,
    private val exporter: ReportExporter,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update {
                    it.copy(
                        canView = user?.can(Permissions.REPORTS_VIEW) == true,
                        canExport = user?.can(Permissions.REPORTS_EXPORT) == true,
                        generatedBy = user?.fullName,
                    )
                }
            }
        }
        load()
        viewModelScope.launch {
            when (val result = dashboardRepository.load()) {
                is AppResult.Success -> _state.update { it.copy(dashboard = result.data.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun onTypeChange(type: ReportType) {
        _state.update { it.copy(selectedType = type, table = UiState()) }
        load()
    }

    fun onRangeChange(from: JalaliDate, to: JalaliDate) {
        if (to < from) {
            _state.update { it.copy(actionMessage = "بازه تاریخ نامعتبر است.") }
            return
        }
        _state.update { it.copy(from = from, to = to) }
        load()
    }

    fun load() {
        val current = _state.value
        _state.update {
            it.copy(
                table = it.table.copy(
                    isLoading = it.table.data == null,
                    isRefreshing = it.table.data != null,
                )
            )
        }
        viewModelScope.launch {
            val result = reportRepository.load(
                type = current.selectedType,
                from = current.from.toGregorian(),
                to = current.to.toGregorian(),
            )
            _state.update { it.copy(table = it.table.applyValue(result)) }
        }
    }

    fun exportPdf() {
        val table = _state.value.table.data ?: return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            runCatching {
                val file = exporter.exportPdf(table, _state.value.generatedBy)
                exporter.share(file, ReportExporter.MIME_PDF, table.title)
            }.onFailure { error ->
                _state.update { it.copy(actionMessage = "ساخت فایل PDF ناموفق بود: ${error.message}") }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    fun exportXlsx() {
        val table = _state.value.table.data ?: return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            runCatching {
                val file = exporter.exportXlsx(table)
                exporter.share(file, ReportExporter.MIME_XLSX, table.title)
            }.onFailure { error ->
                _state.update { it.copy(actionMessage = "ساخت فایل اکسل ناموفق بود: ${error.message}") }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    fun exportDocx() = exportWith { table -> exporter.exportDocx(table) to ReportExporter.MIME_DOCX }

    fun exportPng() = exportWith { table -> exporter.exportPng(table) to ReportExporter.MIME_PNG }

    fun exportRawCsv() = exportWith { table -> exporter.exportRawCsv(table) to ReportExporter.MIME_CSV }

    /** PDF straight to the Bluetooth share flow. */
    fun exportBluetooth() {
        val table = _state.value.table.data ?: return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            runCatching {
                val file = exporter.exportPdf(table, _state.value.generatedBy)
                exporter.shareViaBluetooth(file, ReportExporter.MIME_PDF, table.title)
            }.onFailure { error ->
                _state.update { it.copy(actionMessage = "ارسال بلوتوثی ناموفق بود: ${error.message}") }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    private fun exportWith(block: (ReportTable) -> Pair<java.io.File, String>) {
        val table = _state.value.table.data ?: return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            runCatching {
                val (file, mime) = block(table)
                exporter.share(file, mime, table.title)
            }.onFailure { error ->
                _state.update { it.copy(actionMessage = "ساخت فایل ناموفق بود: ${error.message}") }
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
