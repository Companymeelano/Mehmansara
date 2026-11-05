package ir.ahvaz.mehra.feature.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.date.JalaliDate
import ir.ahvaz.mehra.core.network.NetworkMonitor
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.domain.model.AppUser
import ir.ahvaz.mehra.domain.model.AuditLogEntry
import ir.ahvaz.mehra.domain.model.CreateUserCommand
import ir.ahvaz.mehra.domain.model.Permissions
import ir.ahvaz.mehra.domain.model.RoleInfo
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsersUiState(
    val users: UiState<List<AppUser>> = UiState(),
    val roles: List<RoleInfo> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val query: String = "",
    val isLoadingAudit: Boolean = false,
    val canManage: Boolean = false,
    val canViewAudit: Boolean = false,
    val isOnline: Boolean = true,
    val actionMessage: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    authRepository: AuthRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(UsersUiState(isOnline = networkMonitor.isCurrentlyOnline()))
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { user ->
                _state.update {
                    it.copy(
                        canManage = user?.can(Permissions.USERS_MANAGE) == true,
                        canViewAudit = user?.can(Permissions.AUDIT_VIEW) == true,
                    )
                }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _state.update { it.copy(isOnline = online) } }
        }
        refresh()
        loadRoles()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(users = it.users.copy(isLoading = it.users.data == null, isRefreshing = it.users.data != null))
        }
        viewModelScope.launch {
            val result = userRepository.users(_state.value.query)
            _state.update { it.copy(users = it.users.applyResult(result)) }
        }
    }

    private fun loadRoles() {
        viewModelScope.launch {
            when (val result = userRepository.roles()) {
                is AppResult.Success -> _state.update { it.copy(roles = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun loadAuditLogs(from: JalaliDate, to: JalaliDate) {
        _state.update { it.copy(isLoadingAudit = true) }
        viewModelScope.launch {
            val result = userRepository.auditLogs(from.toGregorian(), to.toGregorian())
            _state.update {
                when (result) {
                    is AppResult.Success -> it.copy(auditLogs = result.data, isLoadingAudit = false)
                    is AppResult.Failure -> it.copy(isLoadingAudit = false, actionMessage = result.error.userMessageFa)
                }
            }
        }
    }

    fun saveUser(
        id: String?,
        fullName: String,
        username: String,
        password: String?,
        roleCode: String,
        phone: String?,
        isActive: Boolean,
    ) {
        if (fullName.isBlank() || username.isBlank() || roleCode.isBlank()) {
            _state.update { it.copy(actionMessage = "نام، نام کاربری و نقش الزامی است.") }
            return
        }
        if (id == null && password.isNullOrBlank()) {
            _state.update { it.copy(actionMessage = "برای کاربر جدید رمز عبور الزامی است.") }
            return
        }
        _state.update { it.copy(isSaving = true, actionMessage = null) }
        viewModelScope.launch {
            val result = userRepository.save(
                CreateUserCommand(
                    id = id,
                    fullName = fullName.trim(),
                    username = username.trim(),
                    password = password?.takeIf { it.isNotBlank() },
                    roleCode = roleCode,
                    phone = phone?.takeIf { it.isNotBlank() },
                    isActive = isActive,
                )
            )
            _state.update {
                it.copy(
                    isSaving = false,
                    actionMessage = when (result) {
                        is AppResult.Success -> "اطلاعات کاربر ذخیره شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    },
                )
            }
            refresh()
        }
    }

    fun setActive(user: AppUser, active: Boolean) {
        viewModelScope.launch {
            val result = userRepository.setActive(user.id, active)
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> if (active) "کاربر فعال شد." else "کاربر غیرفعال شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
            refresh()
        }
    }

    fun resetPassword(user: AppUser, newPassword: String) {
        if (newPassword.length < 8) {
            _state.update { it.copy(actionMessage = "رمز عبور باید حداقل ۸ کاراکتر باشد.") }
            return
        }
        viewModelScope.launch {
            val result = userRepository.resetPassword(user.id, newPassword)
            _state.update {
                it.copy(
                    actionMessage = when (result) {
                        is AppResult.Success -> "رمز عبور کاربر بازنشانی شد."
                        is AppResult.Failure -> result.error.userMessageFa
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
