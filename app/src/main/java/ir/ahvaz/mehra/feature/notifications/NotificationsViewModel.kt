package ir.ahvaz.mehra.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.ahvaz.mehra.core.common.AppResult
import ir.ahvaz.mehra.core.ui.state.UiState
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val items: UiState<List<NotificationItem>> = UiState(),
    val unreadOnly: Boolean = false,
    val unreadCount: Int = 0,
    val actionMessage: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.observeUnreadCount().collect { count ->
                _state.update { it.copy(unreadCount = count) }
            }
        }
        refresh()
    }

    fun setUnreadOnly(value: Boolean) {
        _state.update { it.copy(unreadOnly = value) }
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(items = it.items.copy(isLoading = it.items.data == null, isRefreshing = it.items.data != null))
        }
        viewModelScope.launch {
            val result = notificationRepository.notifications(_state.value.unreadOnly)
            _state.update { it.copy(items = it.items.applyResult(result)) }
        }
    }

    fun markRead(item: NotificationItem) {
        if (item.isRead) return
        viewModelScope.launch {
            when (val result = notificationRepository.markRead(item.id)) {
                is AppResult.Success -> refresh()
                is AppResult.Failure -> _state.update { it.copy(actionMessage = result.error.userMessageFa) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (val result = notificationRepository.markAllRead()) {
                is AppResult.Success -> {
                    _state.update { it.copy(actionMessage = "همه اعلان‌ها خوانده شد.") }
                    refresh()
                }

                is AppResult.Failure -> _state.update { it.copy(actionMessage = result.error.userMessageFa) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
