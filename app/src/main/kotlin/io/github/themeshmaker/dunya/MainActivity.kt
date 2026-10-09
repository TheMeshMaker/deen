package io.github.themeshmaker.dunya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.feature.onboarding.OnboardingRoute
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import io.github.themeshmaker.dunya.ui.DeenApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainActivityUiState.Loading }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainActivityUiState.Ready ?: return@setContent
            // Decided once: finishing onboarding navigates on its own, it must not rebuild the graph.
            val startDestination: Any = remember { if (ready.onboardingComplete) TodayRoute else OnboardingRoute }
            // Light/dark/OLED becomes a user setting in M5; the colour theme already is (More tab).
            DeenTheme(style = ready.themeStyle) {
                DeenApp(startDestination = startDestination)
            }
        }
    }
}
