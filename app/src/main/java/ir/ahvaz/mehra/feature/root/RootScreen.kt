package ir.ahvaz.mehra.feature.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.ahvaz.mehra.feature.auth.LoginScreen
import ir.ahvaz.mehra.feature.auth.ServerSetupScreen
import ir.ahvaz.mehra.navigation.MehraNavGraph

/**
 * Single decision point of the app: server configuration, then authentication, then the
 * local lock screen, then the authenticated navigation graph.
 */
@Composable
fun RootScreen(
    state: RootUiState,
    viewModel: RootViewModel,
) {
    var forceServerSetup by remember { mutableStateOf(false) }

    when {
        !state.serverConfigured || forceServerSetup -> {
            ServerSetupScreen(onSaved = { forceServerSetup = false })
        }

        state.session == null -> {
            LoginScreen(
                onLoggedIn = {
                    viewModel.dismissSessionExpired()
                    viewModel.onAuthenticationSucceeded()
                },
                onOpenServerSettings = { forceServerSetup = true },
                sessionExpired = state.sessionExpired,
            )
        }

        state.pinSet && state.locked -> {
            LockScreen(
                userName = state.session.fullName,
                onUnlock = viewModel::tryUnlock,
                onLogout = { viewModel.logout() },
            )
        }

        else -> {
            MehraNavGraph(
                user = state.session,
                onSessionInvalid = { viewModel.logout() },
            )
        }
    }
}
