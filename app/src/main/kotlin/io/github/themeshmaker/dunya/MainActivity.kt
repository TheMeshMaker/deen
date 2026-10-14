package io.github.themeshmaker.dunya

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.DeenLinks
import io.github.themeshmaker.dunya.core.ui.AppLocales
import io.github.themeshmaker.dunya.feature.onboarding.OnboardingRoute
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import io.github.themeshmaker.dunya.ui.DeenApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    /** An in-app destination from a notification, waiting to be opened. */
    private val pendingLink = mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        // Android 12 and older: apply the language chosen in Settings (13+ does this itself).
        super.attachBaseContext(AppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainActivityUiState.Loading }
        if (savedInstanceState == null) pendingLink.value = intent.getStringExtra(DeenLinks.EXTRA_OPEN)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainActivityUiState.Ready ?: return@setContent
            // Decided once: finishing onboarding navigates on its own, it must not rebuild the graph.
            val startDestination: Any = remember { if (ready.onboardingComplete) TodayRoute else OnboardingRoute }
            val link by pendingLink
            DeenTheme(
                style = ready.themeStyle,
                themeMode = ready.themeMode,
                uiStyle = ready.uiStyle,
                customColors = ready.customColors,
                liquid = ready.liquid
            ) {
                DeenApp(startDestination = startDestination, link = link, onLinkHandled = { pendingLink.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(DeenLinks.EXTRA_OPEN)?.let { pendingLink.value = it }
    }
}
