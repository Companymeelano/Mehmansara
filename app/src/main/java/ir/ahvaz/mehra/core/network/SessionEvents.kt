package ir.ahvaz.mehra.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionEvent {
    /** Refresh token is gone — the user must sign in again. */
    data object Expired : SessionEvent
    data object LoggedOut : SessionEvent
    data class Refreshed(val accessTokenExpiresAtMillis: Long?) : SessionEvent
}

/**
 * Broadcasts session lifecycle changes coming from the network layer (which cannot show UI
 * itself) so MainActivity / AuthRepository can react exactly once.
 */
@Singleton
class SessionEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8, replay = 0)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun emit(event: SessionEvent) {
        _events.tryEmit(event)
    }
}
