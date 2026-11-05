package ir.ahvaz.mehra

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.ahvaz.mehra.core.ui.theme.MehraTheme
import ir.ahvaz.mehra.core.ui.theme.ThemePreset
import ir.ahvaz.mehra.feature.root.RootScreen
import ir.ahvaz.mehra.feature.root.RootViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var keepSplashVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { keepSplashVisible }

        setContent {
            val viewModel: RootViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val preset = ThemePreset.fromId(state.themeId)

            val view = LocalView.current
            DisposableEffect(preset.isDark) {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !preset.isDark
                        isAppearanceLightNavigationBars = !preset.isDark
                    }
                }
                onDispose { }
            }

            MehraTheme(preset = preset) {
                RootScreen(state = state, viewModel = viewModel)
            }

            LaunchedEffect(Unit) { keepSplashVisible = false }
        }
    }
}
