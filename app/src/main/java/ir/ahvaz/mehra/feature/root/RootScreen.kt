package ir.ahvaz.mehra.feature.root

import androidx.compose.runtime.Composable
import ir.ahvaz.mehra.core.security.LocalOperator
import ir.ahvaz.mehra.navigation.MehraNavGraph

/**
 * Single decision point of the app.
 *
 * Since 1.4.0 the app is fully local: there is no server configuration and no login screen.
 * After install it opens straight into the main navigation graph; a local operator session
 * is created silently by [RootViewModel]. Only the optional app-lock (PIN) screen remains.
 */
@Composable
fun RootScreen(
    state: RootUiState,
    viewModel: RootViewModel,
) {
    when {
        state.pinSet && state.locked -> {
            LockScreen(
                userName = (state.session ?: LocalOperator.session()).fullName,
                onUnlock = viewModel::tryUnlock,
                onLogout = { viewModel.logout() },
            )
        }

        else -> {
            MehraNavGraph(
                user = state.session ?: LocalOperator.session(),
                onSessionInvalid = { viewModel.logout() },
            )
        }
    }
}
