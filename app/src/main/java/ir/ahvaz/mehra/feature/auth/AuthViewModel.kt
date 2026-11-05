package ir.ahvaz.mehra.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.Validators
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import ir.ahvaz.mehra.domain.repository.AuthRepository
import ir.ahvaz.mehra.domain.repository.ServerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConnectionProbe {
    data object Idle : ConnectionProbe
    data object Testing : ConnectionProbe
    data class Success(val info: ServerInfo) : ConnectionProbe
    data class Failure(val message: String) : ConnectionProbe
}

data class AuthUiState(
    val serverUrl: String = "",
    val savedServerUrl: String = "",
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val probe: ConnectionProbe = ConnectionProbe.Idle,
    val loggedIn: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverUrlProvider: ServerUrlProvider,
    sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState(
            serverUrl = serverUrlProvider.current(),
            savedServerUrl = serverUrlProvider.current(),
            loggedIn = sessionManager.isLoggedIn,
        )
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onServerUrlChange(value: String) {
        _state.update { it.copy(serverUrl = value, probe = ConnectionProbe.Idle, errorMessage = null) }
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, fieldErrors = it.fieldErrors - "username", errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, fieldErrors = it.fieldErrors - "password", errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    /** Probes the address the operator typed *without* saving it. */
    fun testConnection() {
        val candidate = _state.value.serverUrl
        if (candidate.isBlank()) {
            _state.update { it.copy(errorMessage = "آدرس سرور را وارد کنید.") }
            return
        }
        _state.update { it.copy(probe = ConnectionProbe.Testing, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.serverInfo(candidate)) {
                is ir.ahvaz.mehra.core.common.AppResult.Success ->
                    _state.update { it.copy(probe = ConnectionProbe.Success(result.data)) }

                is ir.ahvaz.mehra.core.common.AppResult.Failure ->
                    _state.update { it.copy(probe = ConnectionProbe.Failure(result.error.userMessageFa)) }
            }
        }
    }

    /** Persists the address and reports a Persian error when it is not usable. */
    fun saveServerUrl(onSaved: () -> Unit) {
        val error = serverUrlProvider.save(_state.value.serverUrl)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        val normalized = serverUrlProvider.current()
        _state.update { it.copy(savedServerUrl = normalized, serverUrl = normalized, errorMessage = null) }
        onSaved()
    }

    fun login(onSuccess: () -> Unit) {
        val current = _state.value
        val fields = mutableMapOf<String, String>()
        if (current.username.isBlank()) fields["username"] = "نام کاربری الزامی است."
        if (current.password.isBlank()) fields["password"] = "رمز عبور الزامی است."
        if (fields.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = fields, errorMessage = "اطلاعات ورود را کامل کنید.") }
            return
        }
        if (current.savedServerUrl.isBlank()) {
            _state.update { it.copy(errorMessage = "ابتدا آدرس سرور را ذخیره کنید.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = authRepository.login(current.username, current.password)) {
                is ir.ahvaz.mehra.core.common.AppResult.Success ->
                    _state.update { it.copy(isLoading = false, loggedIn = true, password = "") }
                        .also { onSuccess() }

                is ir.ahvaz.mehra.core.common.AppResult.Failure ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.userMessageFa,
                            fieldErrors = result.error.fieldErrors,
                        )
                    }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onResult: (String?) -> Unit) {
        val passwordError = Validators.passwordError(newPassword)
        if (passwordError != null) {
            onResult(passwordError)
            return
        }
        viewModelScope.launch {
            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                is ir.ahvaz.mehra.core.common.AppResult.Success -> onResult(null)
                is ir.ahvaz.mehra.core.common.AppResult.Failure -> onResult(result.error.userMessageFa)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
